package com.dailyback.features.accountoccurrences.application

import com.dailyback.features.accounts.application.AccountAccessContextResolver
import com.dailyback.features.accounts.application.AccountQueryScope
import com.dailyback.features.accounts.application.AccountRepository
import com.dailyback.features.accounts.application.matchesScope
import com.dailyback.features.accounts.domain.AccountAccessDeniedException
import com.dailyback.features.accounts.domain.AccountOccurrence
import com.dailyback.features.accountoccurrences.domain.InvalidOccurrenceAmountException
import com.dailyback.features.accountoccurrences.domain.OccurrenceNotFoundException
import java.math.BigDecimal
import java.util.UUID

class ListOccurrencesUseCase(
    private val occurrenceRepository: OccurrenceRepository,
    private val accountRepository: AccountRepository,
    private val accountAccess: AccountAccessContextResolver,
) {
    fun execute(
        userId: UUID,
        filters: OccurrenceFilters,
        scope: AccountQueryScope = AccountQueryScope.VISIBLE_TO_ME,
    ): List<AccountOccurrence> {
        val q = accountAccess.buildViewerQuery(userId)
        val accountIds = accountRepository.findVisibleForUser(q)
            .filter { it.matchesScope(scope, q) }
            .map { it.id }
            .toSet()
        if (accountIds.isEmpty()) {
            return emptyList()
        }
        return occurrenceRepository.findByFilters(filters.copy(accountIds = accountIds))
    }
}

class GetOccurrenceByIdUseCase(
    private val occurrenceRepository: OccurrenceRepository,
    private val accountRepository: AccountRepository,
    private val accountAccess: AccountAccessContextResolver,
) {
    fun execute(userId: UUID, id: UUID): AccountOccurrence {
        val occurrence = occurrenceRepository.findById(id) ?: throw OccurrenceNotFoundException(id)
        val account = accountRepository.findById(occurrence.accountId) ?: throw OccurrenceNotFoundException(id)
        val q = accountAccess.buildViewerQuery(userId)
        if (!accountAccess.canView(account, q)) {
            throw OccurrenceNotFoundException(id)
        }
        return occurrence
    }
}

class MarkOccurrencePaidUseCase(
    private val occurrenceRepository: OccurrenceRepository,
    private val accountRepository: AccountRepository,
    private val accountAccess: AccountAccessContextResolver,
) {
    fun execute(userId: UUID, id: UUID): AccountOccurrence {
        val occurrence = occurrenceRepository.findById(id) ?: throw OccurrenceNotFoundException(id)
        val account = accountRepository.findById(occurrence.accountId) ?: throw OccurrenceNotFoundException(id)
        val q = accountAccess.buildViewerQuery(userId)
        if (!accountAccess.canMarkPaid(account, q)) {
            throw AccountAccessDeniedException()
        }
        return occurrenceRepository.markPaid(id)
    }
}

class UnmarkOccurrencePaidUseCase(
    private val occurrenceRepository: OccurrenceRepository,
    private val accountRepository: AccountRepository,
    private val accountAccess: AccountAccessContextResolver,
) {
    fun execute(userId: UUID, id: UUID): AccountOccurrence {
        val occurrence = occurrenceRepository.findById(id) ?: throw OccurrenceNotFoundException(id)
        val account = accountRepository.findById(occurrence.accountId) ?: throw OccurrenceNotFoundException(id)
        val q = accountAccess.buildViewerQuery(userId)
        if (!accountAccess.canMarkPaid(account, q)) {
            throw AccountAccessDeniedException()
        }
        return occurrenceRepository.unmarkPaid(id)
    }
}

class OverrideOccurrenceAmountUseCase(
    private val occurrenceRepository: OccurrenceRepository,
    private val accountRepository: AccountRepository,
    private val accountAccess: AccountAccessContextResolver,
) {
    fun execute(userId: UUID, id: UUID, amount: BigDecimal): AccountOccurrence {
        if (amount < BigDecimal.ZERO) {
            throw InvalidOccurrenceAmountException()
        }
        val occurrence = occurrenceRepository.findById(id) ?: throw OccurrenceNotFoundException(id)
        val account = accountRepository.findById(occurrence.accountId) ?: throw OccurrenceNotFoundException(id)
        val q = accountAccess.buildViewerQuery(userId)
        if (!accountAccess.canEdit(account, q)) {
            throw AccountAccessDeniedException()
        }
        return occurrenceRepository.overrideAmount(id, amount)
    }
}
