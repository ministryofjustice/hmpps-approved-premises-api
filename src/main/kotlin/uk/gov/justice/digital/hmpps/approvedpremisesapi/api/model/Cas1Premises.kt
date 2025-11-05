package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1PremisesLocalRestrictionSummary

data class Cas1Premises(

  val id: java.util.UUID,

  @field:Schema(example = "Hope House")
  val name: kotlin.String,

  @field:Schema(example = "NEHOPE1")
  val apCode: kotlin.String,

  @field:Schema(description = "Full address, excluding postcode")
  val fullAddress: kotlin.String,

  @field:Schema(example = "LS1 3AD")
  val postcode: kotlin.String,

  val apArea: ApArea,

  @field:Schema(example = "22", description = "The total number of beds in this premises")
  val bedCount: kotlin.Int,

  @field:Schema(example = "20", description = "The total number of beds available at this moment in time")
  val availableBeds: kotlin.Int,

  @field:Schema(example = "2", description = "The total number of out of service beds at this moment in time")
  val outOfServiceBeds: kotlin.Int,

  val supportsSpaceBookings: kotlin.Boolean,

  val managerDetails: kotlin.String? = null,

  @field:Schema(
    example = "No hate based offences",
    description = "A list of restrictions that apply specifically to this approved premises.",
  )
  val localRestrictions: List<Cas1PremisesLocalRestrictionSummary>? = emptyList(),

  @field:Schema(description = "Room and premise characteristics")
  val characteristics: List<Cas1SpaceCharacteristic>,

  @field:Schema(description = "This is deprecated and only returns an empty list", deprecated = true)
  val overbookingSummary: List<Cas1OverbookingRange> = emptyList(),
)
