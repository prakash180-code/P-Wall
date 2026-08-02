package com.prakash.pwall

import android.app.Application
import com.prakash.pwall.di.AppContainer

class PWallApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
