package com.parv.tasteindia.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The "relaunch the app, favourites remain" scenario: write with one database instance, close
 * it (as a process death would), reopen from the same file and read the rows back.
 */
@RunWith(AndroidJUnit4::class)
class FavouritesPersistenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "persistence_test.db"

    private fun openDb() =
        Room.databaseBuilder(context, TasteIndiaDatabase::class.java, dbName).build()

    @Before
    fun clean() {
        context.deleteDatabase(dbName)
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun favourites_survive_closing_and_reopening_the_database() = runBlocking {
        var db = openDb()
        db.favouriteMealDao().insert(FavouriteMealEntity("52795", addedAt = 10))
        db.favouriteMealDao().insert(FavouriteMealEntity("52930", addedAt = 20))
        db.close()

        db = openDb()
        assertEquals(listOf("52930", "52795"), db.favouriteMealDao().observeIds().first())
        db.close()
    }

    @Test
    fun removing_a_favourite_also_persists() = runBlocking {
        var db = openDb()
        db.favouriteMealDao().insert(FavouriteMealEntity("52795", addedAt = 10))
        db.favouriteMealDao().insert(FavouriteMealEntity("52930", addedAt = 20))
        db.favouriteMealDao().deleteById("52795")
        db.close()

        db = openDb()
        assertEquals(listOf("52930"), db.favouriteMealDao().observeIds().first())
        db.close()
    }
}
