package com.dailyback.features.families.application

import com.dailyback.features.families.domain.DuplicateFamilyMemberInviteException
import com.dailyback.features.families.domain.Family
import com.dailyback.features.families.domain.FamilyAggregateStatus
import com.dailyback.features.families.domain.FamilyMember
import com.dailyback.features.families.domain.FamilyMemberNotFoundException
import com.dailyback.features.families.domain.InvalidFamilyMemberDisplayNameException
import com.dailyback.features.families.domain.InvalidFamilyMemberRoleChangeException
import com.dailyback.features.families.domain.InvalidFamilyMemberRoleValueException
import com.dailyback.features.families.domain.InvalidFamilyNameException
import com.dailyback.features.families.domain.InviteTargetInAnotherFamilyException
import com.dailyback.features.families.domain.LastFamilyAdminException
import com.dailyback.features.families.domain.NoFamilyForUserException
import com.dailyback.features.families.domain.NotFamilyAdminException
import com.dailyback.features.families.domain.UserAlreadyHasFamilyException
import com.dailyback.features.users.application.UserRepository
import com.dailyback.features.users.domain.User
import com.dailyback.features.users.domain.UserStatus
import com.dailyback.shared.domain.identity.LoginIdentifier
import com.dailyback.shared.domain.family.FamilyMemberPermissionFlags
import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FamilyServiceTest {

    private val userId = UUID.randomUUID()
    private val otherUserId = UUID.randomUUID()
    private val permissionRepo = FakeFamilyMemberPermissionRepository()

    private fun user(
        id: UUID = userId,
        first: String = "João",
        last: String = "Souza",
        document: String = "DOC1",
    ): User = User(
        id = id,
        firstName = first,
        lastName = last,
        document = document,
        birthDate = LocalDate.of(1991, 5, 5),
        passwordHash = "x",
        phone = null,
        email = null,
        status = UserStatus.ACTIVE,
        lastLoginAt = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    @Test
    fun `createFamily should insert family and first ADMIN member`() {
        val families = ConcurrentHashMap<UUID, Family>()
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val userRepo = FakeUserRepository(mapOf(userId to user()))
        val familyRepo = FakeFamilyRepository(families)
        val memberRepo = FakeFamilyMemberRepository(families, members)
        val service = FamilyService(familyRepo, memberRepo, userRepo, permissionRepo)

        val family = service.createFamily(userId, "  Casa Silva  ")

        assertEquals("Casa Silva", family.name)
        assertEquals(userId, family.createdByUserId)
        assertEquals(FamilyAggregateStatus.ACTIVE, family.status)
        val admin = members.values.single { it.userId == userId }
        assertEquals(family.id, admin.familyId)
        assertEquals(FamilyMemberRole.ADMIN, admin.role)
        assertEquals(FamilyMembershipStatus.ACTIVE, admin.status)
        assertEquals("João Souza", admin.displayName)
        assertTrue(admin.joinedAt != null)
    }

    @Test
    fun `createFamily should reject when user already has membership`() {
        val familyId = UUID.randomUUID()
        val existingMember = stubMember(familyId = familyId, userId = userId)
        val memberRepo = FakeFamilyMemberRepository(
            families = ConcurrentHashMap(),
            members = ConcurrentHashMap(mapOf(UUID.randomUUID() to existingMember)),
        )
        val service = FamilyService(
            FakeFamilyRepository(ConcurrentHashMap()),
            memberRepo,
            FakeUserRepository(mapOf(userId to user())),
            permissionRepo,
        )

        assertFailsWith<UserAlreadyHasFamilyException> {
            service.createFamily(userId, "Nova")
        }
    }

    @Test
    fun `createFamily should reject blank name`() {
        val service = FamilyService(
            FakeFamilyRepository(ConcurrentHashMap()),
            FakeFamilyMemberRepository(ConcurrentHashMap(), ConcurrentHashMap()),
            FakeUserRepository(mapOf(userId to user())),
            permissionRepo,
        )
        assertFailsWith<InvalidFamilyNameException> {
            service.createFamily(userId, "   ")
        }
    }

    @Test
    fun `getMyFamily should return family when user is member`() {
        val familyId = UUID.randomUUID()
        val family = Family(
            id = familyId,
            name = "F1",
            createdByUserId = userId,
            status = FamilyAggregateStatus.ACTIVE,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        val member = stubMember(familyId = familyId, userId = userId)
        val service = FamilyService(
            FakeFamilyRepository(ConcurrentHashMap(mapOf(familyId to family))),
            FakeFamilyMemberRepository(
                ConcurrentHashMap(mapOf(familyId to family)),
                ConcurrentHashMap(mapOf(UUID.randomUUID() to member)),
            ),
            FakeUserRepository(mapOf(userId to user())),
            permissionRepo,
        )

        assertEquals(familyId, service.getMyFamily(userId).id)
    }

    @Test
    fun `getMyFamily should throw when user has no family`() {
        val service = FamilyService(
            FakeFamilyRepository(ConcurrentHashMap()),
            FakeFamilyMemberRepository(ConcurrentHashMap(), ConcurrentHashMap()),
            FakeUserRepository(mapOf(userId to user())),
            permissionRepo,
        )
        assertFailsWith<NoFamilyForUserException> {
            service.getMyFamily(userId)
        }
    }

    @Test
    fun `listMyFamilyMembers should list members of users family`() {
        val familyId = UUID.randomUUID()
        val family = Family(
            id = familyId,
            name = "F1",
            createdByUserId = userId,
            status = FamilyAggregateStatus.ACTIVE,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        val m1 = stubMember(id = UUID.randomUUID(), familyId = familyId, userId = userId)
        val m2 = stubMember(id = UUID.randomUUID(), familyId = familyId, userId = otherUserId, displayName = "Other")
        val service = FamilyService(
            FakeFamilyRepository(ConcurrentHashMap(mapOf(familyId to family))),
            FakeFamilyMemberRepository(
                ConcurrentHashMap(mapOf(familyId to family)),
                ConcurrentHashMap(
                    mapOf(
                        m1.id to m1,
                        m2.id to m2,
                    ),
                ),
            ),
            FakeUserRepository(mapOf(userId to user(), otherUserId to user(id = otherUserId, first = "A", last = "B"))),
            permissionRepo,
        )

        val list = service.listMyFamilyMembers(userId)
        assertEquals(2, list.size)
    }

    @Test
    fun `inviteMember should reject when actor is not ADMIN`() {
        val familyId = UUID.randomUUID()
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val adminRow = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        val memberRow = stubMember(
            familyId = familyId,
            userId = otherUserId,
            displayName = "Member",
            role = FamilyMemberRole.MEMBER,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[adminRow.id] = adminRow
        members[memberRow.id] = memberRow
        val service = inviteScenarioService(familyId, members)

        assertFailsWith<NotFamilyAdminException> {
            service.inviteMember(otherUserId, "Novo", "999", null, null)
        }
    }

    @Test
    fun `inviteMember should create PENDING_REGISTRATION without user when document unknown`() {
        val familyId = UUID.randomUUID()
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val adminRow = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[adminRow.id] = adminRow
        val service = inviteScenarioService(familyId, members)

        val invited = service.inviteMember(userId, "Filha", "98765432100", null, null)

        assertEquals(FamilyMembershipStatus.PENDING_REGISTRATION, invited.status)
        assertEquals(null, invited.userId)
        assertEquals("98765432100", invited.document)
        assertEquals(FamilyMemberRole.MEMBER, invited.role)
        assertEquals(userId, invited.invitedByUserId)
        assertEquals(null, invited.joinedAt)
    }

    @Test
    fun `inviteMember should link ACTIVE when document matches user without family`() {
        val familyId = UUID.randomUUID()
        val targetId = UUID.randomUUID()
        val targetDoc = "11222333000155"
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val adminRow = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[adminRow.id] = adminRow
        val targetUser = user(id = targetId, document = targetDoc)
        val service = inviteScenarioService(
            familyId = familyId,
            members = members,
            users = mapOf(userId to user(), targetId to targetUser),
        )

        val linked = service.inviteMember(userId, "Esposo", targetDoc, null, null)

        assertEquals(FamilyMembershipStatus.ACTIVE, linked.status)
        assertEquals(targetId, linked.userId)
        assertEquals(targetDoc, linked.document)
        assertEquals(FamilyMemberRole.MEMBER, linked.role)
        assertTrue(linked.joinedAt != null)
    }

    @Test
    fun `inviteMember should reject when target user already in another family`() {
        val familyId = UUID.randomUUID()
        val otherFamilyId = UUID.randomUUID()
        val targetId = UUID.randomUUID()
        val targetDoc = "55666777000188"
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val adminRow = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[adminRow.id] = adminRow
        val elsewhere = stubMember(
            familyId = otherFamilyId,
            userId = targetId,
            displayName = "Elsewhere",
            role = FamilyMemberRole.MEMBER,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[elsewhere.id] = elsewhere
        val service = inviteScenarioService(
            familyId = familyId,
            members = members,
            users = mapOf(userId to user(), targetId to user(id = targetId, document = targetDoc)),
        )

        assertFailsWith<InviteTargetInAnotherFamilyException> {
            service.inviteMember(userId, "Tentativa", targetDoc, null, null)
        }
    }

    @Test
    fun `inviteMember should reject duplicate document in same family`() {
        val familyId = UUID.randomUUID()
        val doc = "11122233344"
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val adminRow = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        val pending = stubMember(
            familyId = familyId,
            userId = null,
            displayName = "Pendente",
            document = doc,
            role = FamilyMemberRole.MEMBER,
            status = FamilyMembershipStatus.PENDING_REGISTRATION,
            joinedAt = null,
        )
        members[adminRow.id] = adminRow
        members[pending.id] = pending
        val service = inviteScenarioService(familyId, members)

        assertFailsWith<DuplicateFamilyMemberInviteException> {
            service.inviteMember(userId, "Outro nome", doc, null, null)
        }
    }

    @Test
    fun `inviteMember should reject duplicate email in same family`() {
        val familyId = UUID.randomUUID()
        val email = "same@family.com"
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val adminRow = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        val pending = stubMember(
            familyId = familyId,
            userId = null,
            displayName = "Pendente",
            document = null,
            email = email,
            role = FamilyMemberRole.MEMBER,
            status = FamilyMembershipStatus.PENDING_REGISTRATION,
            joinedAt = null,
        )
        members[adminRow.id] = adminRow
        members[pending.id] = pending
        val service = inviteScenarioService(familyId, members)

        assertFailsWith<DuplicateFamilyMemberInviteException> {
            service.inviteMember(userId, "Outro", null, email, null)
        }
    }

    @Test
    fun `inviteMember should reject blank display name`() {
        val familyId = UUID.randomUUID()
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val adminRow = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[adminRow.id] = adminRow
        val service = inviteScenarioService(familyId, members)

        assertFailsWith<InvalidFamilyMemberDisplayNameException> {
            service.inviteMember(userId, "   ", null, null, null)
        }
    }

    @Test
    fun `inviteMember should reject when inviting own document`() {
        val familyId = UUID.randomUUID()
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val adminDoc = "DOC1"
        val adminRow = stubMember(
            familyId = familyId,
            userId = userId,
            document = adminDoc,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[adminRow.id] = adminRow
        val service = inviteScenarioService(familyId, members)

        assertFailsWith<DuplicateFamilyMemberInviteException> {
            service.inviteMember(userId, "Me again", adminDoc, null, null)
        }
    }

    @Test
    fun `inviteMember should reject when target already member in family`() {
        val familyId = UUID.randomUUID()
        val doc = "99887766554"
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val adminRow = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        val existing = stubMember(
            familyId = familyId,
            userId = otherUserId,
            displayName = "Já dentro",
            document = doc,
            role = FamilyMemberRole.MEMBER,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[adminRow.id] = adminRow
        members[existing.id] = existing
        val service = inviteScenarioService(
            familyId = familyId,
            members = members,
            users = mapOf(userId to user(), otherUserId to user(id = otherUserId, document = doc)),
        )

        assertFailsWith<DuplicateFamilyMemberInviteException> {
            service.inviteMember(userId, "Duplicado", doc, null, null)
        }
    }

    @Test
    fun `updateMemberRole should promote MEMBER to ADMIN`() {
        val familyId = UUID.randomUUID()
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val adminRow = stubMember(
            id = UUID.randomUUID(),
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        val memberRow = stubMember(
            id = UUID.randomUUID(),
            familyId = familyId,
            userId = otherUserId,
            displayName = "Filho",
            role = FamilyMemberRole.MEMBER,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[adminRow.id] = adminRow
        members[memberRow.id] = memberRow
        val service = inviteScenarioService(familyId, members)

        val updated = service.updateMemberRole(userId, memberRow.id, "ADMIN")

        assertEquals(FamilyMemberRole.ADMIN, updated.role)
        assertEquals(FamilyMembershipStatus.ACTIVE, members[memberRow.id]?.status)
    }

    @Test
    fun `updateMemberRole should demote ADMIN to MEMBER when another admin exists`() {
        val familyId = UUID.randomUUID()
        val third = UUID.randomUUID()
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val admin1 = stubMember(
            id = UUID.randomUUID(),
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        val admin2 = stubMember(
            id = UUID.randomUUID(),
            familyId = familyId,
            userId = otherUserId,
            displayName = "Co-admin",
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        val memberRow = stubMember(
            id = UUID.randomUUID(),
            familyId = familyId,
            userId = third,
            displayName = "Membro",
            role = FamilyMemberRole.MEMBER,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[admin1.id] = admin1
        members[admin2.id] = admin2
        members[memberRow.id] = memberRow
        val service = inviteScenarioService(
            familyId,
            members,
            users = mapOf(userId to user(), otherUserId to user(id = otherUserId), third to user(id = third, document = "D3")),
        )

        val updated = service.updateMemberRole(userId, admin2.id, "MEMBER")

        assertEquals(FamilyMemberRole.MEMBER, updated.role)
    }

    @Test
    fun `updateMemberRole should reject demoting last admin`() {
        val familyId = UUID.randomUUID()
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val onlyAdmin = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[onlyAdmin.id] = onlyAdmin
        val service = inviteScenarioService(familyId, members)

        assertFailsWith<LastFamilyAdminException> {
            service.updateMemberRole(userId, onlyAdmin.id, "MEMBER")
        }
    }

    @Test
    fun `updateMemberRole should reject when actor is not admin`() {
        val familyId = UUID.randomUUID()
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val adminRow = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        val memberRow = stubMember(
            familyId = familyId,
            userId = otherUserId,
            role = FamilyMemberRole.MEMBER,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[adminRow.id] = adminRow
        members[memberRow.id] = memberRow
        val service = inviteScenarioService(familyId, members)

        assertFailsWith<NotFamilyAdminException> {
            service.updateMemberRole(otherUserId, adminRow.id, "MEMBER")
        }
    }

    @Test
    fun `updateMemberRole should reject unknown member id in family`() {
        val familyId = UUID.randomUUID()
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val adminRow = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[adminRow.id] = adminRow
        val service = inviteScenarioService(familyId, members)

        assertFailsWith<FamilyMemberNotFoundException> {
            service.updateMemberRole(userId, UUID.randomUUID(), "MEMBER")
        }
    }

    @Test
    fun `updateMemberRole should reject invalid role string`() {
        val familyId = UUID.randomUUID()
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val adminRow = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        val memberRow = stubMember(
            familyId = familyId,
            userId = otherUserId,
            role = FamilyMemberRole.MEMBER,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[adminRow.id] = adminRow
        members[memberRow.id] = memberRow
        val service = inviteScenarioService(familyId, members)

        assertFailsWith<InvalidFamilyMemberRoleValueException> {
            service.updateMemberRole(userId, memberRow.id, "SUPERUSER")
        }
    }

    @Test
    fun `updateMemberRole should reject role change for PENDING_REGISTRATION`() {
        val familyId = UUID.randomUUID()
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val adminRow = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        val pending = stubMember(
            familyId = familyId,
            userId = null,
            displayName = "Convite",
            role = FamilyMemberRole.MEMBER,
            status = FamilyMembershipStatus.PENDING_REGISTRATION,
            joinedAt = null,
        )
        members[adminRow.id] = adminRow
        members[pending.id] = pending
        val service = inviteScenarioService(familyId, members)

        assertFailsWith<InvalidFamilyMemberRoleChangeException> {
            service.updateMemberRole(userId, pending.id, "ADMIN")
        }
    }

    @Test
    fun `removeMember should soft remove MEMBER`() {
        val familyId = UUID.randomUUID()
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val adminRow = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        val memberRow = stubMember(
            familyId = familyId,
            userId = otherUserId,
            role = FamilyMemberRole.MEMBER,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[adminRow.id] = adminRow
        members[memberRow.id] = memberRow
        val service = inviteScenarioService(familyId, members)

        service.removeMember(userId, memberRow.id)

        assertEquals(FamilyMembershipStatus.REMOVED, members[memberRow.id]?.status)
    }

    @Test
    fun `removeMember should reject removing last admin including self`() {
        val familyId = UUID.randomUUID()
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val onlyAdmin = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[onlyAdmin.id] = onlyAdmin
        val service = inviteScenarioService(familyId, members)

        assertFailsWith<LastFamilyAdminException> {
            service.removeMember(userId, onlyAdmin.id)
        }
    }

    @Test
    fun `removeMember should allow removing one admin when another exists`() {
        val familyId = UUID.randomUUID()
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val admin1 = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        val admin2 = stubMember(
            familyId = familyId,
            userId = otherUserId,
            displayName = "B",
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[admin1.id] = admin1
        members[admin2.id] = admin2
        val service = inviteScenarioService(
            familyId,
            members,
            users = mapOf(userId to user(), otherUserId to user(id = otherUserId)),
        )

        service.removeMember(userId, admin2.id)

        assertEquals(FamilyMembershipStatus.REMOVED, members[admin2.id]?.status)
    }

    @Test
    fun `removeMember should reject when actor is not admin`() {
        val familyId = UUID.randomUUID()
        val members = ConcurrentHashMap<UUID, FamilyMember>()
        val adminRow = stubMember(
            familyId = familyId,
            userId = userId,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
        )
        val memberRow = stubMember(
            familyId = familyId,
            userId = otherUserId,
            role = FamilyMemberRole.MEMBER,
            status = FamilyMembershipStatus.ACTIVE,
        )
        members[adminRow.id] = adminRow
        members[memberRow.id] = memberRow
        val service = inviteScenarioService(familyId, members)

        assertFailsWith<NotFamilyAdminException> {
            service.removeMember(otherUserId, memberRow.id)
        }
    }

    private fun inviteScenarioService(
        familyId: UUID,
        members: MutableMap<UUID, FamilyMember>,
        users: Map<UUID, User> = mapOf(userId to user()),
    ): FamilyService {
        val family = Family(
            id = familyId,
            name = "Família Teste",
            createdByUserId = userId,
            status = FamilyAggregateStatus.ACTIVE,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        return FamilyService(
            FakeFamilyRepository(ConcurrentHashMap(mapOf(familyId to family))),
            FakeFamilyMemberRepository(ConcurrentHashMap(mapOf(familyId to family)), members),
            FakeUserRepository(users),
            permissionRepo,
        )
    }

    private fun stubMember(
        id: UUID = UUID.randomUUID(),
        familyId: UUID,
        userId: UUID?,
        displayName: String = "X",
        document: String? = null,
        email: String? = null,
        phone: String? = null,
        role: FamilyMemberRole = FamilyMemberRole.MEMBER,
        status: FamilyMembershipStatus = FamilyMembershipStatus.ACTIVE,
        invitedByUserId: UUID? = null,
        joinedAt: Instant? = Instant.now(),
    ): FamilyMember = FamilyMember(
        id = id,
        familyId = familyId,
        userId = userId,
        displayName = displayName,
        document = document,
        email = email,
        phone = phone,
        role = role,
        status = status,
        invitedByUserId = invitedByUserId,
        joinedAt = joinedAt,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )
}

private class FakeUserRepository(
    private val users: Map<UUID, User>,
) : UserRepository {
    override fun findById(id: UUID): User? = users[id]
    override fun findByLoginIdentifier(identifier: LoginIdentifier): User? =
        when (identifier) {
            is LoginIdentifier.Document ->
                users.values.firstOrNull { it.document == identifier.normalized }
            else -> null
        }
    override fun existsByDocumentNormalized(document: String): Boolean = false
    override fun existsByEmailNormalized(email: String): Boolean = false
    override fun existsByPhoneDigits(phone: String): Boolean = false
    override fun create(
        firstName: String,
        lastName: String,
        documentNormalized: String,
        birthDate: LocalDate,
        passwordHash: String,
        phoneDigits: String?,
        emailNormalized: String?,
    ): User = throw UnsupportedOperationException()
    override fun updateLastLoginAt(userId: UUID, at: Instant) {}
}

private class FakeFamilyRepository(
    private val families: MutableMap<UUID, Family>,
) : FamilyRepository {
    override fun insert(name: String, createdByUserId: UUID, status: FamilyAggregateStatus): Family {
        val id = UUID.randomUUID()
        val now = Instant.now()
        val f = Family(id = id, name = name, createdByUserId = createdByUserId, status = status, createdAt = now, updatedAt = now)
        families[id] = f
        return f
    }

    override fun findById(id: UUID): Family? = families[id]
}

private class FakeFamilyMemberRepository(
    private val families: MutableMap<UUID, Family>,
    private val members: MutableMap<UUID, FamilyMember>,
) : FamilyMemberRepository {
    override fun findActiveMembershipForUser(userId: UUID): FamilyMember? =
        members.values.firstOrNull { it.userId == userId && it.status in setOf(FamilyMembershipStatus.ACTIVE, FamilyMembershipStatus.PENDING_REGISTRATION) }

    override fun findActiveMembersByFamily(familyId: UUID): List<FamilyMember> =
        members.values.filter { it.familyId == familyId && it.status != FamilyMembershipStatus.REMOVED }

    override fun findNonRemovedMemberInFamilyByUser(familyId: UUID, userId: UUID): FamilyMember? =
        members.values.firstOrNull {
            it.familyId == familyId &&
                it.userId == userId &&
                it.status != FamilyMembershipStatus.REMOVED
        }

    override fun findConflictingInviteInFamily(
        familyId: UUID,
        documentNormalized: String?,
        emailLower: String?,
        phoneDigits: String?,
    ): FamilyMember? {
        val statuses = setOf(FamilyMembershipStatus.ACTIVE, FamilyMembershipStatus.PENDING_REGISTRATION)
        return members.values.firstOrNull { m ->
            m.familyId == familyId && m.status in statuses && (
                (documentNormalized != null && m.document == documentNormalized) ||
                    (emailLower != null && m.email?.equals(emailLower, ignoreCase = true) == true) ||
                    (phoneDigits != null && m.phone == phoneDigits)
                )
        }
    }

    override fun findMemberByIdInFamily(memberId: UUID, familyId: UUID): FamilyMember? {
        val m = members[memberId] ?: return null
        return m.takeIf { it.familyId == familyId && it.status != FamilyMembershipStatus.REMOVED }
    }

    override fun countActiveAdminsInFamily(familyId: UUID): Int =
        members.values.count {
            it.familyId == familyId &&
                it.role == FamilyMemberRole.ADMIN &&
                it.status == FamilyMembershipStatus.ACTIVE
        }

    override fun updateMemberRole(memberId: UUID, familyId: UUID, newRole: FamilyMemberRole): FamilyMember? {
        val current = members[memberId] ?: return null
        if (current.familyId != familyId || current.status != FamilyMembershipStatus.ACTIVE) {
            return null
        }
        val now = Instant.now()
        val updated = current.copy(role = newRole, updatedAt = now)
        members[memberId] = updated
        return updated
    }

    override fun markMemberRemoved(memberId: UUID, familyId: UUID): FamilyMember? {
        val current = members[memberId] ?: return null
        if (current.familyId != familyId) {
            return null
        }
        if (current.status == FamilyMembershipStatus.REMOVED) {
            return null
        }
        if (current.status != FamilyMembershipStatus.ACTIVE &&
            current.status != FamilyMembershipStatus.PENDING_REGISTRATION
        ) {
            return null
        }
        val now = Instant.now()
        val updated = current.copy(status = FamilyMembershipStatus.REMOVED, updatedAt = now)
        members[memberId] = updated
        return updated
    }

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
    ): FamilyMember {
        val id = UUID.randomUUID()
        val now = Instant.now()
        val m = FamilyMember(
            id = id,
            familyId = familyId,
            userId = userId,
            displayName = displayName,
            document = document,
            email = email,
            phone = phone,
            role = role,
            status = status,
            invitedByUserId = invitedByUserId,
            joinedAt = joinedAt,
            createdAt = now,
            updatedAt = now,
        )
        members[id] = m
        return m
    }
}

private class FakeFamilyMemberPermissionRepository : FamilyMemberPermissionRepository {
    override fun findByMemberId(memberId: UUID): FamilyMemberPermissionFlags? = null

    override fun upsert(memberId: UUID, flags: FamilyMemberPermissionFlags): FamilyMemberPermissionFlags = flags
}
