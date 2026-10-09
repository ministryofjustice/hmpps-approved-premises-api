package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import java.time.Instant

data class Cas2ArrivalDto(
  val markedAsArrivedDateTime: Instant,
  val arrivedByUsername: String,
)
