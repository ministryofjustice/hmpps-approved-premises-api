package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class BedSearchResultPremisesSummary(

  val id: java.util.UUID,

  val name: kotlin.String,

  val addressLine1: kotlin.String,

  val postcode: kotlin.String,

  val characteristics: kotlin.collections.List<CharacteristicPair>,

  @Schema(example = "null", required = true, description = "the total number of Beds in the Premises")
  val bedCount: kotlin.Int,

  val addressLine2: kotlin.String? = null,

  val town: kotlin.String? = null,

  val probationDeliveryUnitName: kotlin.String? = null,

  val notes: kotlin.String? = null,

  @Schema(example = "null", description = "the total number of booked Beds in the Premises")
  val bookedBedCount: kotlin.Int? = null,
)
