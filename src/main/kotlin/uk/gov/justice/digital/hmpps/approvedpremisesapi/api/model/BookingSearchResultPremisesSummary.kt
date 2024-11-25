package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param addressLine1
 * @param postcode
 * @param addressLine2
 * @param town
 */
data class BookingSearchResultPremisesSummary(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("addressLine1", required = true) val addressLine1: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("postcode", required = true) val postcode: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("addressLine2") val addressLine2: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("town") val town: kotlin.String? = null,
)
