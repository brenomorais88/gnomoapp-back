package com.dailyback.shared.application.maintenance

import com.dailyback.features.accounts.application.AccountRepository
import com.dailyback.features.accounts.application.RecurrenceGenerationService
import com.dailyback.shared.time.UtcClock

class RecurrenceMaintenanceService(
    private val accountRepository: AccountRepository,
    private val recurrenceGenerationService: RecurrenceGenerationService,
    private val utcClock: UtcClock,
) {
    fun execute() {
        val today = utcClock.today()
        val horizon = today.plusMonths(24)
        accountRepository.findActiveRecurringAccounts().forEach { account ->
            val snapshots = recurrenceGenerationService.generateSnapshots(
                account = account,
                fromDate = today,
                horizonEndDate = horizon,
            )
            accountRepository.upsertOccurrences(snapshots)
        }
    }
}
