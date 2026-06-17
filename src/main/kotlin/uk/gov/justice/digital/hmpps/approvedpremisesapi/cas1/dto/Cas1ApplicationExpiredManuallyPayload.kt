package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

data class Cas1ApplicationExpiredManuallyPayload(

  val expiredReason: String,

  override val type: Cas1TimelineEventType,

) : Cas1TimelineEventContentPayload
