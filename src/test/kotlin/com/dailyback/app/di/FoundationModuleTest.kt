package com.dailyback.app.di

import com.dailyback.shared.application.identity.LoginIdentifierParser
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.Test
import kotlin.test.assertNotNull

class FoundationModuleTest {

    @Test
    fun `should expose LoginIdentifierParser`() {
        startKoin { modules(foundationModule()) }
        try {
            assertNotNull(GlobalContext.get().get<LoginIdentifierParser>())
        } finally {
            stopKoin()
        }
    }
}
