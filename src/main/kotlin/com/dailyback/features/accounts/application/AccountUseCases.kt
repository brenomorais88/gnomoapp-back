package com.dailyback.features.accounts.application

import com.dailyback.features.accounts.domain.Account
import com.dailyback.features.accounts.domain.AccountAccessDeniedException
import com.dailyback.features.accounts.domain.AccountCategoryNotFoundException
import com.dailyback.features.accounts.domain.AccountInvalidAmountException
import com.dailyback.features.accounts.domain.AccountInvalidDateRangeException
import com.dailyback.features.accounts.domain.AccountInvalidOwnershipException
import com.dailyback.features.accounts.domain.AccountInvalidTitleException
import com.dailyback.features.accounts.domain.AccountNotFoundException
import com.dailyback.features.accounts.domain.AccountOwnershipType
import com.dailyback.features.accounts.domain.RecurrenceType
import com.dailyback.features.categories.application.CategoryRepository
import com.dailyback.features.families.application.FamilyMemberRepository
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import com.dailyback.shared.time.UtcClock
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

private const val OCCURRENCE_WINDOW_MONTHS = 24L

data class UpsertAccountInput(
    val title: String,
    val baseAmount: BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val recurrenceType: RecurrenceType = RecurrenceType.UNIQUE,
    val categoryId: UUID,
    val notes: String?,
    val active: Boolean = true,
)

data class CreateAccountInput(
    val ownershipType: AccountOwnershipType,
    val responsibleMemberId: UUID?,
    val title: String,
    val baseAmount: BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val recurrenceType: RecurrenceType,
    val categoryId: UUID,
    val notes: String?,
    val active: Boolean = true,
)

class ListAccountsUseCase(
    private val accountRepository: AccountRepository,
    private val accountAccess: AccountAccessContextResolver,
) {
    fun execute(userId: UUID, scope: AccountQueryScope = AccountQueryScope.VISIBLE_TO_ME): List<Account> {
        val q = accountAccess.buildViewerQuery(userId)
        return accountRepository.findVisibleForUser(q)
            .filter { it.matchesScope(scope, q) }
    }
}

class GetAccountByIdUseCase(
    private val accountRepository: AccountRepository,
    private val accountAccess: AccountAccessContextResolver,
) {
    fun execute(userId: UUID, id: UUID): Account {
        val account = accountRepository.findById(id) ?: throw AccountNotFoundException(id)
        val q = accountAccess.buildViewerQuery(userId)
        if (!accountAccess.canView(account, q)) {
            throw AccountNotFoundException(id)
        }
        return account
    }
}

class CreateAccountUseCase(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val familyMemberRepository: FamilyMemberRepository,
    private val accountAccess: AccountAccessContextResolver,
    private val recurrenceGenerationService: RecurrenceGenerationService,
    private val utcClock: UtcClock,
) {
    fun execute(userId: UUID, input: CreateAccountInput): Account {
        validateAccountFields(
            UpsertAccountInput(
                title = input.title,
                baseAmount = input.baseAmount,
                startDate = input.startDate,
                endDate = input.endDate,
                recurrenceType = input.recurrenceType,
                categoryId = input.categoryId,
                notes = input.notes,
                active = input.active,
            ),
        )

        val membership = familyMemberRepository.findActiveMembershipForUser(userId)
        val viewerFamilyId = membership?.takeIf { it.status == FamilyMembershipStatus.ACTIVE }?.familyId

        val (ownershipType, ownerUserId, familyId, responsibleMemberId) = when (input.ownershipType) {
            AccountOwnershipType.PERSONAL -> {
                if (input.responsibleMemberId != null) {
                    throw AccountInvalidOwnershipException("responsibleMemberId applies only to FAMILY accounts")
                }
                TupleOwnership(
                    AccountOwnershipType.PERSONAL,
                    userId,
                    null,
                    null,
                )
            }

            AccountOwnershipType.FAMILY -> {
                val fid = viewerFamilyId
                    ?: throw AccountInvalidOwnershipException("FAMILY account requires an active family membership")
                if (input.responsibleMemberId != null) {
                    val member = familyMemberRepository.findMemberByIdInFamily(input.responsibleMemberId, fid)
                        ?: throw AccountInvalidOwnershipException("responsibleMemberId must be a member of the family")
                    if (member.familyId != fid) {
                        throw AccountInvalidOwnershipException("responsibleMemberId must belong to the same family")
                    }
                }
                TupleOwnership(AccountOwnershipType.FAMILY, null, fid, input.responsibleMemberId)
            }
        }

        val familyIdForCategory = familyId ?: viewerFamilyId
        if (!categoryRepository.isVisibleToUser(input.categoryId, userId, familyIdForCategory)) {
            throw AccountCategoryNotFoundException(input.categoryId)
        }

        val created = accountRepository.create(
            SaveAccountCommand(
                title = input.title.trim(),
                baseAmount = input.baseAmount,
                startDate = input.startDate,
                endDate = input.endDate,
                recurrenceType = input.recurrenceType,
                categoryId = input.categoryId,
                notes = input.notes?.trim()?.ifBlank { null },
                active = input.active,
                ownershipType = ownershipType,
                ownerUserId = ownerUserId,
                familyId = familyId,
                createdByUserId = userId,
                responsibleMemberId = responsibleMemberId,
            ),
        )
        regenerateFutureOccurrences(created)
        return created
    }

    private data class TupleOwnership(
        val ownershipType: AccountOwnershipType,
        val ownerUserId: UUID?,
        val familyId: UUID?,
        val responsibleMemberId: UUID?,
    )

    /**
     * Materializes pending occurrences from [Account.startDate] through the rolling horizon.
     * Using "today" as the lower bound would skip past-due installments (e.g. monthly bills
     * between start and the current date).
     */
    private fun regenerateFutureOccurrences(account: Account) {
        val today = utcClock.today()
        val horizon = today.plusMonths(OCCURRENCE_WINDOW_MONTHS)
        val windowStart = account.startDate
        val snapshots = recurrenceGenerationService.generateSnapshots(account, windowStart, horizon)
        accountRepository.upsertOccurrences(snapshots)
    }
}

