package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param addressLine1
 * @param postcode
 * @param characteristics
 * @param bedCount the total number of Beds in the Premises
 * @param addressLine2
 * @param town
 * @param probationDeliveryUnitName
 * @param notes
 * @param bookedBedCount the total number of booked Beds in the Premises
 */
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
