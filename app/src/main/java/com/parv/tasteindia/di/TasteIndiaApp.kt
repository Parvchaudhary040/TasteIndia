package com.parv.tasteindia.di

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade

/**
 * Owns the single [AppContainer] for the process lifetime. ViewModels read it through
 * `AndroidViewModelFactory` / `APPLICATION_KEY`, so no Activity or Context is retained
 * anywhere longer than it should be.
 *
 * Also supplies Coil's singleton [ImageLoader] so image requests reuse the app's one
 * [okhttp3.OkHttpClient] (shared connection pool + timeout policy) instead of Coil spinning
 * up its own.
 */
class TasteIndiaApp : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { container.okHttpClient }))
            }
            .crossfade(true)
            .build()
}
