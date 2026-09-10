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
}
