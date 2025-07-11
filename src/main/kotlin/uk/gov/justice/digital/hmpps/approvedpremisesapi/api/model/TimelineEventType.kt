package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: cas3PersonArrived,cas3PersonDeparted,applicationTimelineNote,cas2ApplicationSubmitted,cas2Note,cas2StatusUpdate,cas2PrisonTransfer,cas2NewPomAssigned
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class TimelineEventType(@get:JsonValue val value: kotlin.String) {

  cas3PersonArrived("cas3_person_arrived"),
  cas3PersonDeparted("cas3_person_departed"),
  applicationTimelineNote("application_timeline_note"),
  cas2ApplicationSubmitted("cas2_application_submitted"),
  cas2Note("cas2_note"),
  cas2StatusUpdate("cas2_status_update"),
  cas2PrisonTransfer("cas2_prison_transfer"),
  cas2NewPomAssigned("cas2_new_pom_assigned"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): TimelineEventType = entries.first { it.value == value }
  }
}
