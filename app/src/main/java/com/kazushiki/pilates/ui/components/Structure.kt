package com.kazushiki.pilates.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kazushiki.pilates.ui.sfIcon
import com.kazushiki.pilates.ui.theme.KP

/** Red used for destructive actions (iPhone's `role: .destructive`). */
internal val KPDestructive = Color(0xFFD32F2F)

/**
 * Top padding for the status bar on screens drawn edge to edge. Kept in one place so it can be
 * switched off if the hosting Scaffold already pads for the status bar.
 */
internal fun Modifier.kpStatusBarPadding(): Modifier = this.statusBarsPadding()

/** iPhone's inline navigation bar: a "Back" button with a chevron and a centered title. */
@Composable
fun KPTopBar(
    title: String = "",
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = KP.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(c.background)
            .kpStatusBarPadding()
            .height(44.dp),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBack)
                .padding(start = 2.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // chevron.left: the mapped chevron.right turned around.
            Icon(
                imageVector = sfIcon("chevron.right"),
                contentDescription = null,
                tint = c.accent,
                modifier = Modifier.size(30.dp).rotate(180f),
            )
            Text(text = "Back", style = kpFont(17), color = c.accent)
        }
        if (title.isNotEmpty()) {
            Text(
                text = title,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 88.dp),
                style = kpFont(17, FontWeight.SemiBold),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/** iPhone's `.fullScreenCover`: a full-screen window over everything (player, paywall). */
@Composable
fun FullScreenDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(KP.colors.background)
                .safeDrawingPadding(),
        ) {
            content()
        }
    }
}

/** iPhone's `.sheet`: a full-height modal bottom sheet on the app background. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KPBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = KP.colors.background,
        contentColor = KP.colors.text,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            content = content,
        )
    }
}

/** `.borderedProminent` + `.controlSize(.large)`: a full-width accent button. */
@Composable
fun KPPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    systemImage: String? = null,
    modifier: Modifier = Modifier,
) {
    KPFilledButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        systemImage = systemImage,
        container = KP.colors.accent,
        content = KP.colors.onAccent,
    )
}

/** `.bordered` + `.controlSize(.large)`: a full-width soft accent button. */
@Composable
fun KPSecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    systemImage: String? = null,
    modifier: Modifier = Modifier,
) {
    KPFilledButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        systemImage = systemImage,
        container = KP.colors.accentSoft,
        content = KP.colors.accent,
    )
}

/**
 * The shared large button. [fontWeight], [letterSpacing] and [extraVerticalPadding] cover the
 * heavier "START WORKOUT" style buttons (label `.font(.headline.weight(.heavy)).padding(.vertical, 8)`).
 */
@Composable
internal fun KPFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    systemImage: String? = null,
    container: Color = KP.colors.accent,
    content: Color = KP.colors.onAccent,
    fontWeight: FontWeight = FontWeight.SemiBold,
    letterSpacing: Float = 0f,
    extraVerticalPadding: Dp = 0.dp,
) {
    val c = KP.colors
    val style = kpFont(17, fontWeight, tracking = letterSpacing)
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp + extraVerticalPadding * 2),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = c.divider,
            disabledContentColor = c.mutedText,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp + extraVerticalPadding),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (systemImage != null) {
                Icon(
                    imageVector = sfIcon(systemImage),
                    contentDescription = null,
                    modifier = Modifier.size(iconSizeFor(style)),
                )
            }
            Text(text = text, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
