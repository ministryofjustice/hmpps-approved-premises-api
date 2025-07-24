package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1PremisesLocalRestrictionSummary

/**
 *
 * @param id
 * @param name
 * @param apCode
 * @param fullAddress Full address, excluding postcode
 * @param postcode
 * @param apArea
 * @param bedCount The total number of beds in this premises
 * @param availableBeds The total number of beds available at this moment in time
 * @param outOfServiceBeds The total number of out of service beds at this moment in time
 * @param supportsSpaceBookings
 * @param overbookingSummary over-bookings for the next 12 weeks
 * @param managerDetails
 */
data class Cas1Premises(

  val id: java.util.UUID,

  @Schema(example = "Hope House")
  val name: kotlin.String,

  @Schema(example = "NEHOPE1")
  val apCode: kotlin.String,

  @Schema(description = "Full address, excluding postcode")
  val fullAddress: kotlin.String,

  @Schema(example = "LS1 3AD")
  val postcode: kotlin.String,

  val apArea: ApArea,

  @Schema(example = "22", description = "The total number of beds in this premises")
  val bedCount: kotlin.Int,

  @Schema(example = "20", description = "The total number of beds available at this moment in time")
  val availableBeds: kotlin.Int,

  @Schema(example = "2", description = "The total number of out of service beds at this moment in time")
  val outOfServiceBeds: kotlin.Int,

  val supportsSpaceBookings: kotlin.Boolean,

  @Schema(description = "over-bookings for the next 12 weeks")
  val overbookingSummary: kotlin.collections.List<Cas1OverbookingRange>,

  val managerDetails: kotlin.String? = null,

  @Schema(
    example = "No hate based offences",
    description = "A list of restrictions that apply specifically to this approved premises.",
  )
  val localRestrictions: List<Cas1PremisesLocalRestrictionSummary>? = emptyList(),

  @Schema(description = "Room and premise characteristics")
  val characteristics: List<Cas1SpaceCharacteristic>,
)
