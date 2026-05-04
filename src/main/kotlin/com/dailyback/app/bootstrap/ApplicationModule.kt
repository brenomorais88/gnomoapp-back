package com.dailyback.app.bootstrap

import com.dailyback.app.config.AppConfig
import com.dailyback.app.config.SchedulerConfig
import com.dailyback.app.di.coreModule
import com.dailyback.app.startup.StartupInitializer
import com.dailyback.features.accountoccurrences.api.occurrenceRoutes
import com.dailyback.features.accountoccurrences.application.GetOccurrenceByIdUseCase
import com.dailyback.features.accountoccurrences.application.ListOccurrencesUseCase
import com.dailyback.features.accountoccurrences.application.MarkOccurrencePaidUseCase
import com.dailyback.features.accountoccurrences.application.OverrideOccurrenceAmountUseCase
import com.dailyback.features.accountoccurrences.application.UnmarkOccurrencePaidUseCase
import com.dailyback.features.accounts.api.accountRoutes
import com.dailyback.features.accounts.application.ActivateAccountUseCase
import com.dailyback.features.accounts.application.AccountAccessContextResolver
import com.dailyback.features.accounts.application.CreateAccountUseCase
import com.dailyback.features.accounts.application.DeactivateAccountUseCase
import com.dailyback.features.accounts.application.DeleteAccountUseCase
import com.dailyback.features.accounts.application.GetAccountByIdUseCase
import com.dailyback.features.accounts.application.ListAccountsUseCase
import com.dailyback.features.accounts.application.UpdateAccountUseCase
import com.dailyback.features.categories.api.categoryRoutes
import com.dailyback.features.categories.application.CategoryRepository
import com.dailyback.features.categories.application.CreateCategoryUseCase
import com.dailyback.features.categories.application.DeleteCategoryUseCase
import com.dailyback.features.categories.application.GetCategoryByIdUseCase
import com.dailyback.features.categories.application.ListCategoriesUseCase
import com.dailyback.features.categories.application.UpdateCategoryUseCase
import com.dailyback.features.dashboard.api.dashboardRoutes
import com.dailyback.features.dashboard.application.GetDashboardCategorySummaryUseCase
import com.dailyback.features.families.api.familyRoutes
import com.dailyback.features.families.application.FamilyMemberPermissionService
import com.dailyback.features.families.application.FamilyMemberRepository
import com.dailyback.features.families.application.FamilyPermissionAuthorizer
import com.dailyback.features.families.application.FamilyService
import com.dailyback.features.families.domain.FamilyPermissionDeniedException
import com.dailyback.features.users.api.authRoutes
import com.dailyback.features.users.application.JwtTokenService
import com.dailyback.features.users.application.UserAuthService
import com.dailyback.features.dashboard.application.GetDashboardDayDetailsUseCase
import com.dailyback.features.dashboard.application.GetDashboardHomeSummaryUseCase
import com.dailyback.features.dashboard.application.GetDashboardNext12MonthsProjectionUseCase
import com.dailyback.shared.api.routes.healthRoutes
import com.dailyback.shared.application.maintenance.RecurrenceMaintenanceService
import com.dailyback.shared.application.health.GetHealthStatusUseCase
import com.dailyback.shared.domain.health.DatabaseHealthChecker
import com.dailyback.shared.errors.ApiErrorResponse
import com.dailyback.shared.errors.ApiException
import com.dailyback.shared.errors.ErrorCodes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.koin.core.context.GlobalContext
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun Application.module(
    appConfig: AppConfig = AppConfig.fromEnvironment(),
    databaseHealthCheckerOverride: DatabaseHealthChecker? = null,
    categoryRepositoryOverride: CategoryRepository? = null,
    userAuthServiceOverride: UserAuthService? = null,
    familyPermissionAuthorizerOverride: FamilyPermissionAuthorizer? = null,
    familyMemberRepositoryOverride: FamilyMemberRepository? = null,
    runStartup: Boolean = true,
) {
    install(CallLogging)
    install(CORS) {
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Patch)
        allowMethod(io.ktor.http.HttpMethod.Delete)
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        allowCredentials = true

        // Local frontend development
        allowHost(host = "localhost:3000", schemes = listOf("http"))
        allowHost(host = "localhost:3001", schemes = listOf("http"))
        allowHost(host = "127.0.0.1:3000", schemes = listOf("http"))
        allowHost(host = "127.0.0.1:3001", schemes = listOf("http"))

        // Public frontend domains
        allowHost(host = "www.gnomoapp.com.br", schemes = listOf("https"))
        allowHost(host = "hml.gnomoapp.com.br", schemes = listOf("https"))
    }
    install(ContentNegotiation) {
        json()
    }
    install(Koin) {
        modules(
            coreModule(
                appConfig,
                databaseHealthCheckerOverride,
                categoryRepositoryOverride,
                userAuthServiceOverride,
                familyPermissionAuthorizerOverride,
                familyMemberRepositoryOverride,
            ),
        )
    }
    install(Authentication) {
        val jwtTokenService: JwtTokenService = GlobalContext.get().get(JwtTokenService::class)
        jwt("auth-jwt") {
            realm = "daily-back"
            verifier(jwtTokenService.verifier)
            validate { credential ->
                val subject = credential.payload.subject ?: return@validate null
                if (runCatching { UUID.fromString(subject) }.isFailure) {
                    return@validate null
                }
                JWTPrincipal(credential.payload)
            }
        }
    }
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respond(
                status = cause.statusCode,
                message = ApiErrorResponse(
                    timestamp = Instant.now().toString(),
                    path = call.request.local.uri,
                    errorCode = cause.errorCode,
                    message = cause.message,
                    details = cause.details,
                    traceId = UUID.randomUUID().toString(),
                ),
            )
        }

        exception<FamilyPermissionDeniedException> { call, cause ->
            call.respond(
                status = io.ktor.http.HttpStatusCode.Forbidden,
                message = ApiErrorResponse(
                    timestamp = Instant.now().toString(),
                    path = call.request.local.uri,
                    errorCode = ErrorCodes.FAMILY_PERMISSION_DENIED,
                    message = cause.message ?: "Permission denied",
                    details = mapOf("permission" to cause.permission.name),
                    traceId = UUID.randomUUID().toString(),
                ),
            )
        }

        exception<Throwable> { call, cause ->
            call.respond(
                status = io.ktor.http.HttpStatusCode.InternalServerError,
                message = ApiErrorResponse(
                    timestamp = Instant.now().toString(),
                    path = call.request.local.uri,
                    errorCode = ErrorCodes.INTERNAL_ERROR,
                    message = "An unexpected error occurred",
                    details = mapOf("reason" to (cause.message ?: "unknown")),
                    traceId = UUID.randomUUID().toString(),
                ),
            )
        }
    }

    if (runStartup) {
        val startupInitializer by inject<StartupInitializer>()
        startupInitializer.initialize()
    }

    val getHealthStatusUseCase by inject<GetHealthStatusUseCase>()
    val listCategoriesUseCase by inject<ListCategoriesUseCase>()
    val getCategoryByIdUseCase by inject<GetCategoryByIdUseCase>()
    val createCategoryUseCase by inject<CreateCategoryUseCase>()
    val updateCategoryUseCase by inject<UpdateCategoryUseCase>()
    val deleteCategoryUseCase by inject<DeleteCategoryUseCase>()
    val listAccountsUseCase by inject<ListAccountsUseCase>()
    val getAccountByIdUseCase by inject<GetAccountByIdUseCase>()
    val createAccountUseCase by inject<CreateAccountUseCase>()
    val updateAccountUseCase by inject<UpdateAccountUseCase>()
    val deleteAccountUseCase by inject<DeleteAccountUseCase>()
    val activateAccountUseCase by inject<ActivateAccountUseCase>()
    val deactivateAccountUseCase by inject<DeactivateAccountUseCase>()
    val listOccurrencesUseCase by inject<ListOccurrencesUseCase>()
    val getOccurrenceByIdUseCase by inject<GetOccurrenceByIdUseCase>()
    val markOccurrencePaidUseCase by inject<MarkOccurrencePaidUseCase>()
    val unmarkOccurrencePaidUseCase by inject<UnmarkOccurrencePaidUseCase>()
    val overrideOccurrenceAmountUseCase by inject<OverrideOccurrenceAmountUseCase>()
    val getDashboardHomeSummaryUseCase by inject<GetDashboardHomeSummaryUseCase>()
    val getDashboardDayDetailsUseCase by inject<GetDashboardDayDetailsUseCase>()
    val getDashboardNext12MonthsProjectionUseCase by inject<GetDashboardNext12MonthsProjectionUseCase>()
    val getDashboardCategorySummaryUseCase by inject<GetDashboardCategorySummaryUseCase>()
    val schedulerConfig by inject<SchedulerConfig>()
    val recurrenceMaintenanceService by inject<RecurrenceMaintenanceService>()
    val userAuthService by inject<UserAuthService>()
    val familyService by inject<FamilyService>()
    val familyMemberPermissionService by inject<FamilyMemberPermissionService>()
    val familyPermissionAuthorizer by inject<FamilyPermissionAuthorizer>()
    val accountAccessContextResolver by inject<AccountAccessContextResolver>()

    if (runStartup && schedulerConfig.recurrenceMaintenanceEnabled) {
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        val intervalHours = schedulerConfig.recurrenceMaintenanceIntervalHours.coerceAtLeast(1)
        scheduler.scheduleAtFixedRate(
            { recurrenceMaintenanceService.execute() },
            intervalHours,
            intervalHours,
            TimeUnit.HOURS,
        )
        monitor.subscribe(io.ktor.server.application.ApplicationStopping) {
            scheduler.shutdownNow()
        }
    }

    routing {
        authRoutes(userAuthService)
        authenticate("auth-jwt") {
            familyRoutes(
                familyService = familyService,
                familyMemberPermissionService = familyMemberPermissionService,
                familyPermissionAuthorizer = familyPermissionAuthorizer,
            )
            categoryRoutes(
                familyPermissionAuthorizer = familyPermissionAuthorizer,
                accountAccessContextResolver = accountAccessContextResolver,
                listCategoriesUseCase = listCategoriesUseCase,
                getCategoryByIdUseCase = getCategoryByIdUseCase,
                createCategoryUseCase = createCategoryUseCase,
                updateCategoryUseCase = updateCategoryUseCase,
                deleteCategoryUseCase = deleteCategoryUseCase,
            )
            accountRoutes(
                familyPermissionAuthorizer = familyPermissionAuthorizer,
                listAccountsUseCase = listAccountsUseCase,
                getAccountByIdUseCase = getAccountByIdUseCase,
                createAccountUseCase = createAccountUseCase,
                updateAccountUseCase = updateAccountUseCase,
                deleteAccountUseCase = deleteAccountUseCase,
                activateAccountUseCase = activateAccountUseCase,
                deactivateAccountUseCase = deactivateAccountUseCase,
            )
            occurrenceRoutes(
                familyPermissionAuthorizer = familyPermissionAuthorizer,
                listOccurrencesUseCase = listOccurrencesUseCase,
                getOccurrenceByIdUseCase = getOccurrenceByIdUseCase,
                markOccurrencePaidUseCase = markOccurrencePaidUseCase,
                unmarkOccurrencePaidUseCase = unmarkOccurrencePaidUseCase,
                overrideOccurrenceAmountUseCase = overrideOccurrenceAmountUseCase,
            )
            dashboardRoutes(
                familyPermissionAuthorizer = familyPermissionAuthorizer,
                accountAccessContextResolver = accountAccessContextResolver,
                getDashboardHomeSummaryUseCase = getDashboardHomeSummaryUseCase,
                getDashboardDayDetailsUseCase = getDashboardDayDetailsUseCase,
                getDashboardNext12MonthsProjectionUseCase = getDashboardNext12MonthsProjectionUseCase,
                getDashboardCategorySummaryUseCase = getDashboardCategorySummaryUseCase,
            )
        }
        healthRoutes(getHealthStatusUseCase)
        swaggerUI(path = "swagger", swaggerFile = "openapi/openapi.yaml")
        get("/") {
            call.respond(mapOf("service" to "daily-back", "status" to "running"))
        }
    }
}
