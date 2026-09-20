package com.bugenzhao.mnga.util

import android.app.Activity
import android.os.Build
import android.util.Log

/**
 * Runtime toggle for Android Predictive Back ([Activity] hidden API
 * `setEnableOnBackInvokedCallback`, API 33+).
 *
 * The manifest must keep `android:enableOnBackInvokedCallback="true"` so the
 * feature can be turned on; this helper then matches the user preference.
 * When reflection is unavailable, the activity stays at the manifest default.
 */
fun Activity.applyPredictiveBackEnabled(enabled: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    try {
        val method = Activity::class.java.getDeclaredMethod(
            "setEnableOnBackInvokedCallback",
            Boolean::class.javaPrimitiveType,
        )
        method.isAccessible = true
        method.invoke(this, enabled)
    } catch (t: Throwable) {
        Log.w("PredictiveBack", "runtime toggle unavailable: ${t.message}")
    }
}
