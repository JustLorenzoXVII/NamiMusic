@file:OptIn(kotlin.time.ExperimentalTime::class)

package moe.rukamori.archivetune.compat.time

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant as KInstant
import kotlinx.datetime.LocalDate as KLocalDate
import kotlinx.datetime.LocalDateTime as KLocalDateTime
import kotlinx.datetime.LocalTime as KLocalTime

/*
 * Sottoinsieme di java.time usato da ArchiveTune, riscritto sopra kotlinx-datetime.
 * Stessi nomi e stessi metodi, così il codice originale (database, statistiche, filtri)
 * funziona anche su iPhone cambiando solo gli import.
 */

open class ZoneId internal constructor(
    internal val timeZone: TimeZone,
) {
    val id: String get() = timeZone.id

    override fun equals(other: Any?): Boolean = other is ZoneId && other.timeZone == timeZone

    override fun hashCode(): Int = timeZone.hashCode()

    override fun toString(): String = id

    companion object {
        fun systemDefault(): ZoneId = ZoneId(TimeZone.currentSystemDefault())

        fun of(zoneId: String): ZoneId = if (zoneId == "UTC" || zoneId == "Z") ZoneOffset.UTC else ZoneId(TimeZone.of(zoneId))
    }
}

class ZoneOffset private constructor(
    timeZone: TimeZone,
) : ZoneId(timeZone) {
    companion object {
        val UTC: ZoneOffset = ZoneOffset(TimeZone.UTC)
    }
}

class Instant internal constructor(
    internal val value: KInstant,
) : Comparable<Instant> {
    fun toEpochMilli(): Long = value.toEpochMilliseconds()

    val epochSecond: Long get() = value.epochSeconds


    fun plusMillis(millis: Long): Instant = Instant(KInstant.fromEpochMilliseconds(toEpochMilli() + millis))

    fun minusMillis(millis: Long): Instant = plusMillis(-millis)

    fun plusSeconds(seconds: Long): Instant = plusMillis(seconds * 1000)

    fun minusSeconds(seconds: Long): Instant = plusMillis(-seconds * 1000)

    fun isBefore(other: Instant): Boolean = value < other.value

    fun isAfter(other: Instant): Boolean = value > other.value

    fun atZone(zone: ZoneId): ZonedDateTime = ZonedDateTime(LocalDateTime(value.toLocalDateTime(zone.timeZone)), zone)

    override fun compareTo(other: Instant): Int = value.compareTo(other.value)

    override fun equals(other: Any?): Boolean = other is Instant && other.value == value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    companion object {
        val EPOCH: Instant = Instant(KInstant.fromEpochMilliseconds(0))

        fun now(): Instant = Instant(Clock.System.now())

        fun ofEpochMilli(epochMilli: Long): Instant = Instant(KInstant.fromEpochMilliseconds(epochMilli))

        fun ofEpochSecond(epochSecond: Long): Instant = Instant(KInstant.fromEpochSeconds(epochSecond))

        fun parse(text: CharSequence): Instant = Instant(KInstant.parse(text.toString()))
    }
}

class ZonedDateTime internal constructor(
    private val dateTime: LocalDateTime,
    val zone: ZoneId,
) {
    fun toLocalDateTime(): LocalDateTime = dateTime

    fun toLocalDate(): LocalDate = dateTime.toLocalDate()

    fun toInstant(): Instant = dateTime.toInstant(zone)

    fun toEpochSecond(): Long = toInstant().epochSecond

    val year: Int get() = dateTime.year
    val monthValue: Int get() = dateTime.monthValue
    val dayOfMonth: Int get() = dateTime.dayOfMonth
    val hour: Int get() = dateTime.hour
    val minute: Int get() = dateTime.minute

    fun format(formatter: DateTimeFormatter): String = formatter.format(dateTime)
}

enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY,
    ;

    val value: Int get() = ordinal + 1

}

enum class Month {
    JANUARY,
    FEBRUARY,
    MARCH,
    APRIL,
    MAY,
    JUNE,
    JULY,
    AUGUST,
    SEPTEMBER,
    OCTOBER,
    NOVEMBER,
    DECEMBER,
    ;

    val value: Int get() = ordinal + 1


    companion object {
        fun of(month: Int): Month = entries[month - 1]
    }
}

