package com.dailyback.features.dashboard.api

import com.dailyback.features.dashboard.application.DashboardCategorySummaryItem
import com.dailyback.features.dashboard.application.DashboardHomeSummary
import com.dailyback.features.dashboard.application.DashboardMonthProjectionItem
import com.dailyback.features.dashboard.application.DashboardOccurrenceItem
import kotlinx.serialization.Serializable

@Serializable
data class DashboardOccurrenceResponse(
    val id: String,
    val accountId: String,
    val title: String,
    val amount: String,
    val dueDate: String,
    val status: String,
    val categoryId: String,
)

@Serializable
data class DashboardCategorySummaryResponse(
    val categoryId: String,
    val totalAmount: String,
    val count: Int,
)

@Serializable
data class DashboardHomeSummaryResponse(
    val overdue: List<DashboardOccurrenceResponse>,
    val next7Days: List<DashboardOccurrenceResponse>,
    val totalPendingInMonth: String,
    val totalPaidInMonth: String,
    val upcoming: List<DashboardOccurrenceResponse>,
    val categorySummary: List<DashboardCategorySummaryResponse>,
)

@Serializable
data class DashboardMonthProjectionResponse(
    val month: String,
    val totalAmount: String,
    val occurrences: List<DashboardOccurrenceResponse>,
)

fun DashboardHomeSummary.toResponse(): DashboardHomeSummaryResponse = DashboardHomeSummaryResponse(
    overdue = overdue.map { it.toResponse() },
    next7Days = next7Days.map { it.toResponse() },
    totalPendingInMonth = totalPendingInMonth.toPlainString(),
    totalPaidInMonth = totalPaidInMonth.toPlainString(),
    upcoming = upcoming.map { it.toResponse() },
    categorySummary = categorySummary.map { it.toResponse() },
)

fun DashboardCategorySummaryItem.toResponse(): DashboardCategorySummaryResponse = DashboardCategorySummaryResponse(
    categoryId = categoryId.toString(),
    totalAmount = totalAmount.toPlainString(),
    count = count,
)

fun DashboardMonthProjectionItem.toResponse(): DashboardMonthProjectionResponse = DashboardMonthProjectionResponse(
    month = month,
    totalAmount = totalAmount.toPlainString(),
    occurrences = occurrences.map { it.toResponse() },
)

fun DashboardOccurrenceItem.toResponse(): DashboardOccurrenceResponse = DashboardOccurrenceResponse(
    id = id.toString(),
    accountId = accountId.toString(),
    title = title,
    amount = amount.toPlainString(),
    dueDate = dueDate.toString(),
    status = status.name,
    categoryId = categoryId.toString(),
)
