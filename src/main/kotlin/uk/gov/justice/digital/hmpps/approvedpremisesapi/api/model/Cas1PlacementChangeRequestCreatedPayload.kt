package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param changeRequestId
 * @param booking
 * @param reason
 * @param changeRequestType
 */
data class Cas1PlacementChangeRequestCreatedPayload(

  val changeRequestId: java.util.UUID,

  val booking: Cas1TimelineEventPayloadBookingSummary,

  val reason: NamedId,

  val changeRequestType: Cas1ChangeRequestType,

  override val type: Cas1TimelineEventType,
) : Cas1TimelineEventContentPayload
