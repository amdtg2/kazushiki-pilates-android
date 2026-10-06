package com.kazushiki.pilates.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Rowing
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps the iPhone app's SF Symbol names (kept as plain strings in the shared models) to the
 * closest Material icon, so models stay identical between platforms.
 */
fun sfIcon(name: String): ImageVector = when (name) {
    // Tabs and navigation
    "house.fill" -> Icons.Filled.Home
    "flame.fill" -> Icons.Filled.Whatshot
    "flame" -> Icons.Outlined.Whatshot
    "chart.bar.fill" -> Icons.Filled.BarChart
    "person.crop.circle" -> Icons.Filled.Person
    "chevron.right" -> Icons.Filled.ChevronRight
    "chevron.left" -> Icons.AutoMirrored.Filled.ArrowBackIos
    "chevron.down" -> Icons.Filled.KeyboardArrowDown
    "xmark" -> Icons.Filled.Close
    "trash" -> Icons.Filled.Delete
    "pause.fill" -> Icons.Filled.Pause
    "play.circle.fill" -> Icons.Filled.PlayCircle
    "arrow.counterclockwise" -> Icons.Filled.Replay
    "forward.end.fill" -> Icons.Filled.SkipNext
    "speaker.wave.2.fill" -> Icons.AutoMirrored.Filled.VolumeUp
    "speaker.slash.fill" -> Icons.AutoMirrored.Filled.VolumeOff
    "arrow.right" -> Icons.AutoMirrored.Filled.ArrowForward
    "ellipsis", "ellipsis.circle" -> Icons.Filled.MoreHoriz
    "list.bullet" -> Icons.AutoMirrored.Filled.List
    "rectangle.portrait.and.arrow.right" -> Icons.AutoMirrored.Filled.ExitToApp
    "plus" -> Icons.Filled.Add
    "repeat" -> Icons.Filled.Repeat

    // Actions and states
    "play.fill" -> Icons.Filled.PlayArrow
    "lock.fill" -> Icons.Filled.Lock
    "checkmark" -> Icons.Filled.Check
    "checkmark.circle.fill" -> Icons.Filled.CheckCircle
    "checkmark.seal.fill" -> Icons.Filled.Verified
    "sparkles" -> Icons.Filled.AutoAwesome
    "bolt.fill" -> Icons.Filled.Bolt
    "bolt.heart.fill" -> Icons.Filled.Bolt
    "calendar", "calendar.badge.plus" -> Icons.Filled.CalendarMonth
    "calendar.badge.checkmark" -> Icons.Filled.EventAvailable
    "star.fill" -> Icons.Filled.Star
    "medal.fill" -> Icons.Filled.MilitaryTech
    "trophy.fill" -> Icons.Filled.EmojiEvents
    "crown.fill" -> Icons.Filled.WorkspacePremium
    "graduationcap.fill" -> Icons.Filled.School
    "target" -> Icons.Filled.TrackChanges
    "clock.fill" -> Icons.Filled.AccessAlarm
    "stopwatch.fill" -> Icons.Filled.Timer
    "hourglass" -> Icons.Filled.HourglassBottom
    "sunrise.fill" -> Icons.Filled.WbTwilight
    "moon.stars.fill" -> Icons.Filled.Bedtime
    "map.fill" -> Icons.Filled.Map
    "hand.raised.fill" -> Icons.Filled.Flag
    "10.circle.fill" -> Icons.Filled.Star
    "sun.max.fill" -> Icons.Filled.WbSunny

    // Figures and equipment
    "figure.pilates", "figure.core.training", "figure.flexibility" -> Icons.Filled.SportsGymnastics
    "figure.strengthtraining.traditional", "figure.strengthtraining.functional", "dumbbell.fill" -> Icons.Filled.FitnessCenter
    "figure.step.training", "figure.run" -> Icons.AutoMirrored.Filled.DirectionsRun
    "figure.cooldown", "figure.stand" -> Icons.Filled.AccessibilityNew
    "figure.yoga" -> Icons.Filled.SelfImprovement
    "figure.rower" -> Icons.Filled.Rowing
    "leaf" -> Icons.Filled.Spa
    "bed.double.fill" -> Icons.Filled.Bed
    "circle" -> Icons.Outlined.Circle
    "circle.fill" -> Icons.Filled.Circle
    "rectangle.portrait.fill" -> Icons.Filled.CalendarToday
    "line.diagonal" -> Icons.Filled.Repeat
    "square.split.bottomrightquarter" -> Icons.Filled.CalendarToday
    else -> Icons.Filled.Circle
}
