package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic

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

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bedId", required = true) val bedId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("startDate", required = true) val startDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "This date is inclusive. The bed will be unavailable for the whole of the day")
  @get:JsonProperty("endDate", required = true) val endDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reason", required = true) val reason: Cas1OutOfServiceBedReason,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("characteristics", required = true) val characteristics: kotlin.collections.List<Cas1SpaceCharacteristic>,

  @Schema(example = "null", description = "")
  @get:JsonProperty("roomName") val roomName: kotlin.String? = null,
)