class UpdateAccountUseCase(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val familyMemberRepository: FamilyMemberRepository,
    private val accountAccess: AccountAccessContextResolver,
    private val recurrenceGenerationService: RecurrenceGenerationService,
    private val utcClock: UtcClock,
) {
    /**
     * Recurrence refresh criteria:
     * - old occurrence: dueDate < today OR already paid (status == PAID)
     * - future occurrence: dueDate >= today AND pending (status == PENDING)
     *
     * Only future pending occurrences are replaced after a base account update.
     */
    fun execute(userId: UUID, id: UUID, input: UpsertAccountInput): Account {
        validateAccountFields(input)
        val existing = accountRepository.findById(id) ?: throw AccountNotFoundException(id)
        val q = accountAccess.buildViewerQuery(userId)
        if (!accountAccess.canEdit(existing, q)) {
            throw AccountAccessDeniedException()
        }

        val familyIdForCategory = existing.familyId ?: familyMemberRepository.findActiveMembershipForUser(userId)
            ?.takeIf { it.status == FamilyMembershipStatus.ACTIVE }?.familyId
        if (!categoryRepository.isVisibleToUser(input.categoryId, userId, familyIdForCategory)) {
            throw AccountCategoryNotFoundException(input.categoryId)
        }

        val updateCommand = SaveAccountCommand(
            title = input.title.trim(),
            baseAmount = input.baseAmount,
            startDate = input.startDate,
            endDate = input.endDate,
            recurrenceType = input.recurrenceType,
            categoryId = input.categoryId,
            notes = input.notes?.trim()?.ifBlank { null },
            active = input.active,
            ownershipType = existing.ownershipType,
            ownerUserId = existing.ownerUserId,
            familyId = existing.familyId,
            createdByUserId = existing.createdByUserId ?: userId,
            responsibleMemberId = existing.responsibleMemberId,
        )

        val today = utcClock.today()
        val updatedProjection = existing.copy(
            title = updateCommand.title,
            baseAmount = updateCommand.baseAmount,
            startDate = updateCommand.startDate,
            endDate = updateCommand.endDate,
            recurrenceType = updateCommand.recurrenceType,
            categoryId = updateCommand.categoryId,
            notes = updateCommand.notes,
            active = updateCommand.active,
        )
        val horizon = today.plusMonths(OCCURRENCE_WINDOW_MONTHS)
        val snapshots = recurrenceGenerationService.generateSnapshots(updatedProjection, today, horizon)
        return accountRepository.updateAndRefreshFuturePendingOccurrences(
            id = id,
            command = updateCommand,
            fromDate = today,
            futurePendingSnapshots = snapshots,
        )
    }
}

class ActivateAccountUseCase(
    private val accountRepository: AccountRepository,
    private val accountAccess: AccountAccessContextResolver,
    private val recurrenceGenerationService: RecurrenceGenerationService,
    private val utcClock: UtcClock,
) {
    fun execute(userId: UUID, id: UUID): Account {
        val existing = accountRepository.findById(id) ?: throw AccountNotFoundException(id)
        val q = accountAccess.buildViewerQuery(userId)
        if (!accountAccess.canEdit(existing, q)) {
            throw AccountAccessDeniedException()
        }
        val activated = accountRepository.setActive(id, true)
        val today = utcClock.today()
        val horizon = today.plusMonths(OCCURRENCE_WINDOW_MONTHS)
        val windowStart = activated.startDate
        val snapshots = recurrenceGenerationService.generateSnapshots(activated, windowStart, horizon)
        accountRepository.upsertOccurrences(snapshots)
        return activated
    }
}

class DeactivateAccountUseCase(
    private val accountRepository: AccountRepository,
    private val accountAccess: AccountAccessContextResolver,
    private val utcClock: UtcClock,
) {
    fun execute(userId: UUID, id: UUID): Account {
        val existing = accountRepository.findById(id) ?: throw AccountNotFoundException(id)
        val q = accountAccess.buildViewerQuery(userId)
        if (!accountAccess.canEdit(existing, q)) {
            throw AccountAccessDeniedException()
        }
        val deactivated = accountRepository.setActive(id, false)
        accountRepository.deleteFuturePendingOccurrences(id, utcClock.today())
        return deactivated
    }
}

class DeleteAccountUseCase(
    private val accountRepository: AccountRepository,
    private val accountAccess: AccountAccessContextResolver,
    private val utcClock: UtcClock,
) {
    fun execute(userId: UUID, id: UUID) {
        val existing = accountRepository.findById(id) ?: throw AccountNotFoundException(id)
        val q = accountAccess.buildViewerQuery(userId)
        if (!accountAccess.canDelete(existing, q)) {
            throw AccountAccessDeniedException()
        }
        val today = utcClock.today()
        if (accountRepository.hasRelevantHistory(id, today)) {
            accountRepository.setActive(id, false)
            accountRepository.deleteFuturePendingOccurrences(id, today)
            return
        }
        accountRepository.delete(id)
    }
}

private fun validateAccountFields(input: UpsertAccountInput) {
    if (input.title.trim().isBlank()) {
        throw AccountInvalidTitleException()
    }
    if (input.baseAmount < BigDecimal.ZERO) {
        throw AccountInvalidAmountException()
    }
    if (input.endDate != null && input.endDate < input.startDate) {
        throw AccountInvalidDateRangeException()
    }
}
