package com.parv.tasteindia.testutil

import com.parv.tasteindia.data.local.FavouriteMealDao
import com.parv.tasteindia.data.local.FavouriteMealEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory stand-in for the Room favourites DAO (ordered most-recent-first, like the query). */
class FakeFavouriteMealDao : FavouriteMealDao {

    private val rows = MutableStateFlow<List<FavouriteMealEntity>>(emptyList())

    override fun observeIds(): Flow<List<String>> =
        rows.map { list -> list.sortedByDescending { it.addedAt }.map { it.idMeal } }

    override suspend fun exists(id: String): Boolean = rows.value.any { it.idMeal == id }

    override suspend fun insert(entity: FavouriteMealEntity) {
        if (rows.value.none { it.idMeal == entity.idMeal }) {
            rows.value = rows.value + entity
        }
    }

    override suspend fun deleteById(id: String) {
        rows.value = rows.value.filterNot { it.idMeal == id }
    }
}
