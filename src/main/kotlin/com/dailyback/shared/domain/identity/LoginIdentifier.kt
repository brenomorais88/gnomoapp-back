package com.dailyback.shared.domain.identity

sealed class LoginIdentifier {
    abstract val normalized: String

    data class Document(override val normalized: String) : LoginIdentifier()

    data class Email(override val normalized: String) : LoginIdentifier()

    data class Phone(override val normalized: String) : LoginIdentifier()
}
