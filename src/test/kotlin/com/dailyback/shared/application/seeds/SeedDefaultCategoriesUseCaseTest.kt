package com.dailyback.shared.application.seeds

import com.dailyback.shared.domain.seeds.DefaultCategoriesSeedProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class SeedDefaultCategoriesUseCaseTest {

    @Test
    fun `should seed all default categories`() {
        val repository = InMemoryCategorySeedRepository()
        val useCase = SeedDefaultCategoriesUseCase(
            categorySeedRepository = repository,
            defaultCategoriesSeedProvider = DefaultCategoriesSeedProvider(),
        )

        useCase.execute()

        assertEquals(9, repository.savedNames.size)
    }

    @Test
    fun `should be idempotent when executed multiple times`() {
        val repository = InMemoryCategorySeedRepository()
        val useCase = SeedDefaultCategoriesUseCase(
            categorySeedRepository = repository,
            defaultCategoriesSeedProvider = DefaultCategoriesSeedProvider(),
        )

        useCase.execute()
        useCase.execute()

        assertEquals(9, repository.savedNames.size)
        assertEquals(18, repository.totalCalls)
    }
}

private class InMemoryCategorySeedRepository : CategorySeedRepository {
    val savedNames = linkedSetOf<String>()
    var totalCalls: Int = 0
        private set

    override fun upsertByName(name: String) {
        totalCalls += 1
        savedNames.add(name)
    }
}
