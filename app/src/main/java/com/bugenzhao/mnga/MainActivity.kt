package com.bugenzhao.mnga

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.bugenzhao.mnga.ui.root.LumaGARoot
import com.bugenzhao.mnga.util.applyPredictiveBackEnabled

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Apply before first frame so the first back gesture matches the pref.
        applyPredictiveBackEnabled(App.prefs.predictiveBackEnabled.value)
        handleIntent(intent)
        setContent {
            val prefs = App.prefs
            val portrait by prefs.alwaysPortraitOnPhone.flow.collectAsState()
            val predictiveBack by prefs.predictiveBackEnabled.flow.collectAsState()
            LaunchedEffect(portrait) {
                requestedOrientation =
                    if (portrait) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            LaunchedEffect(predictiveBack) {
                applyPredictiveBackEnabled(predictiveBack)
            }
            LumaGARoot(onNewIntent = { handleIntent(it) })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (App.schemes.canNavigateTo(uri)) App.schemes.navigateTo(uri)
    }
}
