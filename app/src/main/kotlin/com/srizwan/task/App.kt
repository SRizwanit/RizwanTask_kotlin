package com.srizwan.task

import android.app.Application
import android.content.Intent
import java.io.File
import kotlin.system.exitProcess

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        AppFonts.start(this)
    }
}

object CrashHandler {
    private const val MAX_CHARS = 100_000

    fun install(app: Application) {
        if (processName().endsWith(":debug")) return
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val trace = buildString {
                    append("থ্রেড: ").append(thread.name).append('\n')
                    append(android.util.Log.getStackTraceString(error))
                }.take(MAX_CHARS)
                val intent = Intent(app, DebugActivity::class.java).apply {
                    putExtra(DebugActivity.EXTRA_TRACE, trace)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                app.startActivity(intent)
            } catch (_: Throwable) {
                previous?.uncaughtException(thread, error)
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }

    private fun processName(): String = try {
        File("/proc/self/cmdline").readText().trim { it == '\u0000' || it.isWhitespace() }
    } catch (_: Throwable) {
        ""
    }
}
