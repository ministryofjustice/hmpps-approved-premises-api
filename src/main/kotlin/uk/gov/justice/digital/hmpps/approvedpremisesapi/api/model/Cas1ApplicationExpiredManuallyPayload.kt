package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param expiredReason
 * @param type
 */
data class Cas1ApplicationExpiredManuallyPayload(

  @Schema(example = "null", required = true, description = "")
  val expiredReason: String,

  @Schema(example = "null", required = true, description = "")
  override val type: Cas1TimelineEventType,

) : Cas1TimelineEventContentPayload
