package com.dailyback.features.families.application

import com.dailyback.features.families.domain.FamilyMember
import com.dailyback.features.families.domain.FamilyMemberNotFoundException
import com.dailyback.features.families.domain.NoFamilyForUserException
import com.dailyback.features.families.domain.NotFamilyAdminException
import com.dailyback.shared.domain.family.FamilyMemberPermissionFlags
import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FamilyMemberPermissionServiceTest {

    private val adminUser = UUID.randomUUID()
    private val memberUser = UUID.randomUUID()
    private val familyId = UUID.randomUUID()

    @Test
    fun `getMemberPermissions should return all granted for active admin target`() {
        val adminMemberId = UUID.randomUUID()
        val members = mapOf(
            adminMemberId to stubMember(adminMemberId, adminUser, FamilyMemberRole.ADMIN),
        )
        val service = FamilyMemberPermissionService(
            FakeMemberRepoForPermissions(members),
            RecordingPermRepo(),
        )
        val flags = service.getMemberPermissions(adminUser, adminMemberId)
        assertTrue(flags.canDeleteFamilyAccounts)
        assertTrue(flags.canManageMembers)
    }

    @Test
    fun `getMyPermissions should return all granted for active admin`() {
        val adminMemberId = UUID.randomUUID()
        val members = mapOf(
            adminMemberId to stubMember(adminMemberId, adminUser, FamilyMemberRole.ADMIN),
        )
        val service = FamilyMemberPermissionService(
            FakeMemberRepoForPermissions(members),
            RecordingPermRepo(),
        )

        val flags = service.getMyPermissions(adminUser)

        assertTrue(flags.canDeleteFamilyAccounts)
        assertTrue(flags.canManageMembers)
    }

    @Test
    fun `getMyPermissions should return stored flags for active member`() {
        val memberMemberId = UUID.randomUUID()
        val stored = FamilyMemberPermissionFlags.memberDefaults().copy(
            canViewFamilyAccounts = true,
            canMarkFamilyAccountsPaid = true,
        )
        val members = mapOf(
            memberMemberId to stubMember(memberMemberId, memberUser, FamilyMemberRole.MEMBER),
        )
        val service = FamilyMemberPermissionService(
            FakeMemberRepoForPermissions(members),
            RecordingPermRepo(initial = mapOf(memberMemberId to stored)),
        )

        val flags = service.getMyPermissions(memberUser)

        assertEquals(true, flags.canMarkFamilyAccountsPaid)
        assertEquals(true, flags.canViewFamilyAccounts)
    }

    @Test
    fun `getMyPermissions should return default flags when none are stored`() {
        val memberMemberId = UUID.randomUUID()
        val members = mapOf(
            memberMemberId to stubMember(memberMemberId, memberUser, FamilyMemberRole.MEMBER),
        )
        val service = FamilyMemberPermissionService(
            FakeMemberRepoForPermissions(members),
            RecordingPermRepo(),
        )

        val flags = service.getMyPermissions(memberUser)

        assertEquals(FamilyMemberPermissionFlags.memberDefaults(), flags)
    }

    @Test
    fun `getMyPermissions should fail when user has no family membership`() {
        val service = FamilyMemberPermissionService(
            FakeMemberRepoForPermissions(emptyMap()),
            RecordingPermRepo(),
        )

        assertFailsWith<NoFamilyForUserException> {
            service.getMyPermissions(UUID.randomUUID())
        }
    }

    @Test
    fun `getMemberPermissions should return stored flags for member`() {
        val adminMemberId = UUID.randomUUID()
        val memberMemberId = UUID.randomUUID()
        val stored = FamilyMemberPermissionFlags.memberDefaults().copy(canEditFamilyAccounts = true)
        val members = mapOf(
            adminMemberId to stubMember(adminMemberId, adminUser, FamilyMemberRole.ADMIN),
            memberMemberId to stubMember(memberMemberId, memberUser, FamilyMemberRole.MEMBER),
        )
        val permRepo = RecordingPermRepo(initial = mapOf(memberMemberId to stored))
        val service = FamilyMemberPermissionService(
            FakeMemberRepoForPermissions(members),
            permRepo,
        )
        val flags = service.getMemberPermissions(adminUser, memberMemberId)
        assertEquals(true, flags.canEditFamilyAccounts)
        assertEquals(false, flags.canDeleteFamilyAccounts)
    }

    @Test
    fun `putMemberPermissions should upsert`() {
        val adminMemberId = UUID.randomUUID()
        val memberMemberId = UUID.randomUUID()
        val members = ConcurrentHashMap(
            mapOf(
                adminMemberId to stubMember(adminMemberId, adminUser, FamilyMemberRole.ADMIN),
                memberMemberId to stubMember(memberMemberId, memberUser, FamilyMemberRole.MEMBER),
            ),
        )
        val permRepo = RecordingPermRepo()
        val service = FamilyMemberPermissionService(
            FakeMemberRepoForPermissions(members),
            permRepo,
        )
        val newFlags = FamilyMemberPermissionFlags.allGranted().copy(canDeleteFamilyAccounts = false)
        val saved = service.putMemberPermissions(adminUser, memberMemberId, newFlags)
        assertEquals(false, saved.canDeleteFamilyAccounts)
        assertEquals(memberMemberId, permRepo.lastUpsertMemberId)
    }

    @Test
    fun `getMemberPermissions should reject non admin actor`() {
        val adminMemberId = UUID.randomUUID()
        val memberMemberId = UUID.randomUUID()
        val members = mapOf(
            adminMemberId to stubMember(adminMemberId, adminUser, FamilyMemberRole.ADMIN),
            memberMemberId to stubMember(memberMemberId, memberUser, FamilyMemberRole.MEMBER),
        )
        val service = FamilyMemberPermissionService(
            FakeMemberRepoForPermissions(members),
            RecordingPermRepo(),
        )
        assertFailsWith<NotFamilyAdminException> {
            service.getMemberPermissions(memberUser, adminMemberId)
        }
    }

    @Test
    fun `getMemberPermissions should reject unknown member`() {
        val adminMemberId = UUID.randomUUID()
        val members = mapOf(adminMemberId to stubMember(adminMemberId, adminUser, FamilyMemberRole.ADMIN))
        val service = FamilyMemberPermissionService(
            FakeMemberRepoForPermissions(members),
            RecordingPermRepo(),
        )
        assertFailsWith<FamilyMemberNotFoundException> {
            service.getMemberPermissions(adminUser, UUID.randomUUID())
        }
    }

    private fun stubMember(id: UUID, userId: UUID, role: FamilyMemberRole): FamilyMember = FamilyMember(
        id = id,
        familyId = familyId,
        userId = userId,
        displayName = "X",
        document = null,
        email = null,
        phone = null,
        role = role,
        status = FamilyMembershipStatus.ACTIVE,
        invitedByUserId = null,
        joinedAt = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private class FakeMemberRepoForPermissions(
        private val byId: Map<UUID, FamilyMember>,
    ) : FamilyMemberRepository {
        override fun findActiveMembershipForUser(userId: UUID): FamilyMember? =
            byId.values.firstOrNull { it.userId == userId && it.status == FamilyMembershipStatus.ACTIVE }

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

        override fun findMemberByIdInFamily(memberId: UUID, familyId: UUID): FamilyMember? =
            byId[memberId]?.takeIf { it.familyId == familyId && it.status != FamilyMembershipStatus.REMOVED }

        override fun countActiveAdminsInFamily(familyId: UUID): Int = 0

        override fun updateMemberRole(memberId: UUID, familyId: UUID, newRole: FamilyMemberRole): FamilyMember? = null

        override fun markMemberRemoved(memberId: UUID, familyId: UUID): FamilyMember? = null
    }

    private class RecordingPermRepo(
        private val initial: Map<UUID, FamilyMemberPermissionFlags> = emptyMap(),
    ) : FamilyMemberPermissionRepository {
        val store = ConcurrentHashMap(initial)
        var lastUpsertMemberId: UUID? = null

        override fun findByMemberId(memberId: UUID): FamilyMemberPermissionFlags? = store[memberId]

        override fun upsert(memberId: UUID, flags: FamilyMemberPermissionFlags): FamilyMemberPermissionFlags {
            lastUpsertMemberId = memberId
            store[memberId] = flags
            return flags
        }
    }
}
