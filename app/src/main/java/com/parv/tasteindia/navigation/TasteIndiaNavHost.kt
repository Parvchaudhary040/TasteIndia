package com.parv.tasteindia.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.parv.tasteindia.presentation.details.DetailsScreen
import com.parv.tasteindia.presentation.favourites.FavouritesScreen
import com.parv.tasteindia.presentation.recipes.RecipesScreen
import com.parv.tasteindia.presentation.splash.SplashScreen
import com.parv.tasteindia.presentation.welcome.WelcomeScreen

/**
 * Single-Activity navigation graph.
 *
 * Launch flow: Splash (branded name, ~3.6s, fades out) -> Welcome (landing page) -> Recipes.
 * Splash and Welcome are each popped from the back stack once left, so Recipes is the effective
 * home and its back-stack entry (ViewModel + list state) survives Recipes -> Details -> Back.
 */
@Composable
fun TasteIndiaNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Destination.Splash) {

        composable<Destination.Splash>(
            exitTransition = { fadeOut(tween(600)) },
        ) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Destination.Welcome) {
                        popUpTo<Destination.Splash> { inclusive = true }
                    }
                },
            )
        }

        composable<Destination.Welcome>(
            enterTransition = { fadeIn(tween(500)) },
            exitTransition = { fadeOut(tween(300)) },
        ) {
            WelcomeScreen(
                onExplore = {
                    navController.navigate(Destination.Recipes) {
                        popUpTo<Destination.Welcome> { inclusive = true }
                    }
                },
            )
        }

        composable<Destination.Recipes>(
            enterTransition = { fadeIn(tween(400)) },
        ) {
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
