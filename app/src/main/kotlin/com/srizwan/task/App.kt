package com.srizwan.task

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class App : Application() {
    companion object {
        lateinit var instance: Application
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        CrashHandler.install(this)
        AppFonts.start(this)
    }

    object CrashHandler {
        fun install(app: Application) {
            val def = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { t, e ->
                try {
                    val trace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString()
                    val intent = Intent(app, DebugActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("trace", trace)
                    }
                    app.startActivity(intent)
                    Process.killProcess(Process.myPid())
                    exitProcess(1)
                } catch (ignored: Exception) {
                    def?.uncaughtException(t, e)
                }
            }
        }
    }

    private fun processName(): String = try {
        if (Build.VERSION.SDK_INT >= 28) {
            android.app.Application.getProcessName()
        } else {
            @Suppress("DEPRECATION")
            android.app.ActivityManager::class.java
                .getDeclaredMethod("getCurrentProcessName")
                .invoke(null) as? String ?: ""
        }
    } catch (e: Exception) {
        ""
    }
}
