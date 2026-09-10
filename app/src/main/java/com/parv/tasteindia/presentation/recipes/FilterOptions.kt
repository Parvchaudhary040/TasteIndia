package com.parv.tasteindia.presentation.recipes

/**
 * Curated category / main-ingredient options for the filter sheet.
 *
 * Why hard-coded and not fetched: the assignment restricts us to `filter.php` (a/c/i),
 * `lookup.php` and `search.php`. `list.php?c=list` / `list.php?i=list` are outside that set, and
 * the Indian base set (`filter.php?a=India`) carries no category/ingredient data to derive from
 * without enriching every row. So we ship a short, fixed list of values that exist in TheMealDB
 * and are relevant to Indian cooking. The Indian-boundary intersection still runs for whatever
 * the user picks, so an option that happens to match nothing just yields the empty state.
 *
 * Values must match TheMealDB's exact strings (they are passed straight to `filter.php`).
 */
object FilterOptions {

    val CATEGORIES: List<String> = listOf(
        "Chicken",
        "Vegetarian",
        "Dessert",
        "Seafood",
        "Lamb",
        "Beef",
        "Breakfast",
        "Side",
    )

    val INGREDIENTS: List<String> = listOf(
        "Garlic",
        "Onion",
        "Ginger",
        "Coriander",
        "Cumin",
        "Turmeric",
        "Garam Masala",
        "Chicken",
        "Tomatoes",
        "Paneer",
        "Rice",
    )
}
