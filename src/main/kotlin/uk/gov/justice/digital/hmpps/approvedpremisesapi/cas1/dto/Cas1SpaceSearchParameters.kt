package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

data class Cas1SpaceSearchParameters(

  @Schema(example = "null", required = true, description = "The id of the application the space search is for")
  @get:JsonProperty("applicationId", required = true) val applicationId: UUID,

  @Schema(example = "null", required = true, description = "The date the space is required from")
  @get:JsonProperty("startDate", required = true) val startDate: LocalDate,

  @Schema(example = "84", required = true, description = "The number of days the space is needed, from the start date")
  @get:JsonProperty("durationInDays", required = true) val durationInDays: Int,

  @Schema(example = "SE5", required = true, description = "The 'target' location, in the form of a postcode district")
  @get:JsonProperty("targetPostcodeDistrict", required = true) val targetPostcodeDistrict: String,

  @get:JsonProperty("spaceCharacteristics") val spaceCharacteristics: List<Cas1SpaceCharacteristic>? = null,
)
