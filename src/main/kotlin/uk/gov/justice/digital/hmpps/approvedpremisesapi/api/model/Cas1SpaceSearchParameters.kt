package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic

/**
 *
 * @param applicationId The id of the application the space search is for
 * @param startDate The date the space is required from
 * @param durationInDays The number of days the space is needed, from the start date
 * @param targetPostcodeDistrict The 'target' location, in the form of a postcode district
 * @param spaceCharacteristics
 */
data class Cas1SpaceSearchParameters(

  @Schema(example = "null", required = true, description = "The id of the application the space search is for")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @Schema(example = "null", required = true, description = "The date the space is required from")
  @get:JsonProperty("startDate", required = true) val startDate: java.time.LocalDate,

  @Schema(example = "84", required = true, description = "The number of days the space is needed, from the start date")
  @get:JsonProperty("durationInDays", required = true) val durationInDays: kotlin.Int,

  @Schema(example = "SE5", required = true, description = "The 'target' location, in the form of a postcode district")
  @get:JsonProperty("targetPostcodeDistrict", required = true) val targetPostcodeDistrict: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("spaceCharacteristics") val spaceCharacteristics: kotlin.collections.List<Cas1SpaceCharacteristic>? = null,
)
