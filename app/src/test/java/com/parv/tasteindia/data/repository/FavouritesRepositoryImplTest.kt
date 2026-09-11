package com.parv.tasteindia.data.repository

import com.parv.tasteindia.testutil.FakeFavouriteMealDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavouritesRepositoryImplTest {

    private var clock = 0L
    private fun repo() = FavouritesRepositoryImpl(FakeFavouriteMealDao(), now = { clock++ })

    @Test
    fun `toggle adds then removes and reports the new state`() = runTest {
        val repo = repo()

        assertTrue(repo.toggle("52795"))          // added
        assertTrue(repo.isFavourite("52795"))

        assertFalse(repo.toggle("52795"))         // removed
        assertFalse(repo.isFavourite("52795"))
    }

    @Test
    fun `observeFavouriteIds emits the current set and updates`() = runTest {
        val repo = repo()

        assertEquals(emptySet<String>(), repo.observeFavouriteIds().first())

        repo.toggle("1")
        repo.toggle("2")
        assertEquals(setOf("1", "2"), repo.observeFavouriteIds().first())

        repo.toggle("1")
        assertEquals(setOf("2"), repo.observeFavouriteIds().first())
    }

    @Test
    fun `toggling the same id twice quickly is idempotent (no duplicate rows)`() = runTest {
        val repo = repo()

        repo.toggle("1")
        repo.toggle("1") // remove
        repo.toggle("1") // add again

        assertEquals(setOf("1"), repo.observeFavouriteIds().first())
    }

    @Test
    fun `a DAO failure on insert does not throw and reports the unchanged state`() = runTest {
        val dao = FakeFavouriteMealDao().apply { writeError = RuntimeException("disk full") }
        val repo = FavouritesRepositoryImpl(dao, now = { clock++ })

        val result = repo.toggle("1") // would have been an insert; DAO throws

        assertFalse("insert failed, so this id must not be reported as favourited", result)
        assertEquals(emptySet<String>(), repo.observeFavouriteIds().first())
    }

    @Test
    fun `a DAO failure on delete does not throw and reports the unchanged state`() = runTest {
        val dao = FakeFavouriteMealDao()
        val repo = FavouritesRepositoryImpl(dao, now = { clock++ })
        repo.toggle("1") // succeeds: now favourited

        dao.writeError = RuntimeException("disk full")
        val result = repo.toggle("1") // would have been a delete; DAO throws

        assertTrue("delete failed, so this id must still be reported as favourited", result)
        assertEquals(setOf("1"), repo.observeFavouriteIds().first())
    }
}
