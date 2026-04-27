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
import com.dailyback.shared.application.identity.LoginIdentifierParser
import com.dailyback.shared.validation.IdentifierNormalizer
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class UserAuthService(
    private val userRepository: UserRepository,
    private val pendingMemberLinkRepository: FamilyPendingMemberLinkRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtTokenService: JwtTokenService,
    private val loginIdentifierParser: LoginIdentifierParser,
) {
    fun register(
        firstName: String,
        lastName: String,
        rawDocument: String,
        birthDate: LocalDate,
        plainPassword: String,
        rawEmail: String?,
        rawPhone: String?,
    ): AuthSession {
        val normalizedFirst = firstName.trim()
        val normalizedLast = lastName.trim()
        val documentKey = IdentifierNormalizer.normalizeDocumentKey(rawDocument)
        val emailNormalized = rawEmail?.trim()?.takeIf { it.isNotEmpty() }?.let(IdentifierNormalizer::normalizeEmail)
        val phoneDigits = rawPhone?.trim()?.takeIf { it.isNotEmpty() }?.let(IdentifierNormalizer::digitsOnly)

        validateRegistration(
            normalizedFirst = normalizedFirst,
            normalizedLast = normalizedLast,
            documentKey = documentKey,
            birthDate = birthDate,
            plainPassword = plainPassword,
            emailNormalized = emailNormalized,
            phoneDigits = phoneDigits,
        )

        if (userRepository.existsByDocumentNormalized(documentKey)) {
            throw DuplicateUserDocumentException(documentKey)
        }
        if (emailNormalized != null && userRepository.existsByEmailNormalized(emailNormalized)) {
            throw DuplicateUserEmailException()
        }
        if (phoneDigits != null && userRepository.existsByPhoneDigits(phoneDigits)) {
            throw DuplicateUserPhoneException()
        }

        val passwordHash = passwordHasher.hash(plainPassword)

        val user = userRepository.create(
            firstName = normalizedFirst,
            lastName = normalizedLast,
            documentNormalized = documentKey,
            birthDate = birthDate,
            passwordHash = passwordHash,
            phoneDigits = phoneDigits,
            emailNormalized = emailNormalized,
        )
        pendingMemberLinkRepository.linkFirstPendingMemberByDocument(documentKey, user.id)

        val issued = jwtTokenService.createAccessToken(user.id)
        return AuthSession(
            accessToken = issued.token,
            expiresInSeconds = issued.expiresInSeconds,
            user = user,
        )
    }

    fun login(
        rawLogin: String,
        plainPassword: String,
    ): AuthSession {
        val identifier = try {
            loginIdentifierParser.parse(rawLogin)
        } catch (_: IllegalArgumentException) {
            throw InvalidCredentialsException()
        }

        val user = userRepository.findByLoginIdentifier(identifier) ?: throw InvalidCredentialsException()

        if (user.status == UserStatus.DISABLED) {
            throw UserDisabledException()
        }

        if (!passwordHasher.verify(plainPassword, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        val now = Instant.now()
        userRepository.updateLastLoginAt(user.id, now)

        val refreshed = userRepository.findById(user.id) ?: throw InvalidCredentialsException()
        val issued = jwtTokenService.createAccessToken(refreshed.id)
        return AuthSession(
            accessToken = issued.token,
            expiresInSeconds = issued.expiresInSeconds,
            user = refreshed,
        )
    }

    fun getAuthenticatedUser(userId: UUID): User =
        userRepository.findById(userId) ?: throw UserNotFoundException(userId)

    private fun validateRegistration(
        normalizedFirst: String,
        normalizedLast: String,
        documentKey: String,
        birthDate: LocalDate,
        plainPassword: String,
        emailNormalized: String?,
        phoneDigits: String?,
    ) {
        val details = mutableMapOf<String, String>()
        if (normalizedFirst.isBlank()) {
            details["firstName"] = "must not be blank"
        }
        if (normalizedLast.isBlank()) {
            details["lastName"] = "must not be blank"
        }
        if (documentKey.isBlank()) {
            details["document"] = "must not be blank"
        }
        if (birthDate.isAfter(LocalDate.now())) {
            details["birthDate"] = "must not be in the future"
        }
        if (plainPassword.length < MIN_PASSWORD_LENGTH) {
            details["password"] = "must be at least $MIN_PASSWORD_LENGTH characters"
        }
        if (emailNormalized != null && !isValidEmail(emailNormalized)) {
            details["email"] = "invalid format"
        }
        if (phoneDigits != null && phoneDigits.length < MIN_PHONE_DIGITS) {
            details["phone"] = "invalid format"
        }
        if (details.isNotEmpty()) {
            throw InvalidUserRegistrationException(details)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val at = email.indexOf('@')
        if (at <= 0 || at >= email.lastIndex) return false
        return email.substring(at + 1).contains('.')
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MIN_PHONE_DIGITS = 10
    }
}

data class AuthSession(
    val accessToken: String,
    val expiresInSeconds: Long,
    val user: User,
)
