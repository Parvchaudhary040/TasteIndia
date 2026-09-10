package com.parv.tasteindia.di

import android.app.Application

/**
 * Owns the single [AppContainer] for the process lifetime. ViewModels read it through
 * `AndroidViewModelFactory` / `APPLICATION_KEY`, so no Activity or Context is retained
 * anywhere longer than it should be.
 */
class TasteIndiaApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
