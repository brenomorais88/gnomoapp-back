package com.dailyback.features.users.application

import com.dailyback.features.families.application.FamilyPendingMemberLinkRepository
import com.dailyback.features.users.domain.DuplicateUserDocumentException
import com.dailyback.features.users.domain.DuplicateUserEmailException
import com.dailyback.features.users.domain.DuplicateUserPhoneException
import com.dailyback.features.users.domain.InvalidCredentialsException
import com.dailyback.features.users.domain.InvalidUserRegistrationException
import com.dailyback.features.users.domain.User
import com.dailyback.features.users.domain.UserDisabledException
import com.dailyback.features.users.domain.UserNotFoundException
import com.dailyback.features.users.domain.UserStatus
import com.dailyback.app.config.JwtAuthConfig
import com.dailyback.shared.application.identity.LoginIdentifierParser
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import com.dailyback.shared.domain.identity.LoginIdentifier
import com.dailyback.shared.validation.IdentifierNormalizer
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserAuthServiceTest {

    private val jwtConfig = JwtAuthConfig(
        secret = "unit-test-secret-key-for-jwt-hs256-must-be-long-enough",
        issuer = "test",
        audience = "test",
        accessTokenTtlSeconds = 3600,
    )

    private fun buildService(
        users: MutableMap<UUID, User> = ConcurrentHashMap(),
        passwordHasher: PasswordHasher = FakePasswordHasher(),
    ): Pair<UserAuthService, RecordingPendingLinker> {
        val recordingLinker = RecordingPendingLinker()
        val repo = InMemoryUserRepository(users)
        val service = UserAuthService(
            userRepository = repo,
            pendingMemberLinkRepository = recordingLinker,
            passwordHasher = passwordHasher,
            jwtTokenService = JwtTokenService(jwtConfig),
            loginIdentifierParser = LoginIdentifierParser(),
        )
        return service to recordingLinker
    }

    @Test
    fun `register should invoke pending linker with normalized document`() {
        val docKey = IdentifierNormalizer.normalizeDocumentKey("11144477735")
        val state = PendingLinkSlot(expectedDocument = docKey)
        val linker = MutatingPendingMemberLinker(state)
        val users = ConcurrentHashMap<UUID, User>()
        val service = UserAuthService(
            userRepository = InMemoryUserRepository(users),
            pendingMemberLinkRepository = linker,
            passwordHasher = FakePasswordHasher(),
            jwtTokenService = JwtTokenService(jwtConfig),
            loginIdentifierParser = LoginIdentifierParser(),
        )
        val session = service.register(
            firstName = "Novo",
            lastName = "User",
            rawDocument = "111.444.777-35",
            birthDate = LocalDate.of(1992, 3, 3),
            plainPassword = "password1",
            rawEmail = null,
            rawPhone = null,
        )
        assertTrue(state.linked)
        assertEquals(session.user.id, state.linkedUserId)
        assertEquals(FamilyMembershipStatus.ACTIVE, state.statusAfterLink)
        assertNotNull(state.joinedAtAfterLink)
    }

    @Test
    fun `register should create user issue token and attempt pending link`() {
        val (service, linker) = buildService()
        val session = service.register(
            firstName = " Ana ",
            lastName = " Silva ",
            rawDocument = "123.456.789-09",
            birthDate = LocalDate.of(1990, 1, 2),
            plainPassword = "password1",
            rawEmail = " Ana@Example.com ",
            rawPhone = "+55 (11) 98888-7766 ",
        )
        assertTrue(session.accessToken.isNotBlank())
        assertEquals("Ana", session.user.firstName)
        assertEquals("Silva", session.user.lastName)
        assertEquals(IdentifierNormalizer.normalizeDocumentKey("123.456.789-09"), session.user.document)
        assertEquals("ana@example.com", session.user.email)
        assertEquals("5511988887766", session.user.phone)
        assertNull(session.user.lastLoginAt)
        assertEquals(
            IdentifierNormalizer.normalizeDocumentKey("123.456.789-09"),
            linker.lastDocument,
        )
        assertEquals(session.user.id, linker.lastUserId)
    }

    @Test
    fun `register should reject duplicate document`() {
        val id = UUID.randomUUID()
        val existing = sampleUser(id = id, document = "DOC1")
        val (service, _) = buildService(users = mutableMapOf(id to existing))
        assertFailsWith<DuplicateUserDocumentException> {
            service.register(
                firstName = "A",
                lastName = "B",
                rawDocument = "doc1",
                birthDate = LocalDate.of(1990, 1, 1),
                plainPassword = "password1",
                rawEmail = null,
                rawPhone = null,
            )
        }
    }

    @Test
    fun `register should reject duplicate email`() {
        val id = UUID.randomUUID()
        val existing = sampleUser(id = id, document = "D1", email = "x@y.com")
        val (service, _) = buildService(users = mutableMapOf(id to existing))
        assertFailsWith<DuplicateUserEmailException> {
            service.register(
                firstName = "A",
                lastName = "B",
                rawDocument = "D2",
                birthDate = LocalDate.of(1990, 1, 1),
                plainPassword = "password1",
                rawEmail = "X@Y.COM",
                rawPhone = null,
            )
        }
    }

    @Test
    fun `register should reject duplicate phone`() {
        val id = UUID.randomUUID()
        val existing = sampleUser(id = id, document = "D1", phone = "11999999999")
        val (service, _) = buildService(users = mutableMapOf(id to existing))
        assertFailsWith<DuplicateUserPhoneException> {
            service.register(
                firstName = "A",
                lastName = "B",
                rawDocument = "D2",
                birthDate = LocalDate.of(1990, 1, 1),
                plainPassword = "password1",
                rawEmail = null,
                rawPhone = "(11) 99999-9999",
            )
        }
    }

    @Test
    fun `register should reject weak password`() {
        val (service, _) = buildService()
        val ex = assertFailsWith<InvalidUserRegistrationException> {
            service.register(
                firstName = "A",
                lastName = "B",
                rawDocument = "DOC",
                birthDate = LocalDate.of(1990, 1, 1),
                plainPassword = "short",
                rawEmail = null,
                rawPhone = null,
            )
        }
        assertEquals("must be at least 8 characters", ex.details["password"])
    }

    @Test
    fun `register should reject future birth date`() {
        val (service, _) = buildService()
        assertFailsWith<InvalidUserRegistrationException> {
            service.register(
                firstName = "A",
                lastName = "B",
                rawDocument = "DOC",
                birthDate = LocalDate.now().plusDays(1),
                plainPassword = "password1",
                rawEmail = null,
                rawPhone = null,
            )
        }
    }

    @Test
    fun `login should update lastLoginAt`() {
        val id = UUID.randomUUID()
        val user = sampleUser(
            id = id,
            document = "ABC",
            passwordHash = FakePasswordHasher().hash("password1"),
        )
        val (service, _) = buildService(users = mutableMapOf(id to user))
        val session = service.login(rawLogin = "ABC", plainPassword = "password1")
        assertNotNull(session.user.lastLoginAt)
    }

    @Test
    fun `login should accept email identifier`() {
        val id = UUID.randomUUID()
        val user = sampleUser(
            id = id,
            document = "D",
            email = "me@here.com",
            passwordHash = FakePasswordHasher().hash("password1"),
        )
        val (service, _) = buildService(users = mutableMapOf(id to user))
        val session = service.login(rawLogin = "Me@Here.com", plainPassword = "password1")
        assertEquals(id, session.user.id)
    }

    @Test
    fun `login should reject wrong password`() {
        val id = UUID.randomUUID()
        val user = sampleUser(
            id = id,
            document = "D",
            passwordHash = FakePasswordHasher().hash("right"),
        )
        val (service, _) = buildService(users = mutableMapOf(id to user))
        assertFailsWith<InvalidCredentialsException> {
            service.login(rawLogin = "D", plainPassword = "wrong")
        }
    }

    @Test
    fun `login should reject unknown user`() {
        val (service, _) = buildService()
        assertFailsWith<InvalidCredentialsException> {
            service.login(rawLogin = "missing", plainPassword = "password1")
        }
    }

    @Test
    fun `login should reject disabled user`() {
        val id = UUID.randomUUID()
        val user = sampleUser(
            id = id,
            document = "D",
            status = UserStatus.DISABLED,
            passwordHash = FakePasswordHasher().hash("password1"),
        )
        val (service, _) = buildService(users = mutableMapOf(id to user))
        assertFailsWith<UserDisabledException> {
            service.login(rawLogin = "D", plainPassword = "password1")
        }
    }

    @Test
    fun `getAuthenticatedUser should return user`() {
        val id = UUID.randomUUID()
        val user = sampleUser(id = id, document = "D")
        val (service, _) = buildService(users = mutableMapOf(id to user))
        assertEquals(id, service.getAuthenticatedUser(id).id)
    }

    @Test
    fun `getAuthenticatedUser should throw when missing`() {
        val (service, _) = buildService()
        assertFailsWith<UserNotFoundException> {
            service.getAuthenticatedUser(UUID.randomUUID())
        }
    }

    private fun sampleUser(
        id: UUID,
        document: String,
        email: String? = null,
        phone: String? = null,
        passwordHash: String = "HASH::x",
        status: UserStatus = UserStatus.ACTIVE,
    ): User = User(
        id = id,
        firstName = "F",
        lastName = "L",
        document = document,
        birthDate = LocalDate.of(1990, 1, 1),
        passwordHash = passwordHash,
        phone = phone,
        email = email,
        status = status,
        lastLoginAt = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
}

private class FakePasswordHasher : PasswordHasher {
    override fun hash(rawPassword: String): String = "HASH::$rawPassword"

    override fun verify(rawPassword: String, storedHash: String): Boolean =
        storedHash == "HASH::$rawPassword"
}

private class PendingLinkSlot(
    val expectedDocument: String,
    var linked: Boolean = false,
    var linkedUserId: UUID? = null,
    var statusAfterLink: FamilyMembershipStatus = FamilyMembershipStatus.PENDING_REGISTRATION,
    var joinedAtAfterLink: Instant? = null,
)

private class MutatingPendingMemberLinker(
    private val slot: PendingLinkSlot,
) : FamilyPendingMemberLinkRepository {
    override fun linkFirstPendingMemberByDocument(normalizedDocument: String, userId: UUID): Boolean {
        if (normalizedDocument != slot.expectedDocument || slot.linkedUserId != null) {
            return false
        }
        slot.linked = true
        slot.linkedUserId = userId
        slot.statusAfterLink = FamilyMembershipStatus.ACTIVE
        slot.joinedAtAfterLink = Instant.now()
        return true
    }
}

private class RecordingPendingLinker : FamilyPendingMemberLinkRepository {
    var lastDocument: String? = null
    var lastUserId: UUID? = null

    override fun linkFirstPendingMemberByDocument(normalizedDocument: String, userId: UUID): Boolean {
        lastDocument = normalizedDocument
        lastUserId = userId
        return false
    }
}

private class InMemoryUserRepository(
    private val users: MutableMap<UUID, User>,
) : UserRepository {
    override fun findById(id: UUID): User? = users[id]

    override fun findByLoginIdentifier(identifier: LoginIdentifier): User? =
        users.values.firstOrNull { u ->
            when (identifier) {
                is LoginIdentifier.Document -> u.document == identifier.normalized
                is LoginIdentifier.Email -> u.email?.equals(identifier.normalized, ignoreCase = true) == true
                is LoginIdentifier.Phone -> u.phone == identifier.normalized
            }
        }

    override fun existsByDocumentNormalized(document: String): Boolean =
        users.values.any { it.document == document }

    override fun existsByEmailNormalized(email: String): Boolean =
        users.values.any { it.email?.equals(email, ignoreCase = true) == true }

    override fun existsByPhoneDigits(phone: String): Boolean =
        users.values.any { it.phone == phone }

    override fun create(
        firstName: String,
        lastName: String,
        documentNormalized: String,
        birthDate: LocalDate,
        passwordHash: String,
        phoneDigits: String?,
        emailNormalized: String?,
    ): User {
        val id = UUID.randomUUID()
        val now = Instant.now()
        val user = User(
            id = id,
            firstName = firstName,
            lastName = lastName,
            document = documentNormalized,
            birthDate = birthDate,
            passwordHash = passwordHash,
            phone = phoneDigits,
            email = emailNormalized,
            status = UserStatus.ACTIVE,
            lastLoginAt = null,
            createdAt = now,
            updatedAt = now,
        )
        users[id] = user
        return user
    }

    override fun updateLastLoginAt(userId: UUID, at: Instant) {
        val current = users[userId] ?: return
        users[userId] = current.copy(lastLoginAt = at, updatedAt = at)
    }
}
