package com.parv.tasteindia.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes. `Details` carries ONLY `mealId` — the full `Meal`/`MealDetail`
 * is never passed through navigation; the details screen re-fetches (from cache) by ID.
 */
sealed interface Destination {

    /** Branded launch screen: the app name, shown briefly then faded out. */
    @Serializable
    data object Splash : Destination

    /** One-screen welcome / landing page that leads into the recipe list. */
    @Serializable
    data object Welcome : Destination

    @Serializable
    data object Recipes : Destination

    @Serializable
    data object Favourites : Destination

    @Serializable
    data class Details(val mealId: String) : Destination
}
