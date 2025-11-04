package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param arrivalDate
 * @param departureDate
 * @param bedId
 * @param premisesId
 */
data class NewPlacementRequestBooking(

  @Schema(example = "Thu Jul 28 01:00:00 BST 2022", required = true, description = "")
  val arrivalDate: java.time.LocalDate,

  @Schema(example = "Fri Sep 30 01:00:00 BST 2022", required = true, description = "")
  val departureDate: java.time.LocalDate,

  val bedId: java.util.UUID? = null,

  val premisesId: java.util.UUID? = null,
)
