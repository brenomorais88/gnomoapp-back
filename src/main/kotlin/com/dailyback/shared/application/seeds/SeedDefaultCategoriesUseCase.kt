package com.dailyback.shared.application.seeds

import com.dailyback.shared.domain.seeds.DefaultCategoriesSeedProvider

class SeedDefaultCategoriesUseCase(
    private val categorySeedRepository: CategorySeedRepository,
    private val defaultCategoriesSeedProvider: DefaultCategoriesSeedProvider,
) {
    fun execute() {
        defaultCategoriesSeedProvider.categories().forEach(categorySeedRepository::upsertByName)
    }
}

interface CategorySeedRepository {
    fun upsertByName(name: String)
}
