package com.parv.tasteindia.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CachedMealDetailDaoTest {

    private lateinit var db: TasteIndiaDatabase
    private lateinit var dao: CachedMealDetailDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TasteIndiaDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.cachedMealDetailDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun getById_returns_null_when_absent() = runBlocking {
        assertNull(dao.getById("nope"))
    }

    @Test
    fun upsert_inserts_then_replaces() = runBlocking {
        dao.upsert(CachedMealDetailEntity("52795", payloadJson = """{"v":1}""", cachedAt = 1))
        assertEquals("""{"v":1}""", dao.getById("52795")?.payloadJson)

        dao.upsert(CachedMealDetailEntity("52795", payloadJson = """{"v":2}""", cachedAt = 2))
        assertEquals("""{"v":2}""", dao.getById("52795")?.payloadJson)
    }
}
