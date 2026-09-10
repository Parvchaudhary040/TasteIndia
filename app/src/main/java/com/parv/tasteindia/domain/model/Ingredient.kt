package com.parv.tasteindia.domain.model

/**
 * A single normalized ingredient/measure pair.
 *
 * TheMealDB spreads ingredients across `strIngredient1..20` and `strMeasure1..20` with a mix
 * of `null`, `""` and `" "` for unused slots. The mapping layer collapses those into this
 * model, keeping only pairs that have a non-blank ingredient name.
 */
data class Ingredient(
    val name: String,
    val measure: String,
)
