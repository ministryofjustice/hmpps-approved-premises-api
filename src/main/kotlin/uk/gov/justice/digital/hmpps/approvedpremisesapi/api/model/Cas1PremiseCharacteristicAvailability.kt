package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param characteristic
 * @param availableBedsCount the number of available beds with this characteristic
 * @param bookingsCount the number of bookings requiring this characteristic
 */
data class Cas1PremiseCharacteristicAvailability(

  val characteristic: Cas1SpaceBookingCharacteristic,

  @Schema(example = "null", required = true, description = "the number of available beds with this characteristic")
  val availableBedsCount: kotlin.Int,

  @Schema(example = "null", required = true, description = "the number of bookings requiring this characteristic")
  val bookingsCount: kotlin.Int,
)
