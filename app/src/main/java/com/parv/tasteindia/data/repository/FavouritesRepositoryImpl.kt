package com.parv.tasteindia.data.repository

import com.parv.tasteindia.data.local.FavouriteMealDao
import com.parv.tasteindia.data.local.FavouriteMealEntity
import com.parv.tasteindia.domain.repository.FavouritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavouritesRepositoryImpl(
    private val dao: FavouriteMealDao,
    private val now: () -> Long = System::currentTimeMillis,
) : FavouritesRepository {

    override fun observeFavouriteIds(): Flow<Set<String>> =
        dao.observeIds().map { it.toSet() }

    override suspend fun isFavourite(id: String): Boolean = dao.exists(id)

    override suspend fun toggle(id: String): Boolean {
        return if (dao.exists(id)) {
            dao.deleteById(id)
            false
        } else {
            dao.insert(FavouriteMealEntity(idMeal = id, addedAt = now()))
            true
        }
    }
}
