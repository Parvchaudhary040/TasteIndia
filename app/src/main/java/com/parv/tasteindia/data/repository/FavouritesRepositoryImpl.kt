package com.parv.tasteindia.data.repository

import com.parv.tasteindia.data.local.FavouriteMealDao
import com.parv.tasteindia.data.local.FavouriteMealEntity
import com.parv.tasteindia.domain.repository.FavouritesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavouritesRepositoryImpl(
    private val dao: FavouriteMealDao,
    private val now: () -> Long = System::currentTimeMillis,
) : FavouritesRepository {

    override fun observeFavouriteIds(): Flow<Set<String>> =
        dao.observeIds().map { it.toSet() }

    override suspend fun isFavourite(id: String): Boolean = dao.exists(id)

    /**
     * A Room failure (disk full, corruption, ...) must not crash the caller — toggle() runs
     * inside a bare `viewModelScope.launch {}` with no exception handler in every ViewModel that
     * calls it. On failure, nothing was written, so the pre-toggle state is reported unchanged
     * and the exception does not propagate. [CancellationException] is deliberately rethrown so
     * structured concurrency keeps working.
     */
    override suspend fun toggle(id: String): Boolean {
        val alreadyFavourite = try {
            dao.exists(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return false
        }

        return try {
            if (alreadyFavourite) {
                dao.deleteById(id)
                false
            } else {
                dao.insert(FavouriteMealEntity(idMeal = id, addedAt = now()))
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            alreadyFavourite
        }
    }
}
