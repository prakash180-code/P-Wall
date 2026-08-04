package com.prakash.pwall.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prakash.pwall.ui.clock.ClockRoute
import com.prakash.pwall.ui.customize.CustomizeRoute
import com.prakash.pwall.ui.effects.EffectsRoute
import com.prakash.pwall.ui.home.HomeRoute
import com.prakash.pwall.ui.preview.PreviewRoute
import com.prakash.pwall.ui.settings.SettingsRoute

/** Navigation destinations. */
object Destinations {
    const val HOME = "home"
    const val PREVIEW = "preview"
    const val CLOCK = "clock"
    const val EFFECTS = "effects"
    const val CUSTOMIZE = "customize"
    const val SETTINGS = "settings"
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
                onOpenClock = { navController.navigate(Destinations.CLOCK) },
                onOpenEffects = { navController.navigate(Destinations.EFFECTS) },
                onOpenCustomize = { navController.navigate(Destinations.CUSTOMIZE) },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) }
            )
        }
        composable(Destinations.PREVIEW) {
            PreviewRoute(
                onBack = { navController.popBackStack() },
                onOpenCustomize = { navController.navigate(Destinations.CUSTOMIZE) }
            )
        }
        composable(Destinations.CLOCK) {
            ClockRoute(onBack = { navController.popBackStack() })
        }
        composable(Destinations.EFFECTS) {
            EffectsRoute(onBack = { navController.popBackStack() })
        }
        composable(Destinations.CUSTOMIZE) {
            CustomizeRoute(onBack = { navController.popBackStack() })
        }
        composable(Destinations.SETTINGS) {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
    }
}
