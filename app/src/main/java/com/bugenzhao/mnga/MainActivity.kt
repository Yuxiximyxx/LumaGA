package com.bugenzhao.mnga

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.bugenzhao.mnga.model.SchemesModel
import com.bugenzhao.mnga.storage.PreferencesStorage
import com.bugenzhao.mnga.ui.root.LumaGARoot

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Full status-bar immersion, enforced at the activity level
        // (v1.8.3): the bars stay transparent and the content draws
        // edge-to-edge under them on every screen. enableEdgeToEdge
        // already implies this, but the XML theme's static light-bar
        // flags and any platform default could re-tint the bar area
        // before Compose runs, so assert it explicitly here and again
        // from the theme whenever the color scheme changes (Theme.kt).
        // Screens keep their own inset handling for top chrome — this
        // only guarantees no opaque band is painted over the status
        // area by the window itself.
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        handleIntent(intent)
        setContent {
            val prefs = App.prefs
            val portrait by prefs.alwaysPortraitOnPhone.flow.collectAsState()
            androidx.compose.runtime.LaunchedEffect(portrait) {
                requestedOrientation =
                    if (portrait) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
