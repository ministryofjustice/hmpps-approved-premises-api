package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class Cas2AssessmentStatus(@get:JsonValue val value: String) {
  MORE_INFO_REQUESTED("moreInfoRequested"),
  AWAITING_DECISION("awaitingDecision"),
  ON_WAITING_LIST("onWaitingList"),
  PLACE_OFFERED("placeOffered"),
  OFFER_ACCEPTED("offerAccepted"),
  OFFER_DECLINED("offerDeclined"),
  WITHDRAWN("withdrawn"),
  CANCELLED("cancelled"),
  AWAITING_ARRIVAL("awaitingArrival"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun fromValue(value: String): Cas2AssessmentStatus? = entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
  }
}
