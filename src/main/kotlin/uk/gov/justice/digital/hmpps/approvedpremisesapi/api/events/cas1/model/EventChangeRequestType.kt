package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class EventChangeRequestType(@get:JsonValue val value: String) {
  PLACEMENT_APPEAL("placementAppeal"),
  PLACEMENT_EXTENSION("placementExtension"),
  PLANNED_TRANSFER("plannedTransfer"),
}
