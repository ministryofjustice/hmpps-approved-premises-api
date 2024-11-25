package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param startDate The date the space is required from
 * @param durationInDays The number of days the space is needed, from the start date
 * @param targetPostcodeDistrict The 'target' location, in the form of a postcode district
 * @param requirements
 */
data class Cas1SpaceSearchParameters(

  @Schema(example = "null", required = true, description = "The date the space is required from")
  @get:JsonProperty("startDate", required = true) val startDate: java.time.LocalDate,

  @Schema(example = "84", required = true, description = "The number of days the space is needed, from the start date")
  @get:JsonProperty("durationInDays", required = true) val durationInDays: kotlin.Int,

  @Schema(example = "SE5", required = true, description = "The 'target' location, in the form of a postcode district")
  @get:JsonProperty("targetPostcodeDistrict", required = true) val targetPostcodeDistrict: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("requirements", required = true) val requirements: Cas1SpaceSearchRequirements,
)
