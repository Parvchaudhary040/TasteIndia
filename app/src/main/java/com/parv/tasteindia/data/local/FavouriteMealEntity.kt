package com.parv.tasteindia.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per favourited meal. Only the ID is persisted; `addedAt` is kept so the Favourites
 * screen can show most-recent-first without an extra sort key.
 */
@Entity(tableName = "favourite_meals")
data class FavouriteMealEntity(
    @PrimaryKey val idMeal: String,
    val addedAt: Long,
)
