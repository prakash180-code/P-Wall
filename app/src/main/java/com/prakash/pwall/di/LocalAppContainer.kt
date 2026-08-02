package com.prakash.pwall.di

import androidx.compose.runtime.staticCompositionLocalOf

/** Composition-local handle to the app's dependency container. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
