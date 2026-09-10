package com.parv.tasteindia.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedMealDetailDao {

    @Query("SELECT * FROM cached_meal_details WHERE idMeal = :id")
    suspend fun getById(id: String): CachedMealDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedMealDetailEntity)
}
