package com.dailyback.features.categories.api

import com.dailyback.app.bootstrap.module
import com.dailyback.app.config.AppConfig
import com.dailyback.app.config.DatabaseConfig
import com.dailyback.app.config.FlywayConfig
import com.dailyback.app.config.SchedulerConfig
import com.dailyback.app.config.SeedConfig
import com.dailyback.app.config.ServerConfig
import com.dailyback.features.categories.application.CategoryRepository
import com.dailyback.features.categories.domain.Category
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.time.Instant
import java.util.UUID

class CategoryRoutesTest {

    @Test
    fun `should create and list categories`() = testApplication {
        val repository = InMemoryRouteCategoryRepository()
        application {
            module(
                appConfig = testConfig(),
                categoryRepositoryOverride = repository,
                runStartup = false,
            )
        }

        val createResponse = client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Leisure","color":"#445566"}""")
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)

        val listResponse = client.get("/categories")

        assertEquals(HttpStatusCode.OK, listResponse.status)
        assertTrue(listResponse.bodyAsText().contains("\"name\":\"Leisure\""))
    }

    @Test
    fun `should reject deletion when category is in use`() = testApplication {
        val repository = InMemoryRouteCategoryRepository()
        val category = repository.create(name = "Debt", color = null)
        repository.inUseIds.add(category.id)
        application {
            module(
                appConfig = testConfig(),
                categoryRepositoryOverride = repository,
                runStartup = false,
            )
        }

        val response = client.delete("/categories/${category.id}")

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(response.bodyAsText().contains("CATEGORY_IN_USE"))
    }

    @Test
    fun `should get category by id`() = testApplication {
        val repository = InMemoryRouteCategoryRepository()
        val category = repository.create(name = "Health", color = "#00AA00")
        application {
            module(
                appConfig = testConfig(),
                categoryRepositoryOverride = repository,
                runStartup = false,
            )
        }

        val response = client.get("/categories/${category.id}")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"name\":\"Health\""))
    }

    @Test
    fun `should reject duplicate category on update`() = testApplication {
        val repository = InMemoryRouteCategoryRepository()
        val first = repository.create(name = "Home", color = null)
        val second = repository.create(name = "Debt", color = null)
        application {
            module(
                appConfig = testConfig(),
                categoryRepositoryOverride = repository,
                runStartup = false,
            )
        }

        val response = client.put("/categories/${second.id}") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Home","color":null}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(response.bodyAsText().contains("CATEGORY_NAME_ALREADY_EXISTS"))
        assertTrue(repository.findById(first.id) != null)
    }

    @Test
    fun `should return bad request for invalid uuid path parameter`() = testApplication {
        val repository = InMemoryRouteCategoryRepository()
        application {
            module(
                appConfig = testConfig(),
                categoryRepositoryOverride = repository,
                runStartup = false,
            )
        }

        val response = client.get("/categories/not-a-uuid")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("INVALID_PATH_PARAMETER"))
    }
}

private class InMemoryRouteCategoryRepository : CategoryRepository {
    private val items = linkedMapOf<UUID, Category>()
    val inUseIds = mutableSetOf<UUID>()

    override fun findAll(): List<Category> = items.values.toList()

    override fun findById(id: UUID): Category? = items[id]

    override fun existsByName(name: String): Boolean = items.values.any { it.name.equals(name, ignoreCase = true) }

    override fun existsByNameExcludingId(name: String, excludedId: UUID): Boolean =
        items.values.any { it.id != excludedId && it.name.equals(name, ignoreCase = true) }

    override fun create(name: String, color: String?): Category {
        val now = Instant.now()
        val category = Category(
            id = UUID.randomUUID(),
            name = name,
            color = color,
            createdAt = now,
            updatedAt = now,
        )
        items[category.id] = category
        return category
    }

    override fun update(id: UUID, name: String, color: String?): Category {
        val previous = items.getValue(id)
        val updated = previous.copy(
            name = name,
            color = color,
            updatedAt = Instant.now(),
        )
        items[id] = updated
        return updated
    }

    override fun deleteById(id: UUID) {
        items.remove(id)
    }

    override fun isCategoryInUse(id: UUID): Boolean = inUseIds.contains(id)
}

private fun testConfig(): AppConfig = AppConfig(
    server = ServerConfig(host = "127.0.0.1", port = 8080),
    database = DatabaseConfig(
        host = "localhost",
        port = 5432,
        name = "daily_test",
        user = "daily",
        password = "daily",
        schema = "public",
        ssl = false,
        jdbcUrlOverride = null,
    ),
    flyway = FlywayConfig(
        enabled = false,
        location = "classpath:db/migration",
    ),
    seed = SeedConfig(enabled = false),
    scheduler = SchedulerConfig(
        recurrenceMaintenanceEnabled = false,
        recurrenceMaintenanceIntervalHours = 24,
    ),
)
