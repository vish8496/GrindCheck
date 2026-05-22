package com.grindcheck.app

import android.app.Application

class GrindCheckApp : Application() {
    companion object {
        lateinit var instance: GrindCheckApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
