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
import com.bugenzhao.mnga.ui.nav.Route
import com.bugenzhao.mnga.ui.root.LumaGARoot

open class MainActivity : ComponentActivity() {

    protected open fun initialRoute(): Route? = Route.ForumList

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val route = initialRoute() ?: run {
            finish()
            return
        }
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val prefs = App.prefs
            val portrait by prefs.alwaysPortraitOnPhone.flow.collectAsState()
            androidx.compose.runtime.LaunchedEffect(portrait) {
                requestedOrientation =
                    if (portrait) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            LumaGARoot(
                onNewIntent = { handleIntent(it) },
                initialRoute = route,
                navigationActivity = this,
            )
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
