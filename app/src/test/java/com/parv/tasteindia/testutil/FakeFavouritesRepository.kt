package com.parv.tasteindia.testutil

import com.parv.tasteindia.domain.repository.FavouritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [FavouritesRepository] with an observable id set. */
class FakeFavouritesRepository(initial: Set<String> = emptySet()) : FavouritesRepository {

    private val ids = MutableStateFlow(initial)

    val current: Set<String> get() = ids.value

    override fun observeFavouriteIds(): Flow<Set<String>> = ids.asStateFlow()

    override suspend fun isFavourite(id: String): Boolean = id in ids.value

    override suspend fun toggle(id: String): Boolean {
        val nowFavourite = id !in ids.value
        ids.value = if (nowFavourite) ids.value + id else ids.value - id
        return nowFavourite
    }
}
