package com.dailyback.features.accountoccurrences.api

import com.dailyback.features.accounts.domain.AccountOccurrence
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class OverrideOccurrenceAmountRequest(
    val amount: String,
)

@Serializable
data class OccurrenceResponse(
    val id: String,
    val accountId: String,
    val titleSnapshot: String,
    val amountSnapshot: String,
    val dueDate: String,
    val status: String,
    val paidAt: String? = null,
    val notesSnapshot: String? = null,
    val categoryIdSnapshot: String,
    val createdAt: String,
    val updatedAt: String,
)

fun AccountOccurrence.toResponse(): OccurrenceResponse = OccurrenceResponse(
    id = id.toString(),
    accountId = accountId.toString(),
    titleSnapshot = titleSnapshot,
    amountSnapshot = amountSnapshot.toPlainString(),
    dueDate = dueDate.toString(),
    status = status.name,
    paidAt = paidAt?.toString(),
    notesSnapshot = notesSnapshot,
    categoryIdSnapshot = categoryIdSnapshot.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun OverrideOccurrenceAmountRequest.amountAsBigDecimal(): BigDecimal = BigDecimal(amount)
