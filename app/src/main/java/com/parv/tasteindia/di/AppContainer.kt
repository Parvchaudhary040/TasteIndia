package com.parv.tasteindia.di

import android.content.Context
import com.parv.tasteindia.BuildConfig
import com.parv.tasteindia.data.local.TasteIndiaDatabase
import com.parv.tasteindia.data.remote.MealApi
import com.parv.tasteindia.data.remote.NetworkModule
import com.parv.tasteindia.data.repository.FavouritesRepositoryImpl
import com.parv.tasteindia.data.repository.MealRepositoryImpl
import com.parv.tasteindia.domain.repository.FavouritesRepository
import com.parv.tasteindia.domain.repository.MealRepository
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

/**
 * Hand-rolled DI. No Hilt/Koin: the assignment asks for a *lightweight* approach, and a plain
 * container keeps the whole object graph in one readable file with zero annotation processing.
 *
 * Everything is `by lazy`, so nothing (Room open, OkHttp pool) is created until first use.
 * ViewModels receive these via `viewModelFactory` initializers, never by reaching into a
 * static singleton.
 */
interface AppContainer {
    val json: Json
    val okHttpClient: OkHttpClient
    val mealApi: MealApi
    val mealRepository: MealRepository
    val favouritesRepository: FavouritesRepository
}

class DefaultAppContainer(context: Context) : AppContainer {

    private val appContext = context.applicationContext

    override val json: Json by lazy { NetworkModule.json() }

    override val okHttpClient: OkHttpClient by lazy {
        NetworkModule.okHttpClient(enableLogging = BuildConfig.DEBUG)
    }

    private val retrofit by lazy { NetworkModule.retrofit(okHttpClient, json) }

    override val mealApi: MealApi by lazy { NetworkModule.mealApi(retrofit) }

    private val database by lazy { TasteIndiaDatabase.build(appContext) }

    override val mealRepository: MealRepository by lazy {
        MealRepositoryImpl(
            api = mealApi,
            cacheDao = database.cachedMealDetailDao(),
            json = json,
        )
    }

    override val favouritesRepository: FavouritesRepository by lazy {
        FavouritesRepositoryImpl(database.favouriteMealDao())
    }
}
