package com.dailyback.app.di

import com.dailyback.app.config.AppConfig
import com.dailyback.app.startup.StartupInitializer
import com.dailyback.features.accountoccurrences.application.GetOccurrenceByIdUseCase
import com.dailyback.features.accountoccurrences.application.ListOccurrencesUseCase
import com.dailyback.features.accountoccurrences.application.MarkOccurrencePaidUseCase
import com.dailyback.features.accountoccurrences.application.OccurrenceRepository
import com.dailyback.features.accountoccurrences.application.OverrideOccurrenceAmountUseCase
import com.dailyback.features.accountoccurrences.application.UnmarkOccurrencePaidUseCase
import com.dailyback.features.accountoccurrences.infrastructure.ExposedOccurrenceRepository
import com.dailyback.features.accounts.application.AccountAccessContextResolver
import com.dailyback.features.accounts.application.AccountRepository
import com.dailyback.features.accounts.application.ActivateAccountUseCase
import com.dailyback.features.accounts.application.CreateAccountUseCase
import com.dailyback.features.accounts.application.DeactivateAccountUseCase
import com.dailyback.features.accounts.application.DeleteAccountUseCase
import com.dailyback.features.accounts.application.GetAccountByIdUseCase
import com.dailyback.features.accounts.application.ListAccountsUseCase
import com.dailyback.features.accounts.application.RecurrenceGenerationService
import com.dailyback.features.accounts.application.UpdateAccountUseCase
import com.dailyback.features.accounts.infrastructure.ExposedAccountRepository
import com.dailyback.features.categories.application.CategoryRepository
import com.dailyback.features.categories.application.CreateCategoryUseCase
import com.dailyback.features.categories.application.DeleteCategoryUseCase
import com.dailyback.features.categories.application.GetCategoryByIdUseCase
import com.dailyback.features.categories.application.ListCategoriesUseCase
import com.dailyback.features.categories.application.UpdateCategoryUseCase
import com.dailyback.features.categories.infrastructure.ExposedCategoryRepository
import com.dailyback.features.dashboard.application.GetDashboardCategorySummaryUseCase
import com.dailyback.features.dashboard.application.GetDashboardDayDetailsUseCase
import com.dailyback.features.dashboard.application.GetDashboardHomeSummaryUseCase
import com.dailyback.features.dashboard.application.GetDashboardNext12MonthsProjectionUseCase
import com.dailyback.features.families.application.DefaultFamilyPermissionAuthorizer
import com.dailyback.features.families.application.FamilyMemberPermissionRepository
import com.dailyback.features.families.application.FamilyMemberPermissionService
import com.dailyback.features.families.application.FamilyMemberRepository
import com.dailyback.features.families.application.FamilyPendingMemberLinkRepository
import com.dailyback.features.families.application.FamilyPermissionAuthorizer
import com.dailyback.features.families.application.FamilyRepository
import com.dailyback.features.families.application.FamilyService
import com.dailyback.features.families.infrastructure.ExposedFamilyMemberPermissionRepository
import com.dailyback.features.families.infrastructure.ExposedFamilyMemberRepository
import com.dailyback.features.families.infrastructure.ExposedFamilyPendingMemberLinkRepository
import com.dailyback.features.families.infrastructure.ExposedFamilyRepository
import com.dailyback.features.users.application.JwtTokenService
import com.dailyback.features.users.application.PasswordHasher
import com.dailyback.features.users.application.UserAuthService
import com.dailyback.features.users.application.UserRepository
import com.dailyback.features.users.infrastructure.BcryptPasswordHasher
import com.dailyback.features.users.infrastructure.ExposedUserRepository
import com.dailyback.shared.application.maintenance.RecurrenceMaintenanceService
import com.dailyback.shared.application.health.GetHealthStatusUseCase
import com.dailyback.shared.application.seeds.CategorySeedRepository
import com.dailyback.shared.application.seeds.SeedDefaultCategoriesUseCase
import com.dailyback.shared.application.seeds.SeedScenarioDataUseCase
import com.dailyback.shared.domain.health.DatabaseHealthChecker
import com.dailyback.shared.domain.seeds.DefaultCategoriesSeedProvider
import com.dailyback.shared.infrastructure.database.DatabaseFactory
import com.dailyback.shared.infrastructure.database.ExposedDatabaseHealthChecker
import com.dailyback.shared.infrastructure.database.seeds.ExposedCategorySeedRepository
import com.dailyback.shared.infrastructure.migration.FlywayRunner
import com.dailyback.shared.time.UtcClock
import org.koin.dsl.module

