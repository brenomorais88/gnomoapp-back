package com.dailyback.features.accountoccurrences.application

import com.dailyback.features.accounts.domain.AccountOccurrence
import com.dailyback.features.accountoccurrences.domain.InvalidOccurrenceAmountException
import com.dailyback.features.accountoccurrences.domain.OccurrenceNotFoundException
import java.math.BigDecimal
import java.util.UUID

class ListOccurrencesUseCase(
    private val occurrenceRepository: OccurrenceRepository,
) {
    fun execute(filters: OccurrenceFilters): List<AccountOccurrence> = occurrenceRepository.findByFilters(filters)
}

class GetOccurrenceByIdUseCase(
    private val occurrenceRepository: OccurrenceRepository,
) {
    fun execute(id: UUID): AccountOccurrence =
        occurrenceRepository.findById(id) ?: throw OccurrenceNotFoundException(id)
}

class MarkOccurrencePaidUseCase(
    private val occurrenceRepository: OccurrenceRepository,
) {
    fun execute(id: UUID): AccountOccurrence {
        occurrenceRepository.findById(id) ?: throw OccurrenceNotFoundException(id)
        return occurrenceRepository.markPaid(id)
    }
}

class UnmarkOccurrencePaidUseCase(
    private val occurrenceRepository: OccurrenceRepository,
) {
    fun execute(id: UUID): AccountOccurrence {
        occurrenceRepository.findById(id) ?: throw OccurrenceNotFoundException(id)
        return occurrenceRepository.unmarkPaid(id)
    }
}

class OverrideOccurrenceAmountUseCase(
    private val occurrenceRepository: OccurrenceRepository,
) {
    fun execute(id: UUID, amount: BigDecimal): AccountOccurrence {
        if (amount < BigDecimal.ZERO) {
            throw InvalidOccurrenceAmountException()
        }
        occurrenceRepository.findById(id) ?: throw OccurrenceNotFoundException(id)
        return occurrenceRepository.overrideAmount(id, amount)
    }
}
