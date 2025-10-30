package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class TransferReason(@get:JsonValue val value: String) {

  extendingPlacementNoCapacityAtCurrentAP("extending_placement_no_capacity_at_current_ap"),
  placementPrioritisation("placement_prioritisation"),
  movingPersonCloserToResettlementArea("moving_person_closer_to_resettlement_area"),
  conflictWithStaff("conflict_with_staff"),
  localCommunityIssue("local_community_issue"),
  riskToResident("risk_to_resident"),
  publicProtection("public_protection"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): TransferReason = values().first { it.value == value }
  }
}
