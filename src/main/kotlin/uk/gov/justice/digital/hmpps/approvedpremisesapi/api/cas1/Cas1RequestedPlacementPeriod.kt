package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1RequestedPlacementPeriod(
  val arrival: java.time.LocalDate,
  val arrivalFlexible: Boolean?,
  @Schema(description = "Duration requested by the applicant, which may be the default duration if not overridden")
  val duration: Int?,
)
