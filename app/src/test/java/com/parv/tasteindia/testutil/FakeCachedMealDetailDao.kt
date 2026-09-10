package com.parv.tasteindia.testutil

import com.parv.tasteindia.data.local.CachedMealDetailDao
import com.parv.tasteindia.data.local.CachedMealDetailEntity

/** In-memory stand-in for the Room detail cache. */
class FakeCachedMealDetailDao : CachedMealDetailDao {

    val store = linkedMapOf<String, CachedMealDetailEntity>()

    override suspend fun getById(id: String): CachedMealDetailEntity? = store[id]

    override suspend fun upsert(entity: CachedMealDetailEntity) {
        store[entity.idMeal] = entity
    }
}
