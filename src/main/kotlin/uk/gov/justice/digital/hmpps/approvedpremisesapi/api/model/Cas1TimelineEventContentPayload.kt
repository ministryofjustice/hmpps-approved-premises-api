package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType

/**
 * Base schema for all timeline event payloads
 * @param type
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(value = Cas1BookingCancelledContentPayload::class, name = "booking_cancelled"),
  JsonSubTypes.Type(value = Cas1BookingChangedContentPayload::class, name = "booking_changed"),
  JsonSubTypes.Type(value = Cas1BookingMadeContentPayload::class, name = "booking_made"),
  JsonSubTypes.Type(value = Cas1PlacementChangeRequestCreatedPayload::class, name = "placement_change_request_created"),
  JsonSubTypes.Type(value = Cas1PlacementChangeRequestRejectedPayload::class, name = "placement_change_request_rejected"),
)
interface Cas1TimelineEventContentPayload {
  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val type: Cas1TimelineEventType
}
