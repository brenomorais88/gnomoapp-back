package com.dailyback.features.accounts.application

import com.dailyback.features.accounts.domain.Account
import com.dailyback.features.accounts.domain.AccountOwnershipType

enum class AccountQueryScope {
    PERSONAL,
    FAMILY,
    VISIBLE_TO_ME,
    ;

    companion object {
        fun fromValue(value: String): AccountQueryScope =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported account scope: $value")
    }
}

fun Account.matchesScope(scope: AccountQueryScope, query: AccountViewerQuery): Boolean =
    when (scope) {
        AccountQueryScope.PERSONAL ->
            ownershipType == AccountOwnershipType.PERSONAL && ownerUserId == query.userId

        AccountQueryScope.FAMILY ->
            ownershipType == AccountOwnershipType.FAMILY &&
                query.canViewFamilyAccounts &&
                familyId == query.viewerFamilyId

        AccountQueryScope.VISIBLE_TO_ME ->
            when (ownershipType) {
                AccountOwnershipType.PERSONAL ->
                    if (ownerUserId == query.userId) true
                    else query.canViewOtherPersonalAccounts && query.viewerFamilyId != null

                AccountOwnershipType.FAMILY ->
                    query.canViewFamilyAccounts && familyId == query.viewerFamilyId
            }
    }
