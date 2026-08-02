package com.prakash.pwall.service.render

/**
 * Tracks installed [Module]s and installs them into the engine. Duplicate
 * module ids are rejected.
 */
class ModuleSystem {

    private val modules = mutableListOf<Module>()

    fun add(module: Module): Boolean {
        if (modules.any { it.id == module.id }) return false
        return modules.add(module)
    }

    val ids: List<String> get() = modules.map { it.id }

    val size: Int get() = modules.size

    fun installAll(engine: WallpaperRenderEngine) {
        for (module in modules) {
            module.register(engine)
        }
    }
}
