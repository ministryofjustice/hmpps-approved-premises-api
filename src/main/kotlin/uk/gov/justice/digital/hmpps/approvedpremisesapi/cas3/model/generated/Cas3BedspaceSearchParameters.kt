package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param startDate
 * @param durationDays The number of days the Bed will need to be free from the start_date until
 * @param probationDeliveryUnits The list of pdus Ids to search within
 * @param premisesFilters
 * @param bedspaceFilters
 */
data class Cas3BedspaceSearchParameters(

  @get:JsonProperty("startDate", required = true) val startDate: LocalDate,

  @Schema(
    example = "null",
    required = true,
    description = "The number of days the Bed will need to be free from the start_date until",
  )
  @get:JsonProperty("durationDays", required = true) val durationDays: Long,

  @Schema(example = "null", required = true, description = "The list of pdus Ids to search within")
  @get:JsonProperty("probationDeliveryUnits", required = true) val probationDeliveryUnits: List<UUID>,

  @get:JsonProperty("premisesFilters") val premisesFilters: PremisesFilters? = null,

  @get:JsonProperty("bedspaceFilters") val bedspaceFilters: BedspaceFilters? = null,
)
