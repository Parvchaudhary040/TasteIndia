package com.parv.tasteindia.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** Thrown before a request leaves the app when there is no network. Maps to AppError.Network. */
class OfflineException : IOException("No network connection")

/**
 * Fails fast when the device is offline, so the user sees the offline state immediately instead
 * of waiting out a connect timeout. [isOnline] is injected (from ConnectivityManager in the app,
 * a fake in tests) so this stays Context-free.
 */
class ConnectivityInterceptor(
    private val isOnline: () -> Boolean,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isOnline()) throw OfflineException()
        return chain.proceed(chain.request())
    }
}
