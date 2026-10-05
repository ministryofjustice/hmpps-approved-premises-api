package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.external

import java.time.Instant

data class Cas2ArrivalEventDetails(
  val arrivalDateTime: Instant,
  val arrivedByUsername: String,
)
