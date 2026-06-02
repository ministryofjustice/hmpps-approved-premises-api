package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

fun Instant.minusDays(days: Long): Instant = this.minus(Duration.ofDays(days))

fun OffsetDateTime.roundNanosToMillisToAccountForLossOfPrecisionInPostgres(): OffsetDateTime = this.withNano(nanosToNearestMilli(this.nano.toLong()))

fun LocalDateTime.roundNanosToMillisToAccountForLossOfPrecisionInPostgres(): LocalDateTime = this.withNano(nanosToNearestMilli(this.nano.toLong()))

private fun nanosToNearestMilli(nanos: Long): Int {
  val millis = TimeUnit.NANOSECONDS.toMillis(nanos)
  return TimeUnit.MILLISECONDS.toNanos(millis).toInt()
}
