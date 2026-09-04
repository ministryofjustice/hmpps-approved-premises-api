package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import java.time.LocalDate

data class Cas1AuthorisedPlacementPeriod(
  val arrival: LocalDate,
  val arrivalFlexible: Boolean?,
  val duration: Int,
)
