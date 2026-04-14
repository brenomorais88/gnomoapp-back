package com.dailyback.features.accounts.domain

import java.util.UUID

class AccountNotFoundException(accountId: UUID) : RuntimeException("Account not found: $accountId")

class AccountCategoryNotFoundException(categoryId: UUID) : RuntimeException("Category not found: $categoryId")

class AccountInvalidTitleException : RuntimeException("Account title must not be blank")

class AccountInvalidAmountException : RuntimeException("Account amount must be greater than or equal to zero")

class AccountInvalidDateRangeException : RuntimeException("Account end date must be greater than or equal to start date")
