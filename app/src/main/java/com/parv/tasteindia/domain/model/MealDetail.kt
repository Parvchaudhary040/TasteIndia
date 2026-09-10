package com.parv.tasteindia.domain.model

/**
 * Full recipe detail from `lookup.php?i={id}`.
 *
 * Every optional field is nullable so the details screen can decide, field by field, whether
 * there is something meaningful to show. Blank strings from the API are normalized to `null`
 * during mapping, so the UI only has to check for `null`/empty-list.
 */
data class MealDetail(
    val id: String,
    val name: String,
    val category: String?,
    val area: String?,
    val instructions: String?,
    val thumbnailUrl: String?,
    val tags: List<String>,
    val youtubeUrl: String?,
    val sourceUrl: String?,
    val ingredients: List<Ingredient>,
)
