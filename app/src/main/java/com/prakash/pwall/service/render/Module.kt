package com.prakash.pwall.service.render

/**
 * A self-contained bundle of [Layer]s and [Effect]s. The core module ships the
 * stock wallpaper layers; future premium features arrive as additional modules.
 */
interface Module {
    val id: String
    fun register(engine: WallpaperRenderEngine)
}
