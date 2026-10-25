package com.bugenzhao.mnga.ui.nav.sharednav

import androidx.compose.runtime.Composable
import com.xah.navigation.component.SharedNavHost
import com.xah.navigation.component.rememberNavController
import com.xah.navigation.model.dest.Destination

/** Phase-4 spike: minimal SharedNav host with a single empty destination. */
private object SpikeDestination : Destination() {
    override val key = "spike"

    @Composable
    override fun Content() = Unit
}

@Composable
internal fun SharedNavSpike() {
    val controller = rememberNavController(startDestination = SpikeDestination)
    SharedNavHost(navController = controller)
}
