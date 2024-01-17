package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

fun OffsetDateTime?.toTimestampOrNull(): Timestamp? {
  return this?.toTimestamp()
}

fun OffsetDateTime.toTimestamp(): Timestamp {
  return Timestamp.from(this.toInstant())
}

fun OffsetDateTime.roundNanosToMillisToAccountForLossOfPrecisionInPostgres(): OffsetDateTime {
  val millis = TimeUnit.NANOSECONDS.toMillis(this.nano.toLong())
  val roundedNanos = TimeUnit.MILLISECONDS.toNanos(millis).toInt()
  return this.withNano(roundedNanos)
}
