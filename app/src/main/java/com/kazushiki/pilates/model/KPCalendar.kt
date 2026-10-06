package com.kazushiki.pilates.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * The bits of the Swift `Calendar` the app relies on. Weekdays use Apple's numbering:
 * 1 = Sunday … 7 = Saturday. Injectable (zone, first weekday) so tests are stable.
 */
class KPCalendar(
    val zone: ZoneId = ZoneId.systemDefault(),
    /** 1 = Sunday … 7 = Saturday. */
    val firstWeekday: Int = weekdayNumber(WeekFields.of(Locale.getDefault()).firstDayOfWeek),
    val locale: Locale = Locale.getDefault(),
) {
    private fun zoned(instant: Instant): ZonedDateTime = instant.atZone(zone)

    fun startOfDay(instant: Instant): Instant = zoned(instant).truncatedTo(ChronoUnit.DAYS).toInstant()

    /** Start of the week containing [instant], using [firstWeekday]. */
    fun startOfWeek(instant: Instant): Instant {
        val first = dayOfWeek(firstWeekday)
        return zoned(instant).truncatedTo(ChronoUnit.DAYS)
            .with(TemporalAdjusters.previousOrSame(first))
            .toInstant()
    }

    fun addDays(instant: Instant, days: Long): Instant = zoned(instant).plusDays(days).toInstant()
    fun addWeeks(instant: Instant, weeks: Long): Instant = zoned(instant).plusWeeks(weeks).toInstant()

    /** 1 = Sunday … 7 = Saturday. */
    fun weekday(instant: Instant): Int = weekdayNumber(zoned(instant).dayOfWeek)
    fun hour(instant: Instant): Int = zoned(instant).hour
    fun dayOfMonth(instant: Instant): Int = zoned(instant).dayOfMonth
    fun isSameDay(a: Instant, b: Instant): Boolean = startOfDay(a) == startOfDay(b)

    fun atTime(instant: Instant, hour: Int, minute: Int): Instant =
        zoned(instant).withHour(hour).withMinute(minute).withSecond(0).withNano(0).toInstant()

    /** "Mon" */
    fun shortWeekdaySymbol(weekday: Int): String = dayOfWeek(weekday).getDisplayName(TextStyle.SHORT_STANDALONE, locale)
    /** "M" */
    fun veryShortWeekdaySymbol(weekday: Int): String = dayOfWeek(weekday).getDisplayName(TextStyle.NARROW_STANDALONE, locale)
    /** "Monday" */
    fun weekdayName(weekday: Int): String = dayOfWeek(weekday).getDisplayName(TextStyle.FULL_STANDALONE, locale)

    fun date(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Instant =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant()

    companion object {
        val current: KPCalendar get() = KPCalendar()

        fun weekdayNumber(day: DayOfWeek): Int = day.value % 7 + 1
        fun dayOfWeek(weekday: Int): DayOfWeek = DayOfWeek.of((weekday + 5) % 7 + 1)
    }
}
