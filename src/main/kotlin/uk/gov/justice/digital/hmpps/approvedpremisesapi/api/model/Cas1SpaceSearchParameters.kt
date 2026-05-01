package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1SpaceSearchParameters(

  @Schema(example = "null", required = true, description = "The id of the application the space search is for")
  val applicationId: java.util.UUID,

  @Schema(example = "null", required = true, description = "The date the space is required from")
  val startDate: java.time.LocalDate,

  @Schema(example = "84", required = true, description = "The number of days the space is needed, from the start date")
  val durationInDays: kotlin.Int,

  @Schema(example = "SE5", required = true, description = "The 'target' location, in the form of a postcode district")
  val targetPostcodeDistrict: kotlin.String,

  val spaceCharacteristics: kotlin.collections.List<Cas1SpaceCharacteristic>? = null,
)
