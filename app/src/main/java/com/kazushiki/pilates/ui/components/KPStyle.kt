package com.kazushiki.pilates.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazushiki.pilates.ui.sfIcon
import com.kazushiki.pilates.ui.theme.KP
import kotlin.math.max

// Building blocks for the bold, card-based layout used on Home, Challenges and the
// quick workout builder: a big page header, mode cards, action tiles and choice chips.

// Text helpers ---------------------------------------------------------------------------------

/** iPhone-like line heights for the text sizes the app uses. */
private fun lineHeightFor(size: Int): TextUnit = when (size) {
    34 -> 41.sp
    30 -> 36.sp
    28 -> 34.sp
    22 -> 28.sp
    20 -> 25.sp
    17 -> 22.sp
    15 -> 20.sp
    13 -> 18.sp
    12 -> 16.sp
    11 -> 13.sp
    else -> (size * 1.2f).sp
}

/**
 * A text style matching an iPhone font: size in sp, weight, tracking (letter spacing in sp)
 * and optional tabular (monospaced) digits.
 */
internal fun kpFont(
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    tracking: Float = 0f,
    monospacedDigits: Boolean = false,
): TextStyle = TextStyle(
    fontSize = size.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
    lineHeight = lineHeightFor(size),
    fontFeatureSettings = if (monospacedDigits) "tnum" else null,
)

/** Size of an SF Symbol drawn next to text of this style. */
internal fun iconSizeFor(style: TextStyle): Dp =
    if (style.fontSize.isSp) (style.fontSize.value * 1.15f).dp else 18.dp

/** One line of text that shrinks (down to [minScale]) to fit, like `.minimumScaleFactor`. */
@Composable
internal fun KPFitText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    minScale: Float = 0.8f,
    textAlign: TextAlign? = null,
) {
    var scale by remember(text) { mutableFloatStateOf(1f) }
    var ready by remember(text) { mutableStateOf(false) }
    val scaled = style.copy(
        fontSize = if (style.fontSize.isSp) style.fontSize * scale else style.fontSize,
        letterSpacing = if (style.letterSpacing.isSp) style.letterSpacing * scale else style.letterSpacing,
    )
    Text(
        text = text,
        modifier = modifier.drawWithContent { if (ready) drawContent() },
        color = color,
        style = scaled,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = if (scale <= minScale + 0.001f) TextOverflow.Ellipsis else TextOverflow.Clip,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && scale > minScale + 0.001f) {
                scale = max(minScale, scale - 0.05f)
            } else {
                ready = true
            }
        },
    )
}

/** SwiftUI `Label`: an SF Symbol followed by text. */
@Composable
internal fun KPLabel(
    text: String,
    systemImage: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    iconColor: Color = color,
    spacing: Dp = 6.dp,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        Icon(
            imageVector = sfIcon(systemImage),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(iconSizeFor(style)),
        )
        Text(text = text, style = style, color = color)
    }
}

/** Surface fill with a 1dp border, rounded (the "card" look used by mode cards and tiles). */
@Composable
internal fun Modifier.kpOutlined(
    radius: Dp = KP.cardRadius,
    fill: Color = KP.colors.surface,
    stroke: Color = KP.colors.divider,
    strokeWidth: Dp = 1.dp,
): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .background(fill, shape)
        .border(strokeWidth, stroke, shape)
}

// Theme/KPTheme.swift ---------------------------------------------------------------------------

/** Small uppercase label used above section titles. */
@Composable
fun KPEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = kpFont(11, FontWeight.SemiBold, tracking = 1.5f),
        color = KP.colors.accent,
    )
}

// KPStyle.swift ---------------------------------------------------------------------------------

/** Accent eyebrow, a big heavy headline and an optional muted line under it. */
@Composable
fun KPPageHeader(eyebrow: String, title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    val c = KP.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { heading() },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = eyebrow.uppercase(),
            style = kpFont(12, FontWeight.ExtraBold, tracking = 2.5f),
            color = c.accent,
        )
        Text(
            text = title,
            style = kpFont(34, FontWeight.ExtraBold),
            color = c.text,
        )
        if (!subtitle.isNullOrEmpty()) {
            Text(
                text = subtitle,
                style = kpFont(17),
                color = c.mutedText,
            )
        }
    }
}

/** Uppercase heavy section title, e.g. "THIS WEEK". */
@Composable
fun KPSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier.semantics { heading() },
        style = kpFont(17, FontWeight.ExtraBold, tracking = 0.5f),
        color = KP.colors.text,
    )
}

