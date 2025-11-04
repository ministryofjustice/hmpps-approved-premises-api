package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param crn
 * @param convictionId
 * @param deliusEventNumber
 * @param offenceId
 * @param applicationOrigin
 */
data class NewApplication(

  val crn: kotlin.String,

  @Schema(example = "1502724704", description = "")
  val convictionId: kotlin.Long? = null,

  @Schema(example = "7", description = "")
  val deliusEventNumber: kotlin.String? = null,

  @Schema(example = "M1502750438", description = "")
  val offenceId: kotlin.String? = null,

  val applicationOrigin: ApplicationOrigin? = ApplicationOrigin.homeDetentionCurfew,
)
