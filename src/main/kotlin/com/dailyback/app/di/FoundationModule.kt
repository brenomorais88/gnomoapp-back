package com.dailyback.app.di

import com.dailyback.shared.application.identity.LoginIdentifierParser
import org.koin.dsl.module

fun foundationModule() = module {
    single { LoginIdentifierParser() }
}
