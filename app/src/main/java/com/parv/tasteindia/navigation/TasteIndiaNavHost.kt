package com.parv.tasteindia.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.parv.tasteindia.presentation.details.DetailsScreen
import com.parv.tasteindia.presentation.favourites.FavouritesScreen
import com.parv.tasteindia.presentation.recipes.RecipesScreen

/**
 * Single-Activity navigation graph. Recipes is the start destination; Details is reached with
 * just a meal ID so the Recipes back-stack entry (and its ViewModel + list state) survives
 * Recipes -> Details -> Back untouched.
 */
@Composable
fun TasteIndiaNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Destination.Recipes) {

        composable<Destination.Recipes> {
            RecipesScreen(
                onMealClick = { mealId -> navController.navigate(Destination.Details(mealId)) },
                onOpenFavourites = { navController.navigate(Destination.Favourites) },
            )
        }

        composable<Destination.Favourites> {
            FavouritesScreen(
                onMealClick = { mealId -> navController.navigate(Destination.Details(mealId)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable<Destination.Details> { backStackEntry ->
            val route = backStackEntry.toRoute<Destination.Details>()
            DetailsScreen(
                mealId = route.mealId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
