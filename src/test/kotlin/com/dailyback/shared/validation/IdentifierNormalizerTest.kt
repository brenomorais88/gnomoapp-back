package com.dailyback.shared.validation

import kotlin.test.Test
import kotlin.test.assertEquals

class IdentifierNormalizerTest {

    @Test
    fun `should normalize email`() {
        assertEquals("a@b.co", IdentifierNormalizer.normalizeEmail("  A@B.CO "))
    }

    @Test
    fun `should extract digits`() {
        assertEquals("1199", IdentifierNormalizer.digitsOnly("11-99 x"))
    }

    @Test
    fun `should normalize document key`() {
        assertEquals("AB12", IdentifierNormalizer.normalizeDocumentKey("a-b-1-2"))
    }
}
