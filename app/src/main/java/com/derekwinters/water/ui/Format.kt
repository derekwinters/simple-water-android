package com.derekwinters.water.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

/** UI-009: a time of day in the device's 12/24-hour format. */
fun formatTime(context: Context, time: LocalTime): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

fun formatTime(context: Context, millis: Long, zone: ZoneId): String =
    formatTime(context, Instant.ofEpochMilli(millis).atZone(zone).toLocalTime())

fun formatMinuteOfDay(context: Context, minute: Int): String =
    formatTime(context, LocalTime.of(minute / 60, minute % 60))

/** "Sun 11 Oct". */
fun formatDay(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))

/**
 * LOG-002: today's local date, re-read every 30 seconds so an open screen rolls over at local
 * midnight (and follows a time-zone change).
 */
@Composable
fun rememberToday(): State<LocalDate> = produceState(initialValue = LocalDate.now()) {
    while (true) {
        delay(30_000)
        val now = LocalDate.now()
        if (now != value) value = now
    }
}

/** UI-013: the local time of day, re-read every 30 seconds so the home pace line keeps up. */
@Composable
fun rememberNow(): State<LocalTime> = produceState(initialValue = LocalTime.now()) {
    while (true) {
        delay(30_000)
        value = LocalTime.now()
    }
}
