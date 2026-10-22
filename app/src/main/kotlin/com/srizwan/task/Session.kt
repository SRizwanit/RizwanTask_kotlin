package com.srizwan.task

import android.content.Context

object Session {
    private const val PREFS = "kp_session"
    private const val KEY = "kp_session_v1"

    fun exists(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) != null
}
