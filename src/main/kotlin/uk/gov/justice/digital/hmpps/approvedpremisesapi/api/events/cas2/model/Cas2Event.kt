package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import io.swagger.v3.oas.annotations.media.Schema

interface Cas2Event {
  @get:Schema(example = "364145f9-0af8-488e-9901-b4c46cd9ba37", requiredMode = Schema.RequiredMode.REQUIRED, description = "The UUID of an event")
  val id: java.util.UUID

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val timestamp: java.time.Instant

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val eventType: EventType
}
