package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

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

  val startDate: LocalDate,

  @Schema(
    example = "null",
    required = true,
    description = "The number of days the Bed will need to be free from the start_date until",
  )
  val durationDays: Long,

  @Schema(example = "null", required = true, description = "The list of pdus Ids to search within")
  val probationDeliveryUnits: List<UUID>,

  val premisesFilters: PremisesFilters? = null,

  val bedspaceFilters: BedspaceFilters? = null,
)
