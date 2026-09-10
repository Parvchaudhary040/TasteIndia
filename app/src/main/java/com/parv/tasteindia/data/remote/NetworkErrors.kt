package com.parv.tasteindia.data.remote

import com.parv.tasteindia.domain.model.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Single place that turns the exceptions Retrofit/OkHttp/kotlinx-serialization can throw into a
 * typed [AppError]. Kept out of the repository so it can be unit-tested directly and reused.
 *
 * [CancellationException] is deliberately NOT mapped here — callers must let it propagate so
 * structured concurrency / request cancellation keeps working.
 */
fun Throwable.toAppError(): AppError = when (this) {
    is CancellationException -> throw this

    is HttpException -> AppError.Http(code())

    // OkHttp's callTimeout throws a bare InterruptedIOException("timeout"); connect/read
    // timeouts throw SocketTimeoutException (a subclass). Treat both as Timeout.
    is SocketTimeoutException -> AppError.Timeout
    is InterruptedIOException -> AppError.Timeout

    is UnknownHostException -> AppError.Network
    is IOException -> AppError.Network

    is SerializationException -> AppError.Serialization

    else -> AppError.Unknown
}
