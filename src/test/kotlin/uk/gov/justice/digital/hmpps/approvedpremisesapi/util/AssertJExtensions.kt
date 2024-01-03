package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.assertj.core.api.AbstractLocalDateTimeAssert
import org.assertj.core.api.AbstractOffsetDateTimeAssert
import org.assertj.core.api.Assertions
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

fun AbstractOffsetDateTimeAssert<*>.isWithinTheLastMinute() {
  this.isCloseTo(OffsetDateTime.now(), Assertions.within(1, ChronoUnit.MINUTES))
}

fun AbstractLocalDateTimeAssert<*>.isWithinTheLastMinute() {
  this.isCloseTo(LocalDateTime.now(), Assertions.within(1, ChronoUnit.MINUTES))
}
