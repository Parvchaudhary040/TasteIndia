package com.parv.tasteindia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Two tables: favourite IDs (the one piece of truly persistent user state) and a detail cache
 * (a performance aid). `exportSchema = false` — no migration history is shipped with an
 * assignment app; a schema bump would use `fallbackToDestructiveMigration()`.
 */
@Database(
    entities = [FavouriteMealEntity::class, CachedMealDetailEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TasteIndiaDatabase : RoomDatabase() {

    abstract fun favouriteMealDao(): FavouriteMealDao
    abstract fun cachedMealDetailDao(): CachedMealDetailDao

    companion object {
        fun build(context: Context): TasteIndiaDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                TasteIndiaDatabase::class.java,
                "tasteindia.db",
            ).build()
    }
}