class LocalDate internal constructor(
    internal val value: KLocalDate,
) : Comparable<LocalDate> {
    val year: Int get() = value.year
    val monthValue: Int get() = value.month.ordinal + 1
    val month: Month get() = Month.of(monthValue)
    val dayOfMonth: Int get() = value.day
    val dayOfYear: Int get() = value.dayOfYear
    val dayOfWeek: DayOfWeek get() = DayOfWeek.entries[value.dayOfWeek.ordinal]

    /** LocalDate.with(DayOfWeek.X): lo stesso giorno della settimana ISO corrente (da lunedì a domenica). */
    fun with(day: DayOfWeek): LocalDate = plusDays((day.ordinal - dayOfWeek.ordinal).toLong())




    fun plusDays(days: Long): LocalDate = LocalDate(value.plus(days, DateTimeUnit.DAY))

    fun minusDays(days: Long): LocalDate = plusDays(-days)

    fun plusWeeks(weeks: Long): LocalDate = plusDays(weeks * 7)

    fun minusWeeks(weeks: Long): LocalDate = plusDays(-weeks * 7)

    fun plusMonths(months: Long): LocalDate = LocalDate(value.plus(months, DateTimeUnit.MONTH))

    fun minusMonths(months: Long): LocalDate = plusMonths(-months)

    fun plusYears(years: Long): LocalDate = LocalDate(value.plus(years, DateTimeUnit.YEAR))

    fun minusYears(years: Long): LocalDate = plusYears(-years)

    fun withDayOfMonth(day: Int): LocalDate = LocalDate(KLocalDate(value.year, value.month, day))

    fun withDayOfYear(day: Int): LocalDate = LocalDate(KLocalDate(value.year, 1, 1).plus(day - 1, DateTimeUnit.DAY))

    fun atStartOfDay(): LocalDateTime = LocalDateTime(KLocalDateTime(value, KLocalTime(0, 0)))

    fun atStartOfDay(zone: ZoneId): ZonedDateTime = ZonedDateTime(atStartOfDay(), zone)

    fun atTime(
        hour: Int,
        minute: Int,
        second: Int = 0,
    ): LocalDateTime = LocalDateTime(KLocalDateTime(value, KLocalTime(hour, minute, second)))

    fun isBefore(other: LocalDate): Boolean = value < other.value

    fun isAfter(other: LocalDate): Boolean = value > other.value

    fun lengthOfMonth(): Int {
        val first = KLocalDate(value.year, value.month, 1)
        return first.daysUntil(first.plus(DatePeriod(months = 1)))
    }

    fun toEpochDay(): Long = KLocalDate(1970, 1, 1).daysUntil(value).toLong()

    fun format(formatter: DateTimeFormatter): String = formatter.format(atStartOfDay())

    override fun compareTo(other: LocalDate): Int = value.compareTo(other.value)

    override fun equals(other: Any?): Boolean = other is LocalDate && other.value == value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    companion object {
        fun now(): LocalDate = LocalDateTime.now().toLocalDate()

        fun now(zone: ZoneId): LocalDate = LocalDateTime.now(zone).toLocalDate()

        fun of(
            year: Int,
            month: Int,
            dayOfMonth: Int,
        ): LocalDate = LocalDate(KLocalDate(year, month, dayOfMonth))

        fun ofEpochDay(epochDay: Long): LocalDate = LocalDate(KLocalDate(1970, 1, 1).plus(epochDay, DateTimeUnit.DAY))

        fun parse(text: CharSequence): LocalDate = LocalDate(KLocalDate.parse(text.toString()))
    }
}

