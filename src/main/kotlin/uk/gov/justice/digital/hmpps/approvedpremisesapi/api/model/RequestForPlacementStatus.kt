package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class RequestForPlacementStatus(@get:JsonValue val value: String) {
  /**
   * Only returned for placement applications. This is because a placement request
   * would not exist if the application was rejected. In the future
   * we can consider returning this status for rejected applications
   * that include an arrival date (i.e. a request for placement)
   */
  requestRejected("request_rejected"),

  /**
   * Only returned for placement applications. This is because a placement request
   * is created in a state of 'awaiting match' when an application is approved
   *
   * In the future we can consider returning this status for submitted applications
   * that include an arrival date (i.e. a request for placement)
   */
  requestSubmitted("request_submitted"),

  /**
   * Request for placement is approved and a placement is pending
   *
   * A request for placement may be in this state if a placement
   * was created and subsequently cancelled
   */
  awaitingMatch("awaiting_match"),

  /**
   * A withdrawn request for placement may have associated
   * bookings (Which should also be cancelled)
   *
   * If a request for placement has at least one booking with
   * an arrival, it cannot be withdrawn
   */
  requestWithdrawn("request_withdrawn"),

  /**
   * At least one active placement (i.e. not cancelled) exists for
   * this request for placement
   */
  placementBooked("placement_booked"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): RequestForPlacementStatus = entries.first { it.value == value }
  }
}