fun coreModule(
    appConfig: AppConfig,
    databaseHealthCheckerOverride: DatabaseHealthChecker? = null,
    categoryRepositoryOverride: CategoryRepository? = null,
    userAuthServiceOverride: UserAuthService? = null,
    familyPermissionAuthorizerOverride: FamilyPermissionAuthorizer? = null,
    familyMemberRepositoryOverride: FamilyMemberRepository? = null,
) = module {
    includes(foundationModule())

    single { appConfig }
    single { appConfig.database }
    single { appConfig.flyway }
    single { appConfig.seed }
    single { appConfig.scheduler }

    single { UtcClock() }
    single { DatabaseFactory(get()) }
    single { FlywayRunner(get(), get()) }
    single { DefaultCategoriesSeedProvider() }

    if (databaseHealthCheckerOverride != null) {
        single<DatabaseHealthChecker> { databaseHealthCheckerOverride }
    } else {
        single<DatabaseHealthChecker> { ExposedDatabaseHealthChecker(get()) }
    }

    if (categoryRepositoryOverride != null) {
        single<CategoryRepository> { categoryRepositoryOverride }
    } else {
        single<CategoryRepository> { ExposedCategoryRepository(get()) }
    }

    single<AccountRepository> { ExposedAccountRepository(get()) }
    single<OccurrenceRepository> { ExposedOccurrenceRepository(get()) }

    single<CategorySeedRepository> { ExposedCategorySeedRepository(get()) }
    single { SeedDefaultCategoriesUseCase(get(), get()) }
    single { SeedScenarioDataUseCase(get()) }
    single { ListCategoriesUseCase(get(), get()) }
    single { GetCategoryByIdUseCase(get(), get()) }
    single { CreateCategoryUseCase(get(), get()) }
    single { UpdateCategoryUseCase(get(), get()) }
    single { DeleteCategoryUseCase(get(), get()) }

    single { RecurrenceGenerationService() }
    single { AccountAccessContextResolver(get(), get()) }
    single { ListAccountsUseCase(get(), get()) }
    single { GetAccountByIdUseCase(get(), get()) }
    single { CreateAccountUseCase(get(), get(), get(), get(), get(), get()) }
    single { UpdateAccountUseCase(get(), get(), get(), get(), get(), get()) }
    single { ActivateAccountUseCase(get(), get(), get(), get()) }
    single { DeactivateAccountUseCase(get(), get(), get()) }
    single { DeleteAccountUseCase(get(), get(), get()) }

    single { ListOccurrencesUseCase(get(), get(), get()) }
    single { GetOccurrenceByIdUseCase(get(), get(), get()) }
    single { MarkOccurrencePaidUseCase(get(), get(), get()) }
    single { UnmarkOccurrencePaidUseCase(get(), get(), get()) }
    single { OverrideOccurrenceAmountUseCase(get(), get(), get()) }
    single { GetDashboardHomeSummaryUseCase(get(), get(), get(), get()) }
    single { GetDashboardDayDetailsUseCase(get(), get(), get()) }
    single { GetDashboardCategorySummaryUseCase(get(), get(), get(), get()) }
    single { GetDashboardNext12MonthsProjectionUseCase(get(), get(), get(), get()) }
    single { RecurrenceMaintenanceService(get(), get(), get()) }
    single { GetHealthStatusUseCase(get(), get()) }
    single { StartupInitializer(get(), get(), get(), get(), get(), get()) }

    single { JwtTokenService(get<AppConfig>().security.jwt) }
    single<PasswordHasher> { BcryptPasswordHasher() }
    single<UserRepository> { ExposedUserRepository(get()) }
    single<FamilyRepository> { ExposedFamilyRepository(get()) }
    if (familyMemberRepositoryOverride != null) {
        single<FamilyMemberRepository> { familyMemberRepositoryOverride }
    } else {
        single<FamilyMemberRepository> { ExposedFamilyMemberRepository(get()) }
    }
    single<FamilyMemberPermissionRepository> { ExposedFamilyMemberPermissionRepository(get()) }
    single<FamilyPendingMemberLinkRepository> { ExposedFamilyPendingMemberLinkRepository(get()) }
    single { FamilyMemberPermissionService(get(), get()) }
    single<FamilyPermissionAuthorizer> {
        familyPermissionAuthorizerOverride ?: DefaultFamilyPermissionAuthorizer(get(), get())
    }
    single { FamilyService(get(), get(), get(), get()) }
    single<UserAuthService> {
        userAuthServiceOverride ?: UserAuthService(
            userRepository = get(),
            pendingMemberLinkRepository = get(),
            passwordHasher = get(),
            jwtTokenService = get(),
            loginIdentifierParser = get(),
        )
    }
}
