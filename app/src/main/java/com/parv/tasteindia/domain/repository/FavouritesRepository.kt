package com.parv.tasteindia.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Local-only store of favourite meal IDs. Backed by Room, so it works with no network.
 * Only IDs are persisted; the meal data itself is re-resolved from the base set / detail cache.
 */
interface FavouritesRepository {

    /** Emits the current set of favourite IDs and every subsequent change. */
    fun observeFavouriteIds(): Flow<Set<String>>

    suspend fun isFavourite(id: String): Boolean

    /** Adds if absent, removes if present. Returns the new state (`true` = now a favourite). */
    suspend fun toggle(id: String): Boolean
}
