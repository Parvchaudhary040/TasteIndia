package com.parv.tasteindia.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteMealDao {

    @Query("SELECT idMeal FROM favourite_meals ORDER BY addedAt DESC")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favourite_meals WHERE idMeal = :id)")
    suspend fun exists(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: FavouriteMealEntity)

    @Query("DELETE FROM favourite_meals WHERE idMeal = :id")
    suspend fun deleteById(id: String)
}
