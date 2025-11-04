package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param updatedFields
 */
data class CAS3AssessmentUpdatedEvent(

  val updatedFields: kotlin.collections.List<CAS3AssessmentUpdatedField>,

  @Schema(example = "364145f9-0af8-488e-9901-b4c46cd9ba37", required = true, description = "The UUID of an event")
  override val id: java.util.UUID,

  override val timestamp: java.time.Instant,

  override val eventType: EventType,
) : CAS3Event