class LocalDateTime internal constructor(
    internal val value: KLocalDateTime,
) : Comparable<LocalDateTime> {
    val year: Int get() = value.year
    val monthValue: Int get() = value.month.ordinal + 1
    val month: Month get() = Month.of(monthValue)
    val dayOfMonth: Int get() = value.day
    val dayOfYear: Int get() = value.dayOfYear
    val dayOfWeek: DayOfWeek get() = DayOfWeek.entries[value.dayOfWeek.ordinal]
    val hour: Int get() = value.hour
    val minute: Int get() = value.minute
    val second: Int get() = value.second
    val nano: Int get() = value.nanosecond






    private fun shiftUtc(millis: Long): LocalDateTime =
        LocalDateTime(
            KInstant
                .fromEpochMilliseconds(value.toInstant(TimeZone.UTC).toEpochMilliseconds() + millis)
                .toLocalDateTime(TimeZone.UTC),
        )

    private fun shiftDate(period: DatePeriod): LocalDateTime = LocalDateTime(KLocalDateTime(value.date.plus(period), value.time))

    fun plusSeconds(seconds: Long): LocalDateTime = shiftUtc(seconds * 1000)

    fun plusNanos(nanos: Long): LocalDateTime =
        LocalDateTime(
            KInstant
                .fromEpochSeconds(0, value.toInstant(TimeZone.UTC).let { it.epochSeconds * 1_000_000_000L + it.nanosecondsOfSecond } + nanos)
                .toLocalDateTime(TimeZone.UTC),
        )

    fun minusNanos(nanos: Long): LocalDateTime = plusNanos(-nanos)

    fun minusSeconds(seconds: Long): LocalDateTime = shiftUtc(-seconds * 1000)

    fun plusMinutes(minutes: Long): LocalDateTime = shiftUtc(minutes * 60_000)

    fun minusMinutes(minutes: Long): LocalDateTime = shiftUtc(-minutes * 60_000)

    fun plusHours(hours: Long): LocalDateTime = shiftUtc(hours * 3_600_000)

    fun minusHours(hours: Long): LocalDateTime = shiftUtc(-hours * 3_600_000)

    fun plusDays(days: Long): LocalDateTime = shiftDate(DatePeriod(days = days.toInt()))

    fun minusDays(days: Long): LocalDateTime = shiftDate(DatePeriod(days = -days.toInt()))

    fun plusWeeks(weeks: Long): LocalDateTime = plusDays(weeks * 7)

    fun minusWeeks(weeks: Long): LocalDateTime = minusDays(weeks * 7)

    fun plusMonths(months: Long): LocalDateTime = shiftDate(DatePeriod(months = months.toInt()))

    fun minusMonths(months: Long): LocalDateTime = shiftDate(DatePeriod(months = -months.toInt()))

    fun plusYears(years: Long): LocalDateTime = shiftDate(DatePeriod(years = years.toInt()))

    fun minusYears(years: Long): LocalDateTime = shiftDate(DatePeriod(years = -years.toInt()))

    fun withHour(hour: Int): LocalDateTime = LocalDateTime(KLocalDateTime(value.date, KLocalTime(hour, value.minute, value.second, value.nanosecond)))

    fun withMinute(minute: Int): LocalDateTime = LocalDateTime(KLocalDateTime(value.date, KLocalTime(value.hour, minute, value.second, value.nanosecond)))

    fun withSecond(second: Int): LocalDateTime = LocalDateTime(KLocalDateTime(value.date, KLocalTime(value.hour, value.minute, second, value.nanosecond)))

    fun withNano(nano: Int): LocalDateTime = LocalDateTime(KLocalDateTime(value.date, KLocalTime(value.hour, value.minute, value.second, nano)))

    fun withDayOfMonth(day: Int): LocalDateTime = LocalDateTime(KLocalDateTime(KLocalDate(value.year, value.month, day), value.time))

    fun withMonth(month: Int): LocalDateTime =
        LocalDateTime(KLocalDateTime(KLocalDate(value.year, month, minOf(value.date.day, lastDayOf(value.year, month))), value.time))

    fun withYear(year: Int): LocalDateTime =
        LocalDateTime(KLocalDateTime(KLocalDate(year, value.month, minOf(value.date.day, lastDayOf(year, value.month.ordinal + 1))), value.time))

    fun withDayOfYear(day: Int): LocalDateTime =
        LocalDateTime(KLocalDateTime(KLocalDate(value.year, 1, 1).plus(day - 1, DateTimeUnit.DAY), value.time))

    fun toLocalDate(): LocalDate = LocalDate(value.date)

    fun toInstant(offset: ZoneId): Instant = Instant(value.toInstant(offset.timeZone))

    fun toEpochSecond(offset: ZoneId): Long = toInstant(offset).epochSecond

    fun atZone(zone: ZoneId): ZonedDateTime = ZonedDateTime(this, zone)

    fun atOffset(offset: ZoneOffset): ZonedDateTime = ZonedDateTime(this, offset)

    fun isBefore(other: LocalDateTime): Boolean = value < other.value

    fun isAfter(other: LocalDateTime): Boolean = value > other.value

    fun format(formatter: DateTimeFormatter): String = formatter.format(this)

    override fun compareTo(other: LocalDateTime): Int = value.compareTo(other.value)

    override fun equals(other: Any?): Boolean = other is LocalDateTime && other.value == value

    override fun hashCode(): Int = value.hashCode()

    /** Formato ISO come java.time (es. 2026-10-16T13:45:30). */
    override fun toString(): String = value.toString()

    companion object {
        val MIN: LocalDateTime = LocalDateTime(KLocalDateTime(-999_999, 1, 1, 0, 0))
        val MAX: LocalDateTime = LocalDateTime(KLocalDateTime(999_999, 12, 31, 23, 59, 59, 999_999_999))

        fun now(): LocalDateTime = now(ZoneId.systemDefault())

        fun now(zone: ZoneId): LocalDateTime = LocalDateTime(Clock.System.now().toLocalDateTime(zone.timeZone))

        fun of(
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hour: Int,
            minute: Int,
            second: Int = 0,
        ): LocalDateTime = LocalDateTime(KLocalDateTime(year, month, dayOfMonth, hour, minute, second))

        fun of(
            date: LocalDate,
            time: LocalTime,
        ): LocalDateTime = LocalDateTime(KLocalDateTime(date.value, time.value))

        fun ofInstant(
            instant: Instant,
            zone: ZoneId,
        ): LocalDateTime = LocalDateTime(instant.value.toLocalDateTime(zone.timeZone))

        fun ofEpochSecond(
            epochSecond: Long,
            nanoOfSecond: Int,
            offset: ZoneOffset,
        ): LocalDateTime =
            LocalDateTime(
                KInstant.fromEpochSeconds(epochSecond, nanoOfSecond).toLocalDateTime(offset.timeZone),
            )

        fun parse(text: CharSequence): LocalDateTime = LocalDateTime(KLocalDateTime.parse(text.toString()))
    }
}

