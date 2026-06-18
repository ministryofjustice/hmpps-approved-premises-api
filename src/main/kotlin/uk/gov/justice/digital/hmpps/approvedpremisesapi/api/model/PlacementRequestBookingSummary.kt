package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceCharacteristic

data class PlacementRequestBookingSummary(

  val id: java.util.UUID,
  val premisesId: java.util.UUID,
  val premisesName: String,
  val arrivalDate: java.time.LocalDate,
  val departureDate: java.time.LocalDate,
  val createdAt: java.time.Instant,
  val type: Type,
  val characteristics: List<Cas1SpaceCharacteristic>? = null,
  val reason: String? = null,
) {

  @Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
  enum class Type(@get:JsonValue val value: String) {

    space("space"),
    legacy("legacy"),
    ;

    companion object {
      @JvmStatic
      @JsonCreator
      fun forValue(value: String): Type = values().first { it.value == value }
    }
  }
}
