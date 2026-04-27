package com.dailyback.features.accounts.api

import com.dailyback.features.accounts.application.CreateAccountInput
import com.dailyback.features.accounts.application.UpsertAccountInput
import com.dailyback.features.accounts.domain.Account
import com.dailyback.features.accounts.domain.AccountOwnershipType
import com.dailyback.features.accounts.domain.RecurrenceType
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Serializable
data class CreateAccountRequest(
    val ownershipType: String,
    val responsibleMemberId: String? = null,
    val title: String,
    val baseAmount: String,
    val startDate: String,
    val endDate: String? = null,
    val recurrenceType: String? = null,
    val categoryId: String,
    val notes: String? = null,
    val active: Boolean? = null,
)

@Serializable
data class UpsertAccountRequest(
    val title: String,
    val baseAmount: String,
    val startDate: String,
    val endDate: String? = null,
    val recurrenceType: String? = null,
    val categoryId: String,
    val notes: String? = null,
    val active: Boolean? = null,
)

@Serializable
data class AccountResponse(
    val id: String,
    val title: String,
    val baseAmount: String,
    val startDate: String,
    val endDate: String? = null,
    val recurrenceType: String,
    val categoryId: String,
    val notes: String? = null,
    val active: Boolean,
    val ownershipType: String,
    val ownerUserId: String? = null,
    val familyId: String? = null,
    val createdByUserId: String? = null,
    val responsibleMemberId: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

fun Account.toResponse(): AccountResponse = AccountResponse(
    id = id.toString(),
    title = title,
    baseAmount = baseAmount.toPlainString(),
    startDate = startDate.toString(),
    endDate = endDate?.toString(),
    recurrenceType = recurrenceType.name,
    categoryId = categoryId.toString(),
    notes = notes,
    active = active,
    ownershipType = ownershipType.name,
    ownerUserId = ownerUserId?.toString(),
    familyId = familyId?.toString(),
    createdByUserId = createdByUserId?.toString(),
    responsibleMemberId = responsibleMemberId?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun CreateAccountRequest.toCreateInput(): CreateAccountInput = CreateAccountInput(
    ownershipType = AccountOwnershipType.fromValue(ownershipType),
    responsibleMemberId = responsibleMemberId?.let(UUID::fromString),
    title = title,
    baseAmount = BigDecimal(baseAmount),
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let(LocalDate::parse),
    recurrenceType = recurrenceType?.let(RecurrenceType::fromValue) ?: RecurrenceType.UNIQUE,
    categoryId = UUID.fromString(categoryId),
    notes = notes,
    active = active ?: true,
)

fun UpsertAccountRequest.toUpdateInput(): UpsertAccountInput = UpsertAccountInput(
    title = title,
    baseAmount = BigDecimal(baseAmount),
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let(LocalDate::parse),
    recurrenceType = recurrenceType?.let(RecurrenceType::fromValue) ?: RecurrenceType.UNIQUE,
    categoryId = UUID.fromString(categoryId),
    notes = notes,
    active = active ?: true,
)