class LocalTime internal constructor(
    internal val value: KLocalTime,
) : Comparable<LocalTime> {
    val hour: Int get() = value.hour
    val minute: Int get() = value.minute
    val second: Int get() = value.second

    override fun compareTo(other: LocalTime): Int = value.compareTo(other.value)

    fun format(formatter: DateTimeFormatter): String = formatter.format(LocalDateTime(KLocalDateTime(KLocalDate(2000, 1, 1), value)))

    override fun toString(): String = value.toString()

    companion object {
        val MIDNIGHT: LocalTime = LocalTime(KLocalTime(0, 0))
        val MAX: LocalTime = LocalTime(KLocalTime(23, 59, 59, 999_999_999))

        fun of(
            hour: Int,
            minute: Int,
            second: Int = 0,
        ): LocalTime = LocalTime(KLocalTime(hour, minute, second))

        fun now(): LocalTime = LocalTime(LocalDateTime.now().value.time)
    }
}

class Duration private constructor(
    private val millis: Long,
) : Comparable<Duration> {
    fun toMillis(): Long = millis

    fun toSeconds(): Long = millis / 1000


    val seconds: Long get() = millis / 1000

    fun toMinutes(): Long = millis / 60_000

    fun toHours(): Long = millis / 3_600_000

    fun toDays(): Long = millis / 86_400_000

    fun toMinutesPart(): Int = (toMinutes() % 60).toInt()

    fun toSecondsPart(): Int = (toSeconds() % 60).toInt()

    fun toHoursPart(): Int = (toHours() % 24).toInt()

    fun isNegative(): Boolean = millis < 0

    fun isZero(): Boolean = millis == 0L

    fun plus(other: Duration): Duration = Duration(millis + other.millis)

    fun minus(other: Duration): Duration = Duration(millis - other.millis)

    override fun compareTo(other: Duration): Int = millis.compareTo(other.millis)

    override fun equals(other: Any?): Boolean = other is Duration && other.millis == millis

    override fun hashCode(): Int = millis.hashCode()

    companion object {
        val ZERO: Duration = Duration(0)

        fun ofMillis(millis: Long): Duration = Duration(millis)

        fun ofSeconds(seconds: Long): Duration = Duration(seconds * 1000)

        fun ofMinutes(minutes: Long): Duration = Duration(minutes * 60_000)

        fun ofHours(hours: Long): Duration = Duration(hours * 3_600_000)

        fun ofDays(days: Long): Duration = Duration(days * 86_400_000)

        fun between(
            start: LocalDateTime,
            end: LocalDateTime,
        ): Duration = Duration(end.toInstant(ZoneOffset.UTC).toEpochMilli() - start.toInstant(ZoneOffset.UTC).toEpochMilli())

        fun between(
            start: Instant,
            end: Instant,
        ): Duration = Duration(end.toEpochMilli() - start.toEpochMilli())
    }
}

enum class ChronoUnit {
    MILLIS,
    SECONDS,
    MINUTES,
    HOURS,
    DAYS,
    WEEKS,
    MONTHS,
    YEARS,
    ;

