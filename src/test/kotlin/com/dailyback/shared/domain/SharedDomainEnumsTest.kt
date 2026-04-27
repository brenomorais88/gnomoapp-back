package com.dailyback.shared.domain

import com.dailyback.shared.domain.access.ResourceScope
import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import com.dailyback.shared.domain.family.FamilyPermissionKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SharedDomainEnumsTest {

    @Test
    fun `ResourceScope fromValue is case insensitive`() {
        assertEquals(ResourceScope.PERSONAL, ResourceScope.fromValue("personal"))
        assertEquals(ResourceScope.FAMILY, ResourceScope.fromValue(" FAMILY "))
    }

    @Test
    fun `FamilyMemberRole fromValue`() {
        assertEquals(FamilyMemberRole.ADMIN, FamilyMemberRole.fromValue("admin"))
    }

    @Test
    fun `FamilyMembershipStatus fromValue`() {
        assertEquals(
            FamilyMembershipStatus.PENDING_REGISTRATION,
            FamilyMembershipStatus.fromValue("pending_registration"),
        )
        assertEquals(
            FamilyMembershipStatus.PENDING_REGISTRATION,
            FamilyMembershipStatus.fromValue("pending"),
        )
    }

    @Test
    fun `FamilyPermissionKey fromValue`() {
        assertEquals(
            FamilyPermissionKey.CAN_VIEW_FAMILY_ACCOUNTS,
            FamilyPermissionKey.fromValue("CAN_VIEW_FAMILY_ACCOUNTS"),
        )
    }

    @Test
    fun `should fail on unknown enum values`() {
        assertFailsWith<IllegalArgumentException> { ResourceScope.fromValue("SHARED") }
        assertFailsWith<IllegalArgumentException> { FamilyMemberRole.fromValue("OWNER") }
        assertFailsWith<IllegalArgumentException> { FamilyMembershipStatus.fromValue("UNKNOWN") }
    }
}
