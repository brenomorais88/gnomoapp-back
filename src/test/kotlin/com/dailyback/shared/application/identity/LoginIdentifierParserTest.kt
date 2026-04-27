package com.dailyback.shared.application.identity

import com.dailyback.shared.domain.identity.LoginIdentifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class LoginIdentifierParserTest {

    private val parser = LoginIdentifierParser()

    @Test
    fun `should classify email when at sign is present`() {
        val id = parser.parse("  User@Example.COM ")
        assertIs<LoginIdentifier.Email>(id)
        assertEquals("user@example.com", id.normalized)
    }

    @Test
    fun `should classify phone when formatted and digit count in range`() {
        val id = parser.parse("+55 (11) 99988-7766")
        assertIs<LoginIdentifier.Phone>(id)
        assertEquals("5511999887766", id.normalized)
    }

    @Test
    fun `should classify plain digit string as document when no phone formatting`() {
        val id = parser.parse("12345678901")
        assertIs<LoginIdentifier.Document>(id)
        assertEquals("12345678901", id.normalized)
    }

    @Test
    fun `should normalize document letters to uppercase`() {
        val id = parser.parse("ab12cd")
        assertIs<LoginIdentifier.Document>(id)
        assertEquals("AB12CD", id.normalized)
    }

    @Test
    fun `should reject blank login`() {
        assertFailsWith<IllegalArgumentException> {
            parser.parse("   ")
        }
    }
}
