package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param eventDetails
 */
data class CAS3PersonDepartureUpdatedEvent(

  val eventDetails: CAS3PersonDepartedEventDetails,

  @Schema(example = "364145f9-0af8-488e-9901-b4c46cd9ba37", required = true, description = "The UUID of an event")
  override val id: java.util.UUID,

  override val timestamp: java.time.Instant,

  override val eventType: EventType,
) : CAS3Event
