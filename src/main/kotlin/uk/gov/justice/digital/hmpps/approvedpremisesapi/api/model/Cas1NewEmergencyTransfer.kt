package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param destinationPremisesId
 * @param arrivalDate The expected arrival date for the new space booking. The existing space booking will be updated to end on this date
 * @param departureDate The expected departure date for the new space booking
 */
data class Cas1NewEmergencyTransfer(

  val destinationPremisesId: java.util.UUID,

  @Schema(example = "null", required = true, description = "The expected arrival date for the new space booking. The existing space booking will be updated to end on this date")
  val arrivalDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "The expected departure date for the new space booking")
  val departureDate: java.time.LocalDate,
)
