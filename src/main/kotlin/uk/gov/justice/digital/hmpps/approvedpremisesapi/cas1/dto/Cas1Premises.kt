package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.controller.Cas1PremisesLocalRestrictionSummary
import java.util.UUID

data class Cas1Premises(

  val id: UUID,

  @Schema(example = "Hope House")
  val name: String,

  @Schema(example = "NEHOPE1")
  val apCode: String,

  @Schema(description = "Full address, excluding postcode")
  val fullAddress: String,

  @Schema(example = "LS1 3AD")
  val postcode: String,

  val apArea: ApArea,

  @Schema(example = "22", description = "The total number of beds in this premises")
  val bedCount: Int,

  @Schema(example = "20", description = "The total number of beds available at this moment in time")
  val availableBeds: Int,

  @Schema(example = "2", description = "The total number of out of service beds at this moment in time")
  val outOfServiceBeds: Int,

  val supportsSpaceBookings: Boolean,

  val managerDetails: String? = null,

  @Schema(
    example = "No hate based offences",
    description = "A list of restrictions that apply specifically to this approved premises.",
  )
  val localRestrictions: List<Cas1PremisesLocalRestrictionSummary>? = emptyList(),

  @Schema(description = "Room and premise characteristics")
  val characteristics: List<Cas1SpaceCharacteristic>,

  @Schema(description = "This is deprecated and only returns an empty list", deprecated = true)
  val overbookingSummary: List<Cas1OverbookingRange> = emptyList(),
)
