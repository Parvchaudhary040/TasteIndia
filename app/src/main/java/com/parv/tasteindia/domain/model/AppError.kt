package com.parv.tasteindia.domain.model

/**
 * Typed, exhaustive failure reasons surfaced by the data layer.
 *
 * Note: "no results" is NOT represented here. TheMealDB returns HTTP 200 with `{"meals":null}`
 * for an empty match, which is a successful-but-empty state, not an error. Conflating the two
 * is the classic bug with this API, so empty stays on the success path as an empty list.
 */
sealed interface AppError {
    /** No connectivity, DNS failure, or the connection dropped. */
    data object Network : AppError

    /** A configured OkHttp timeout (connect/read/call) elapsed. */
    data object Timeout : AppError

    /** Server replied with a non-2xx status. */
    data class Http(val code: Int) : AppError

    /**
     * A `lookup.php` call succeeded but contained no meal for the requested id. Distinct from
     * the empty-list case (which is normal for filter/search) because a details screen has
     * nothing to render without a meal.
     */
    data object NotFound : AppError

    /** Response body could not be parsed into the expected shape. */
    data object Serialization : AppError

    /** Anything not covered above. */
    data object Unknown : AppError
}
