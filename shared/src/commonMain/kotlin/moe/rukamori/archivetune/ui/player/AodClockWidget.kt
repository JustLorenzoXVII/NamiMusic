/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.player

import moe.rukamori.archivetune.compat.BatteryState

import archivetunex.shared.generated.resources.*

import moe.rukamori.archivetune.compat.format

import moe.rukamori.archivetune.compat.Context
import moe.rukamori.archivetune.compat.Intent
import moe.rukamori.archivetune.compat.startActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import moe.rukamori.archivetune.compat.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.rukamori.archivetune.compat.ContextCompat
import moe.rukamori.archivetune.compat.SimpleDateFormat
import moe.rukamori.archivetune.compat.Date
import moe.rukamori.archivetune.compat.Locale
import moe.rukamori.archivetune.compat.uppercase
import moe.rukamori.archivetune.compat.lowercase
import kotlinx.coroutines.delay
import moe.rukamori.archivetune.constants.AodClockStyle

@Composable
fun AodClockWidget(
    showClock: Boolean,
    clockStyle: AodClockStyle,
    showBattery: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    if (!showClock && !showBattery) return

    val context = LocalContext.current
    var formattedTime by remember { mutableStateOf("") }
    var formattedDate by remember { mutableStateOf("") }
    var formattedHours by remember { mutableStateOf("") }
    var formattedMinutes by remember { mutableStateOf("") }
    var batteryLevel by remember { mutableIntStateOf(-1) }
    var isCharging by remember { mutableStateOf(false) }

    fun updateClock() {
        val now = Date()
        val is24Hour = moe.rukamori.archivetune.compat.DateFormat.is24HourFormat(context)
        val timePattern = when (clockStyle) {
            AodClockStyle.BOLD_DIGITAL -> if (is24Hour) "HH:mm" else "h:mm a"
            AodClockStyle.MINIMAL -> if (is24Hour) "HH:mm" else "h:mm a"
            AodClockStyle.ELEGANT_THIN -> if (is24Hour) "HH:mm" else "h:mm a"
            AodClockStyle.PIXEL_STACKED -> if (is24Hour) "HH:mm" else "h:mm"
        }
        formattedTime = SimpleDateFormat(timePattern, Locale.getDefault()).format(now)
        formattedDate = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now)
        formattedHours = SimpleDateFormat(if (is24Hour) "HH" else "hh", Locale.getDefault()).format(now)
        formattedMinutes = SimpleDateFormat("mm", Locale.getDefault()).format(now)
    }

    LaunchedEffect(clockStyle) {
        while (true) {
            updateClock()
            delay(1000L)
        }
    }

    LaunchedEffect(context, clockStyle) {
        // Nell'originale arrivava con le notifiche di sistema (ACTION_BATTERY_CHANGED).
        while (true) {
            val level = BatteryState.level()
            if (level >= 0) batteryLevel = level
            isCharging = BatteryState.isCharging()
            delay(30_000L)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.padding(vertical = 8.dp),
    ) {
        if (showClock) {
            if (clockStyle == AodClockStyle.PIXEL_STACKED) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = formattedHours,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 60.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = formattedMinutes,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 60.sp,
                        color = Color.White.copy(alpha = 0.80f),
                        textAlign = TextAlign.Center,
                    )
                }
            } else if (formattedTime.isNotBlank()) {
                val (fontSize, fontWeight, fontFamily) = when (clockStyle) {
                    AodClockStyle.BOLD_DIGITAL -> Triple(44.sp, FontWeight.Black, FontFamily.Monospace)
                    AodClockStyle.MINIMAL -> Triple(38.sp, FontWeight.Medium, FontFamily.Default)
                    AodClockStyle.ELEGANT_THIN -> Triple(48.sp, FontWeight.ExtraLight, FontFamily.SansSerif)
                    AodClockStyle.PIXEL_STACKED -> Triple(44.sp, FontWeight.Bold, FontFamily.Default)
                }

                Text(
                    text = formattedTime,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }

            if (formattedDate.isNotBlank()) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (showBattery && batteryLevel >= 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Icon(
                    imageVector = when {
                        isCharging -> Icons.Default.BatteryChargingFull
                        batteryLevel >= 90 -> Icons.Default.BatteryFull
                        else -> Icons.Default.BatteryStd
                    },
                    contentDescription = null,
                    tint = if (isCharging) accentColor else Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(Res.string.aod_battery_percent, batteryLevel),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
        }
    }
}
