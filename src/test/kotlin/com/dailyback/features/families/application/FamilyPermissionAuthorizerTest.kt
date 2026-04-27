package com.dailyback.features.families.application

import com.dailyback.features.families.domain.FamilyMember
import com.dailyback.features.families.domain.FamilyPermissionDeniedException
import com.dailyback.shared.domain.family.FamilyMemberPermissionFlags
import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import com.dailyback.shared.domain.family.FamilyPermissionKey
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FamilyPermissionAuthorizerTest {

    private val userId = UUID.randomUUID()

    private fun membership(
        role: FamilyMemberRole,
        status: FamilyMembershipStatus = FamilyMembershipStatus.ACTIVE,
        memberId: UUID = UUID.randomUUID(),
    ): FamilyMember = FamilyMember(
        id = memberId,
        familyId = UUID.randomUUID(),
        userId = userId,
        displayName = "U",
        document = null,
        email = null,
        phone = null,
        role = role,
        status = status,
        invitedByUserId = null,
        joinedAt = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    @Test
    fun `require should allow any permission for active admin`() {
        val authorizer = DefaultFamilyPermissionAuthorizer(
            familyMemberRepository = StubMemberRepo(membership(FamilyMemberRole.ADMIN)),
            permissionRepository = StubPermRepo(null),
        )
        authorizer.require(userId, FamilyPermissionKey.CAN_DELETE_FAMILY_ACCOUNTS)
    }

    @Test
    fun `require should allow view for member with defaults`() {
        val m = membership(FamilyMemberRole.MEMBER)
        val authorizer = DefaultFamilyPermissionAuthorizer(
            familyMemberRepository = StubMemberRepo(m),
            permissionRepository = StubPermRepo(null),
        )
        authorizer.require(userId, FamilyPermissionKey.CAN_VIEW_FAMILY_ACCOUNTS)
    }

    @Test
    fun `require should deny create accounts for member with defaults`() {
        val m = membership(FamilyMemberRole.MEMBER)
        val authorizer = DefaultFamilyPermissionAuthorizer(
            familyMemberRepository = StubMemberRepo(m),
            permissionRepository = StubPermRepo(null),
        )
        assertFailsWith<FamilyPermissionDeniedException> {
            authorizer.require(userId, FamilyPermissionKey.CAN_CREATE_FAMILY_ACCOUNTS)
        }
    }

    @Test
    fun `require should use stored flags for member`() {
        val m = membership(FamilyMemberRole.MEMBER)
        val flags = FamilyMemberPermissionFlags.memberDefaults().copy(canCreateFamilyAccounts = true)
        val authorizer = DefaultFamilyPermissionAuthorizer(
            familyMemberRepository = StubMemberRepo(m),
            permissionRepository = StubPermRepo(flags),
        )
        authorizer.require(userId, FamilyPermissionKey.CAN_CREATE_FAMILY_ACCOUNTS)
    }

    @Test
    fun `require should deny when no membership`() {
        val authorizer = DefaultFamilyPermissionAuthorizer(
            familyMemberRepository = StubMemberRepo(null),
            permissionRepository = StubPermRepo(null),
        )
        assertFailsWith<FamilyPermissionDeniedException> {
            authorizer.require(userId, FamilyPermissionKey.CAN_VIEW_FAMILY_ACCOUNTS)
        }
    }

    @Test
    fun `require should deny pending member`() {
        val m = membership(FamilyMemberRole.MEMBER, FamilyMembershipStatus.PENDING_REGISTRATION)
        val authorizer = DefaultFamilyPermissionAuthorizer(
            familyMemberRepository = StubMemberRepo(m),
            permissionRepository = StubPermRepo(null),
        )
        assertFailsWith<FamilyPermissionDeniedException> {
            authorizer.require(userId, FamilyPermissionKey.CAN_VIEW_FAMILY_ACCOUNTS)
        }
    }

    private class StubMemberRepo(
        private val row: FamilyMember?,
    ) : FamilyMemberRepository {
        override fun findActiveMembershipForUser(userId: UUID): FamilyMember? = row?.takeIf { it.userId == userId }

        override fun findActiveMembersByFamily(familyId: UUID): List<FamilyMember> = emptyList()

        override fun findNonRemovedMemberInFamilyByUser(familyId: UUID, userId: UUID): FamilyMember? = null

        override fun findConflictingInviteInFamily(
            familyId: UUID,
            documentNormalized: String?,
            emailLower: String?,
            phoneDigits: String?,
        ): FamilyMember? = null

        override fun insertMember(
            familyId: UUID,
            userId: UUID?,
            displayName: String,
            document: String?,
            email: String?,
            phone: String?,
            role: FamilyMemberRole,
            status: FamilyMembershipStatus,
            invitedByUserId: UUID?,
            joinedAt: Instant?,
        ): FamilyMember = throw UnsupportedOperationException()

        override fun findMemberByIdInFamily(memberId: UUID, familyId: UUID): FamilyMember? = null

        override fun countActiveAdminsInFamily(familyId: UUID): Int = 0

        override fun updateMemberRole(memberId: UUID, familyId: UUID, newRole: FamilyMemberRole): FamilyMember? = null

        override fun markMemberRemoved(memberId: UUID, familyId: UUID): FamilyMember? = null
    }

    private class StubPermRepo(
        private val row: FamilyMemberPermissionFlags?,
    ) : FamilyMemberPermissionRepository {
        override fun findByMemberId(memberId: UUID): FamilyMemberPermissionFlags? = row

        override fun upsert(memberId: UUID, flags: FamilyMemberPermissionFlags): FamilyMemberPermissionFlags =
            throw UnsupportedOperationException()
    }
}
