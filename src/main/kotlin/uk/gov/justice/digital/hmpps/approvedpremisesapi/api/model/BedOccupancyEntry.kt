package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param type
 * @param length
 * @param startDate
 * @param endDate
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(value = BedOccupancyBookingEntry::class, name = "booking"),
  JsonSubTypes.Type(value = BedOccupancyLostBedEntry::class, name = "lost_bed"),
  JsonSubTypes.Type(value = BedOccupancyOpenEntry::class, name = "open"),
)
interface BedOccupancyEntry {
  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val type: BedOccupancyEntryType

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val length: kotlin.Int

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val startDate: java.time.LocalDate

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val endDate: java.time.LocalDate
}
