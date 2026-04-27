package com.dailyback.shared.domain.family

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FamilyAdminPolicyTest {

    @Test
    fun `should allow removing member regardless of admin count`() {
        assertTrue(FamilyAdminPolicy.canRemoveMember(FamilyMemberRole.MEMBER, activeAdminCount = 1))
    }

    @Test
    fun `should block removing last admin`() {
        assertFalse(FamilyAdminPolicy.canRemoveMember(FamilyMemberRole.ADMIN, activeAdminCount = 1))
    }

    @Test
    fun `should allow removing admin when another admin exists`() {
        assertTrue(FamilyAdminPolicy.canRemoveMember(FamilyMemberRole.ADMIN, activeAdminCount = 2))
    }

    @Test
    fun `should block demote when single admin`() {
        assertFalse(FamilyAdminPolicy.canDemoteAdmin(activeAdminCount = 1))
    }

    @Test
    fun `should allow demote when multiple admins`() {
        assertTrue(FamilyAdminPolicy.canDemoteAdmin(activeAdminCount = 2))
    }

    @Test
    fun `should detect at least one admin`() {
        assertTrue(FamilyAdminPolicy.hasAtLeastOneAdmin(1))
        assertFalse(FamilyAdminPolicy.hasAtLeastOneAdmin(0))
    }
}