/** An SF Symbol in a soft rounded square. */
@Composable
fun KPIconBadge(systemImage: String, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val c = KP.colors
    Box(
        modifier = modifier
            .size(size)
            .background(c.accentSoft, RoundedCornerShape(size * 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = sfIcon(systemImage),
            contentDescription = null,
            tint = c.accent,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** A full-width card for a way to train: icon, eyebrow, title, detail and a call to action. */
@Composable
fun KPModeCard(
    systemImage: String,
    eyebrow: String,
    title: String,
    detail: String,
    action: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = KP.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KP.cardRadius))
            .clickable(onClick = onClick)
            .kpOutlined()
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KPIconBadge(systemImage = systemImage, size = 58.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = eyebrow.uppercase(),
                style = kpFont(12, FontWeight.ExtraBold, tracking = 2f),
                color = c.accent,
            )
            Text(text = title, style = kpFont(20, FontWeight.ExtraBold), color = c.text)
            Text(text = detail, style = kpFont(15), color = c.mutedText)
            Text(
                text = action.uppercase(),
                modifier = Modifier.padding(top = 4.dp),
                style = kpFont(12, FontWeight.ExtraBold, tracking = 1.5f),
                color = c.mutedText,
            )
        }
        Icon(
            imageVector = sfIcon("chevron.right"),
            contentDescription = null,
            tint = c.mutedText,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** A square-ish tile for a shortcut: icon, uppercase title, subtitle and an arrow. */
@Composable
fun KPActionTile(
    systemImage: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = KP.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .clip(RoundedCornerShape(KP.cardRadius))
            .clickable(onClick = onClick)
            .kpOutlined()
            .padding(16.dp),
    ) {
        KPIconBadge(systemImage = systemImage, size = 46.dp)
        Spacer(Modifier.height(18.dp))
        Spacer(Modifier.weight(1f))
        KPFitText(
            text = title.uppercase(),
            style = kpFont(17, FontWeight.ExtraBold),
            color = c.text,
            minScale = 0.8f,
        )
        KPFitText(
            text = subtitle,
            style = kpFont(15),
            color = c.mutedText,
            minScale = 0.85f,
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = sfIcon("arrow.right"),
                contentDescription = null,
                tint = c.mutedText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** A small uppercase tag, e.g. "8 EXERCISES". */
@Composable
fun KPTag(text: String, modifier: Modifier = Modifier) {
    val c = KP.colors
    Text(
        text = text.uppercase(),
        modifier = modifier
            .background(c.background, RoundedCornerShape(percent = 50))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = kpFont(12, FontWeight.ExtraBold, tracking = 1.2f),
        color = c.text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** A selectable chip. Selected chips get the accent outline and a soft fill. */
@Composable
fun KPChip(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    systemImage: String? = null,
    isLocked: Boolean = false,
    onClick: () -> Unit,
) {
    val c = KP.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .clip(shape)
            .clickable(enabled = !isLocked, onClick = onClick)
            .background(if (isSelected) c.accentSoft else c.background, shape)
            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) c.accent else c.divider, shape)
            .padding(horizontal = 14.dp)
            .semantics { selected = isSelected },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (systemImage != null) {
            Icon(
                imageVector = sfIcon(systemImage),
                contentDescription = null,
                tint = if (isSelected) c.accent else c.mutedText,
                modifier = Modifier.size(17.dp),
            )
        }
        KPFitText(
            text = title,
            style = kpFont(15, FontWeight.SemiBold),
            color = if (isSelected) c.text else c.mutedText,
            modifier = Modifier.weight(1f, fill = false),
            minScale = 0.8f,
        )
        if (isLocked) {
            Icon(
                imageVector = sfIcon("lock.fill"),
                contentDescription = null,
                tint = c.mutedText,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/** A card with a small uppercase label on top, used for each builder setting. */
@Composable
fun KPSettingCard(
    title: String,
    modifier: Modifier = Modifier,
    footnote: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = KP.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .kpOutlined()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = title.uppercase(),
            modifier = Modifier.semantics { heading() },
            style = kpFont(12, FontWeight.ExtraBold, tracking = 2f),
            color = c.mutedText,
        )
        content()
        if (footnote != null) {
            Text(text = footnote, style = kpFont(13), color = c.mutedText)
        }
    }
}

/** Accessibility helper: a plain content description. */
internal fun Modifier.kpDescription(text: String): Modifier = semantics { contentDescription = text }
