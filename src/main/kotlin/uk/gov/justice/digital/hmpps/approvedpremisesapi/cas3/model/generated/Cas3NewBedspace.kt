package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param reference
 * @param startDate Start date of the bedspace availability.
 * @param characteristicIds
 * @param notes
 */
data class Cas3NewBedspace(

  val reference: String,

  @Schema(
    example = "Sat Mar 30 00:00:00 GMT 2024",
    required = true,
    description = "Start date of the bedspace availability.",
  )
  val startDate: LocalDate,

  val characteristicIds: List<UUID>,

  val notes: String? = null,
)
