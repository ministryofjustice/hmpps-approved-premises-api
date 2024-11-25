package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param probationDeliveryUnits The list of pdus Ids to search within
 * @param attributes Bedspace and property attributes to filter on
 */
data class TemporaryAccommodationBedSearchParameters(

  @Schema(example = "null", required = true, description = "The list of pdus Ids to search within")
  @get:JsonProperty("probationDeliveryUnits", required = true) val probationDeliveryUnits: kotlin.collections.List<java.util.UUID>,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("serviceName", required = true) override val serviceName: kotlin.String,

  @Schema(example = "null", required = true, description = "The date the Bed will need to be free from")
  @get:JsonProperty("startDate", required = true) override val startDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "The number of days the Bed will need to be free from the start_date until")
  @get:JsonProperty("durationDays", required = true) override val durationDays: kotlin.Int,

  @Schema(example = "null", description = "Bedspace and property attributes to filter on")
  @get:JsonProperty("attributes") val attributes: kotlin.collections.List<BedSearchAttributes>? = null,
) : BedSearchParameters
