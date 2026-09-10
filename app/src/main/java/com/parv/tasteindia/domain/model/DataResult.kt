package com.parv.tasteindia.domain.model

/**
 * Result type for data-layer calls. Deliberately not `kotlin.Result`, because we want a
 * *typed* error ([AppError]) the UI can branch on exhaustively rather than a bare `Throwable`.
 */
sealed interface DataResult<out T> {
    data class Success<out T>(val data: T) : DataResult<T>
    data class Failure(val error: AppError) : DataResult<Nothing>
}

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(data))
    is DataResult.Failure -> this
}

fun <T> DataResult<T>.getOrNull(): T? = (this as? DataResult.Success)?.data
