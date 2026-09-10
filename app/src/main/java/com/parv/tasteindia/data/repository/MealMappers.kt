package com.parv.tasteindia.data.repository

import com.parv.tasteindia.data.remote.dto.MealDetailDto
import com.parv.tasteindia.data.remote.dto.MealSummaryDto
import com.parv.tasteindia.domain.model.Ingredient
import com.parv.tasteindia.domain.model.Meal
import com.parv.tasteindia.domain.model.MealDetail

/**
 * DTO -> domain mapping. All the "TheMealDB gives you inconsistent strings" cleanup lives here:
 * trimming, turning blank into `null`/omitted, splitting tags, and normalizing the 20
 * ingredient/measure slots into a clean list.
 */

/** Blank -> null; otherwise trimmed. */
private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

fun MealSummaryDto.toDomain(): Meal = Meal(
    id = idMeal,
    name = strMeal.trim(),
    thumbnailUrl = strMealThumb.cleaned(),
)

fun MealDetailDto.toDomain(): MealDetail = MealDetail(
    id = idMeal,
    name = strMeal.trim(),
    category = strCategory.cleaned(),
    area = strArea.cleaned(),
    instructions = strInstructions.cleaned(),
    thumbnailUrl = strMealThumb.cleaned(),
    tags = splitTags(strTags),
    youtubeUrl = strYoutube.cleaned(),
    sourceUrl = strSource.cleaned(),
    ingredients = normalizeIngredients(ingredientMeasurePairs),
)

/**
 * Comma-separated tag string -> distinct, trimmed, non-blank tags, order preserved.
 * `null`, `""`, `"Vegetarian, ,Spicy"` all handled.
 */
fun splitTags(raw: String?): List<String> =
    raw?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.distinct()
        ?: emptyList()

/**
 * The core normalization the assignment calls out. Input is the positional
 * `strIngredientN` / `strMeasureN` pairs (slot 1..20), each element possibly `null`, `""` or
 * whitespace.
 *
 * Rule: a pair is meaningful iff the ingredient name is non-blank after trimming. The measure
 * is kept as trimmed text, or `""` when the recipe lists an ingredient with no measure. A slot
 * with a measure but no ingredient is noise and is dropped.
 */
fun normalizeIngredients(pairs: List<Pair<String?, String?>>): List<Ingredient> =
    pairs.mapNotNull { (rawName, rawMeasure) ->
        val name = rawName?.trim().orEmpty()
        if (name.isEmpty()) return@mapNotNull null
        Ingredient(name = name, measure = rawMeasure?.trim().orEmpty())
    }
