package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class BedSearchResultPremisesSummary(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("name", required = true) val name: String,

  @get:JsonProperty("addressLine1", required = true) val addressLine1: String,

  @get:JsonProperty("postcode", required = true) val postcode: String,

  @get:JsonProperty("characteristics", required = true) val characteristics: List<CharacteristicPair>,

  @Schema(example = "null", required = true, description = "the total number of Beds in the Premises")
  @get:JsonProperty("bedCount", required = true) val bedCount: Int,

  @get:JsonProperty("addressLine2") val addressLine2: String? = null,

  @get:JsonProperty("town") val town: String? = null,

  @get:JsonProperty("probationDeliveryUnitName") val probationDeliveryUnitName: String? = null,

  @get:JsonProperty("notes") val notes: String? = null,

  @Schema(example = "null", description = "the total number of booked Beds in the Premises")
  @get:JsonProperty("bookedBedCount") val bookedBedCount: Int? = null,
)
