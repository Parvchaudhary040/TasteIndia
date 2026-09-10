package com.parv.tasteindia.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * On-disk cache of one `lookup.php` response, keyed by meal ID.
 *
 * The payload is stored as the raw serialized [com.parv.tasteindia.data.remote.dto.MealDetailDto]
 * JSON rather than exploded into columns: we only ever read it back by ID, and keeping the
 * DTO->domain mapping in one place (the repository) is simpler than Room TypeConverters for
 * the 20 ingredient/measure pairs. `cachedAt` supports a future TTL if needed.
 */
@Entity(tableName = "cached_meal_details")
data class CachedMealDetailEntity(
    @PrimaryKey val idMeal: String,
    val payloadJson: String,
    val cachedAt: Long,
)
