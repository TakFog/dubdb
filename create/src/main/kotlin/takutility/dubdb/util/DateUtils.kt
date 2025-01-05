package takutility.dubdb.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

fun Instant?.isBefore(date: LocalDate?) = this != null && date != null
        && this.atZone(ZoneOffset.UTC).isBefore(date.atStartOfDay(ZoneOffset.UTC))
