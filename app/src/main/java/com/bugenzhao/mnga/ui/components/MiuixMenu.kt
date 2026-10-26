package com.bugenzhao.mnga.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.bugenzhao.mnga.ui.root.GlassVariantScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperListPopup
import top.yukonga.miuix.kmp.extra.WindowListPopup
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * MIUIX-styled dropdown menu: the phase-3 replacement for Material3
 * `DropdownMenu` / `DropdownMenuItem`, which phases 1–2 had to leave in
 * place across the app (topic list / topic details overflow menus, row
 * context menus, the favorites folder menu, the subforum favorite menu…).
 *
 * The container is a genuine Miuix popup — [SuperListPopup] (or
 * [WindowListPopup] when the anchor lives inside a platform Dialog, which
 * has no Miuix Scaffold popup host) with a [ListPopupColumn], so the
 * panel itself (surfaceContainer background, 16 dp corners, entrance
 * animation, window dimming, predictive-back aware dismissal) is the
 * library's own [top.yukonga.miuix.kmp.basic.ListPopupContent].
 *
 * The items are hand-rolled, because Miuix 0.8.8's only popup item —
 * `basic.DropdownImpl`, the row [top.yukonga.miuix.kmp.extra.SuperDropdown]
 * uses internally — renders a single line of text plus a selection check
 * and nothing else: it cannot express leading icons, two-line entries,
 * info headers, grouped sections or trailing values, all of which this
 * app's menus need. [MiuixMenuItem] therefore follows the Miuix menu spec
 * directly (the same metrics `DropdownImpl` uses: 20 dp horizontal
 * padding, body1 medium text, primary check for the selected entry), so
 * the result is visually indistinguishable from a native Miuix menu.
 *
 * Submenus: Miuix has no cascading menu, so a submenu replaces the popup
 * content in place — the caller keeps its hoisted submenu state and
 * renders either the main items or the submenu items inside one
 * [MiuixDropdownMenu], staging [MiuixDropdownMenu]'s dismiss callback so
 * an outside tap or back press first closes the submenu, exactly like
 * dismissing the topmost Material popup did.
 *
 * Like Material's `DropdownMenu`, the popup anchors to the bounds of the
 * layout it is composed in — wrap the anchor widget and the menu in a
 * `Box`, as the call sites already do.
 */
/**
 * Widest a dropdown menu panel may grow. Miuix's ListPopup measures its
 * content (the panel is content-sized, never a fixed near-full-screen
 * width), but an item carrying a long title would otherwise stretch the
 * panel to the popup's window constraint; every menu in the app funnels
 * through [MiuixDropdownMenu], so the cap lives here.
 */
val MiuixMenuMaxWidth = 280.dp

@Composable
fun MiuixDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.End,
    windowPopup: Boolean = false,
    content: @Composable () -> Unit,
) {
    val popupContent: @Composable () -> Unit = {
        ListPopupColumn {
            Box(Modifier.widthIn(max = MiuixMenuMaxWidth)) {
                // Liquid glass panel (v1.7.3): SuperListPopup renders in
                // the main window (root Scaffold popup host), so the
                // panel can sample the real content beneath it. The
                // library paints its opaque surfaceContainer UNDER the
                // ListPopupColumn content (ListPopupContent draws its
                // background before the content), so this full-size
                // glass layer — the first content child — covers the
                // panel exactly, clipped by the library's own 16 dp
                // shape clip and revealed by its entrance animation.
                // WindowListPopup menus live in a platform Dialog
                // window instead: cross-window sampling is impossible,
                // so they deliberately keep the library panel.
                if (!windowPopup) {
                    GlassPanelBackground(
                        shape = RoundedCornerShape(16.dp),
                        lensScale = GlassVariantScale.MEDIUM,
                        debugColor = Color(0xFFFF9800),
                    )
                }
                // 8 dp of panel padding top/bottom: with the items' own
                // 12 dp vertical padding this reproduces DropdownImpl's
                // 20 dp end padding rhythm for plain lists.
                Column(Modifier.padding(vertical = 8.dp)) {
                    content()
                }
            }
        }
    }
    if (windowPopup) {
        WindowListPopup(
            show = expanded,
            alignment = alignment,
            onDismissRequest = onDismissRequest,
            content = popupContent,
        )
    } else {
        SuperListPopup(
            show = expanded,
            alignment = alignment,
            onDismissRequest = onDismissRequest,
            content = popupContent,
        )
    }
}

/**
 * One tappable row of a [MiuixDropdownMenu]: optional leading [icon]
 * (or a custom [leadingContent] slot, e.g. a color swatch), one or two
 * lines of text ([text] plus an optional [summary] line), an optional
 * [trailingText] (e.g. the current sort order) and an optional primary
 * check mark ([showCheck]) for the selected entry. [contentColor]
 * overrides the text/icon color (e.g. error red for a destructive
 * entry); disabled rows are dimmed and not clickable.
 */
@Composable
fun MiuixMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    summary: String? = null,
    trailingText: String? = null,
    showCheck: Boolean = false,
    enabled: Boolean = true,
    contentColor: Color = Color.Unspecified,
) {
    val colorScheme = MiuixTheme.colorScheme
    val mainColor = if (contentColor == Color.Unspecified) {
        colorScheme.onSurface
    } else {
        contentColor
    }.copy(alpha = if (enabled) 1f else 0.4f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .defaultMinSize(minHeight = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = mainColor,
            )
            Spacer(Modifier.width(12.dp))
        } else if (leadingContent != null) {
            leadingContent()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = text,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
                color = mainColor,
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MiuixTheme.textStyles.footnote2,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MiuixTheme.textStyles.footnote1,
                color = colorScheme.onSurfaceVariantSummary,
            )
        }
        if (showCheck) {
            Icon(
                imageVector = MiuixIcons.Basic.Check,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(20.dp),
                tint = colorScheme.primary,
            )
        }
    }
}

/**
 * Non-clickable info header at the top of a [MiuixDropdownMenu]
 * (e.g. `#524 漩涡书院`), with an optional [summary] second line.
 */
@Composable
fun MiuixMenuHeader(
    text: String,
    summary: String? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        if (summary != null) {
            Text(
                text = summary,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/** Group separator inside a [MiuixDropdownMenu]. */
@Composable
fun MiuixMenuDivider() {
    HorizontalDivider(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    )
}
