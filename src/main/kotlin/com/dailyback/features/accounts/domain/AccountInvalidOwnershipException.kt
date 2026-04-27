package com.dailyback.features.accounts.domain

class AccountInvalidOwnershipException(
    message: String,
) : RuntimeException(message)
