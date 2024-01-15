package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import java.sql.Timestamp
import java.time.OffsetDateTime

const val NANO_MAX = 1E6

fun OffsetDateTime?.toTimestampOrNull(): Timestamp? {
  return this?.toTimestamp()
}

fun OffsetDateTime.toTimestamp(): Timestamp {
  return Timestamp.from(this.toInstant())
}

fun OffsetDateTime.roundNanosToMillisToAccountForLossOfPrecisionInPostgres(): OffsetDateTime {
  val millis = Math.round(this.nano / NANO_MAX)
  val roundedNanos = (millis * NANO_MAX).toInt()
  return this.withNano(roundedNanos)
}
