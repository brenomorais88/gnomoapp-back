package com.dailyback.features.accounts.application

import com.dailyback.features.accounts.domain.Account
import com.dailyback.features.accounts.domain.AccountCategoryNotFoundException
import com.dailyback.features.accounts.domain.AccountInvalidAmountException
import com.dailyback.features.accounts.domain.AccountInvalidDateRangeException
import com.dailyback.features.accounts.domain.AccountInvalidTitleException
import com.dailyback.features.accounts.domain.AccountNotFoundException
import com.dailyback.features.accounts.domain.RecurrenceType
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

class ListAccountsUseCase(
    private val accountRepository: AccountRepository,
) {
    fun execute(): List<Account> = accountRepository.findAll()
}

class GetAccountByIdUseCase(
    private val accountRepository: AccountRepository,
) {
    fun execute(id: UUID): Account =
        accountRepository.findById(id) ?: throw AccountNotFoundException(id)
}

class CreateAccountUseCase(
    private val accountRepository: AccountRepository,
    private val recurrenceGenerationService: RecurrenceGenerationService,
    private val utcClock: UtcClock,
) {
    fun execute(input: UpsertAccountInput): Account {
        validateInput(input)
        ensureCategoryExists(input.categoryId)
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
            ),
        )
        regenerateFutureOccurrences(created)
        return created
    }

    private fun validateInput(input: UpsertAccountInput) = validateAccountFields(input)

    private fun ensureCategoryExists(categoryId: UUID) {
        if (!accountRepository.categoryExists(categoryId)) {
            throw AccountCategoryNotFoundException(categoryId)
        }
    }

    private fun regenerateFutureOccurrences(account: Account) {
        val today = utcClock.today()
        val horizon = today.plusMonths(OCCURRENCE_WINDOW_MONTHS)
        val snapshots = recurrenceGenerationService.generateSnapshots(account, today, horizon)
        accountRepository.upsertOccurrences(snapshots)
    }
}

class UpdateAccountUseCase(
    private val accountRepository: AccountRepository,
    private val recurrenceGenerationService: RecurrenceGenerationService,
    private val utcClock: UtcClock,
) {
    fun execute(id: UUID, input: UpsertAccountInput): Account {
        validateAccountFields(input)
        ensureCategoryExists(input.categoryId)
        accountRepository.findById(id) ?: throw AccountNotFoundException(id)

        val updated = accountRepository.update(
            id = id,
            command = SaveAccountCommand(
                title = input.title.trim(),
                baseAmount = input.baseAmount,
                startDate = input.startDate,
                endDate = input.endDate,
                recurrenceType = input.recurrenceType,
                categoryId = input.categoryId,
                notes = input.notes?.trim()?.ifBlank { null },
                active = input.active,
            ),
        )

        val today = utcClock.today()
        accountRepository.deleteFuturePendingOccurrences(id, today)
        val horizon = today.plusMonths(OCCURRENCE_WINDOW_MONTHS)
        val snapshots = recurrenceGenerationService.generateSnapshots(updated, today, horizon)
        accountRepository.upsertOccurrences(snapshots)

        return updated
    }

    private fun ensureCategoryExists(categoryId: UUID) {
        if (!accountRepository.categoryExists(categoryId)) {
            throw AccountCategoryNotFoundException(categoryId)
        }
    }
}

class ActivateAccountUseCase(
    private val accountRepository: AccountRepository,
    private val recurrenceGenerationService: RecurrenceGenerationService,
    private val utcClock: UtcClock,
) {
    fun execute(id: UUID): Account {
        accountRepository.findById(id) ?: throw AccountNotFoundException(id)
        val activated = accountRepository.setActive(id, true)
        val today = utcClock.today()
        val horizon = today.plusMonths(OCCURRENCE_WINDOW_MONTHS)
        val snapshots = recurrenceGenerationService.generateSnapshots(activated, today, horizon)
        accountRepository.upsertOccurrences(snapshots)
        return activated
    }
}

class DeactivateAccountUseCase(
    private val accountRepository: AccountRepository,
    private val utcClock: UtcClock,
) {
    fun execute(id: UUID): Account {
        accountRepository.findById(id) ?: throw AccountNotFoundException(id)
        val deactivated = accountRepository.setActive(id, false)
        accountRepository.deleteFuturePendingOccurrences(id, utcClock.today())
        return deactivated
    }
}

class DeleteAccountUseCase(
    private val accountRepository: AccountRepository,
    private val utcClock: UtcClock,
) {
    fun execute(id: UUID) {
        accountRepository.findById(id) ?: throw AccountNotFoundException(id)
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
