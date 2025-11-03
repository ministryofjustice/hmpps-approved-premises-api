package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param expiredReason
 * @param type
 */
data class Cas1ApplicationExpiredManuallyPayload(

  val expiredReason: String,

  override val type: Cas1TimelineEventType,

) : Cas1TimelineEventContentPayload
