package com.parv.tasteindia.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test requirement #4 (persistence half): the favourites DAO against a real Room database.
 */
@RunWith(AndroidJUnit4::class)
class FavouriteMealDaoTest {

    private lateinit var db: TasteIndiaDatabase
    private lateinit var dao: FavouriteMealDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TasteIndiaDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.favouriteMealDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insert_exists_delete() = runBlocking {
        assertFalse(dao.exists("52795"))

        dao.insert(FavouriteMealEntity("52795", addedAt = 1))
        assertTrue(dao.exists("52795"))

        dao.deleteById("52795")
        assertFalse(dao.exists("52795"))
    }

    @Test
    fun observeIds_returns_most_recently_added_first() = runBlocking {
        dao.insert(FavouriteMealEntity("a", addedAt = 100))
        dao.insert(FavouriteMealEntity("b", addedAt = 300))
        dao.insert(FavouriteMealEntity("c", addedAt = 200))

        assertEquals(listOf("b", "c", "a"), dao.observeIds().first())
    }

    @Test
    fun insert_is_ignored_on_primary_key_conflict() = runBlocking {
        dao.insert(FavouriteMealEntity("x", addedAt = 1))
        dao.insert(FavouriteMealEntity("x", addedAt = 999))

        assertEquals(listOf("x"), dao.observeIds().first())
    }
}
