package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

data class Cas1RequestedPlacementPeriod(
  val arrival: java.time.LocalDate,
  val arrivalFlexible: Boolean?,
  val duration: Int,
)
