package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param crn
 * @param name
 */
data class BookingSearchResultPersonSummary(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("crn", required = true) val crn: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("name") val name: kotlin.String? = null,
)
