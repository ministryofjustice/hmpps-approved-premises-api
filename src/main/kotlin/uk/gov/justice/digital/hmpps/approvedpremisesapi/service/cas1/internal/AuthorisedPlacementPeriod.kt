package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.internal

import java.time.LocalDate

data class AuthorisedPlacementPeriod(
  val arrival: LocalDate,
  val arrivalFlexible: Boolean?,
  val duration: Int,
)
