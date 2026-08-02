package com.prakash.pwall.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prakash.pwall.ui.customize.CustomizeRoute
import com.prakash.pwall.ui.home.HomeRoute
import com.prakash.pwall.ui.preview.PreviewRoute

/** Navigation destinations. */
object Destinations {
    const val HOME = "home"
    const val PREVIEW = "preview"
    const val CUSTOMIZE = "customize"
}

@Composable
fun PWallNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME
    ) {
        composable(Destinations.HOME) {
            HomeRoute(
                onOpenPreview = { navController.navigate(Destinations.PREVIEW) },
                onOpenCustomize = { navController.navigate(Destinations.CUSTOMIZE) }
            )
        }
        composable(Destinations.PREVIEW) {
            PreviewRoute(
                onBack = { navController.popBackStack() },
                onOpenCustomize = { navController.navigate(Destinations.CUSTOMIZE) }
            )
        }
        composable(Destinations.CUSTOMIZE) {
            CustomizeRoute(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
