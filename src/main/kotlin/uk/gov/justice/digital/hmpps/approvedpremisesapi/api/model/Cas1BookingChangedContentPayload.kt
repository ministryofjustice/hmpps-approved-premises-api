package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param premises
 * @param expectedArrival
 * @param expectedDeparture
 * @param previousExpectedArrival Only populated if the new value is different, and where schema version = 2
 * @param previousExpectedDeparture Only populated if the new value is different, and where schema version = 2
 * @param characteristics
 * @param previousCharacteristics Only populated if the new value is different, and where schema version = 2
 * @param schemaVersion This is deprecated, use the schema version information on the enclosing Cas1TimelineEvent
 * @param transferredTo
 */
data class Cas1BookingChangedContentPayload(

  val premises: NamedId,

  val expectedArrival: java.time.LocalDate,

  val expectedDeparture: java.time.LocalDate,

  override val type: Cas1TimelineEventType,

  @Schema(example = "null", description = "Only populated if the new value is different, and where schema version = 2")
  val previousExpectedArrival: java.time.LocalDate? = null,

  @Schema(example = "null", description = "Only populated if the new value is different, and where schema version = 2")
  val previousExpectedDeparture: java.time.LocalDate? = null,

  val characteristics: kotlin.collections.List<Cas1SpaceCharacteristic>? = null,

  @Schema(example = "null", description = "Only populated if the new value is different, and where schema version = 2")
  val previousCharacteristics: kotlin.collections.List<Cas1SpaceCharacteristic>? = null,

  @Schema(example = "null", description = "This is deprecated, use the schema version information on the enclosing Cas1TimelineEvent")
  @Deprecated(message = "")
  val schemaVersion: kotlin.Int? = null,

  val transferredTo: Cas1TimelineEventTransferInfo? = null,
) : Cas1TimelineEventContentPayload
