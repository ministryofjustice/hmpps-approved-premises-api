package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param bedId
 * @param startDate
 * @param endDate This date is inclusive. The bed will be unavailable for the whole of the day
 * @param reason
 * @param characteristics
 * @param roomName
 */
data class Cas1OutOfServiceBedSummary(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("bedId", required = true) val bedId: java.util.UUID,

  @get:JsonProperty("startDate", required = true) val startDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "This date is inclusive. The bed will be unavailable for the whole of the day")
  @get:JsonProperty("endDate", required = true) val endDate: java.time.LocalDate,

  @get:JsonProperty("reason", required = true) val reason: Cas1OutOfServiceBedReason,

  @get:JsonProperty("characteristics", required = true) val characteristics: kotlin.collections.List<Cas1SpaceCharacteristic>,

  @get:JsonProperty("roomName") val roomName: kotlin.String? = null,
)
