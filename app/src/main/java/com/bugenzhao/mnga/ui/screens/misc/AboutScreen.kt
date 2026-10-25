package com.bugenzhao.mnga.ui.screens.misc

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.App
import com.bugenzhao.mnga.BuildConfig
import com.bugenzhao.mnga.R
import com.bugenzhao.mnga.ui.components.GroupedList
import com.bugenzhao.mnga.ui.components.GroupedRow
import com.bugenzhao.mnga.ui.components.RowChevron
import com.bugenzhao.mnga.ui.nav.Navigator
import com.bugenzhao.mnga.util.Constants
import com.bugenzhao.mnga.util.L
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * About page: app icon, version, and the update check. Links point at the
 * GitHub repository that releases are published to (see [Constants.GitHub]).
 */
@Composable
fun AboutScreen(navigator: Navigator? = null) {
    val context = LocalContext.current

    fun open(url: String) {
        App.openURL.open(Uri.parse(url), inApp = false, prefs = App.prefs)
    }


    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .exclude(WindowInsets.navigationBars),
        topBar = {
            SmallTopAppBar(
                title = L.str(context, "About"),
                navigationIcon = {
                    IconButton(onClick = { navigator?.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item(key = "header") {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Image(
                        // 用独立 PNG（adaptive-icon XML 不支持 painterResource）。
                        painter = painterResource(R.drawable.ic_launcher_about),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp).clip(RoundedCornerShape(22.dp)),
                    )
                    Text(
                        // Brand name, intentionally not localized.
                        "LumaGA",
                        style = MiuixTheme.textStyles.title2.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Text(
                        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            item(key = "update") {
                GroupedList { Column { CheckForUpdatesRow() } }
            }

            item(key = "links") {
                GroupedList {
                    Column {
                        GroupedRow(
                            onClick = { open(Constants.GitHub.repoUrl) },
                            title = L.str(context, "Source Code"),
                            subtitle = Constants.GitHub.repo,
                            trailing = { RowChevron() },
                        )
                        GroupedRow(
                            onClick = { open(Constants.GitHub.releasesUrl) },
                            title = L.str(context, "Release Notes"),
                            trailing = { RowChevron() },
                        )
                    }
                }
            }

            item(key = "footer") {
                Text(
                    L.str(context, "An NGA forum client, ported from MNGA."),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
        }
    }

    UpdateFlowDialogs()
}
