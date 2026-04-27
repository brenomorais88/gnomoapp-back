package com.dailyback.features.accounts.domain

class AccountAccessDeniedException(
    message: String = "Access denied for this account",
) : RuntimeException(message)
