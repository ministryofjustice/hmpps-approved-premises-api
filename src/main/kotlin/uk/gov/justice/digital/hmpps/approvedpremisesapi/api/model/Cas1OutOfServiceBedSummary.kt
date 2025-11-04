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

  val id: java.util.UUID,

  val bedId: java.util.UUID,

  val startDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "This date is inclusive. The bed will be unavailable for the whole of the day")
  val endDate: java.time.LocalDate,

  val reason: Cas1OutOfServiceBedReason,

  val characteristics: kotlin.collections.List<Cas1SpaceCharacteristic>,

  val roomName: kotlin.String? = null,
)
