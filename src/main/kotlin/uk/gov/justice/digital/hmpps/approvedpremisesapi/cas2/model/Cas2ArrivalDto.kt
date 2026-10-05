package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import java.time.Instant

data class Cas2ArrivalDto(
  val arrivalDateTime: Instant,
  val arrivedByUsername: String,
)