    fun between(
        start: LocalDateTime,
        end: LocalDateTime,
    ): Long =
        when (this) {
            MONTHS, YEARS -> {
                val months = (end.year - start.year) * 12L + (end.monthValue - start.monthValue)
                val adjusted = if (start.plusMonths(months).isAfter(end)) months - 1 else months
                if (this == YEARS) adjusted / 12 else adjusted
            }
            else -> {
                val millis = Duration.between(start, end).toMillis()
                when (this) {
                    MILLIS -> millis
                    SECONDS -> millis / 1000
                    MINUTES -> millis / 60_000
                    HOURS -> millis / 3_600_000
                    DAYS -> millis / 86_400_000
                    else -> millis / (7 * 86_400_000L)
                }
            }
        }

    fun between(
        start: LocalDate,
        end: LocalDate,
    ): Long = between(start.atStartOfDay(), end.atStartOfDay())
}

/**
 * Formattazione con i pattern più comuni di java.time (yyyy, MM, MMM, dd, HH, mm, ss, a, EEE).
 * I nomi di mesi e giorni sono in inglese, come Locale.US / Locale.ROOT.
 */
class DateTimeFormatter private constructor(
    private val pattern: String?,
) {
    fun format(dateTime: LocalDateTime): String {
        if (pattern == null) return dateTime.toString()
        val out = StringBuilder()
        var i = 0
        while (i < pattern.length) {
            val c = pattern[i]
            if (c == '\'') {
                val end = pattern.indexOf('\'', i + 1).takeIf { it > i } ?: pattern.length
                out.append(pattern, i + 1, end)
                i = end + 1
                continue
            }
            var count = 1
            while (i + count < pattern.length && pattern[i + count] == c) count++
            out.append(field(c, count, dateTime) ?: c.toString().repeat(count))
            i += count
        }
        return out.toString()
    }

    private fun field(
        c: Char,
        count: Int,
        dt: LocalDateTime,
    ): String? =
        when (c) {
            'y', 'u' -> if (count == 2) (dt.year % 100).pad(2) else dt.year.pad(count)
            'M', 'L' ->
                when {
                    count >= 4 -> MONTHS[dt.monthValue - 1]
                    count == 3 -> MONTHS[dt.monthValue - 1].take(3)
                    else -> dt.monthValue.pad(count)
                }
            'd' -> dt.dayOfMonth.pad(count)
            'D' -> dt.dayOfYear.pad(count)
            'H' -> dt.hour.pad(count)
            'h' -> ((dt.hour + 11) % 12 + 1).pad(count)
            'm' -> dt.minute.pad(count)
            's' -> dt.second.pad(count)
            'S' -> (dt.nano / 1_000_000).pad(3).take(count)
            'a' -> if (dt.hour < 12) "AM" else "PM"
            'E' -> if (count >= 4) DAYS[dt.dayOfWeek.ordinal] else DAYS[dt.dayOfWeek.ordinal].take(3)
            else -> null
        }

    private fun Int.pad(width: Int): String = toString().padStart(width, '0')

    companion object {
        private val MONTHS =
            listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        private val DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        val ISO_LOCAL_DATE_TIME: DateTimeFormatter = DateTimeFormatter(null)
        val ISO_LOCAL_DATE: DateTimeFormatter = DateTimeFormatter("yyyy-MM-dd")

        fun ofPattern(pattern: String): DateTimeFormatter = DateTimeFormatter(pattern)

        @Suppress("UNUSED_PARAMETER")
        fun ofPattern(
            pattern: String,
            locale: Any?,
        ): DateTimeFormatter = DateTimeFormatter(pattern)

        fun ofLocalizedDate(style: FormatStyle): DateTimeFormatter =
            when (style) {
                FormatStyle.FULL -> DateTimeFormatter("EEEE, MMMM d, yyyy")
                FormatStyle.LONG -> DateTimeFormatter("MMMM d, yyyy")
                FormatStyle.MEDIUM -> DateTimeFormatter("MMM d, yyyy")
                FormatStyle.SHORT -> DateTimeFormatter("M/d/yy")
            }

        fun ofLocalizedDateTime(style: FormatStyle): DateTimeFormatter =
            DateTimeFormatter(ofLocalizedDate(style).pattern + ", HH:mm")
    }

    @Suppress("UNUSED_PARAMETER")
    fun withLocale(locale: Any?): DateTimeFormatter = this
}

enum class FormatStyle { FULL, LONG, MEDIUM, SHORT }

class Year private constructor(
    val value: Int,
) {
    companion object {
        fun now(): Year = Year(LocalDate.now().year)

        fun of(year: Int): Year = Year(year)
    }
}


private fun lastDayOf(
    year: Int,
    month: Int,
): Int {
    val first = KLocalDate(year, month, 1)
    return first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day
}
