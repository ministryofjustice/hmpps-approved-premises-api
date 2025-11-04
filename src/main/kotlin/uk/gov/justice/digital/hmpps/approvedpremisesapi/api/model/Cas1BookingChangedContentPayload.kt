package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1BookingChangedContentPayload(

  @get:JsonProperty("premises", required = true) val premises: NamedId,

  @get:JsonProperty("expectedArrival", required = true) val expectedArrival: java.time.LocalDate,

  @get:JsonProperty("expectedDeparture", required = true) val expectedDeparture: java.time.LocalDate,

  @get:JsonProperty("type", required = true) override val type: Cas1TimelineEventType,

  @Schema(example = "null", description = "Only populated if the new value is different, and where schema version = 2")
  @get:JsonProperty("previousExpectedArrival") val previousExpectedArrival: java.time.LocalDate? = null,

  @Schema(example = "null", description = "Only populated if the new value is different, and where schema version = 2")
  @get:JsonProperty("previousExpectedDeparture") val previousExpectedDeparture: java.time.LocalDate? = null,

  @get:JsonProperty("characteristics") val characteristics: kotlin.collections.List<Cas1SpaceCharacteristic>? = null,

  @Schema(example = "null", description = "Only populated if the new value is different, and where schema version = 2")
  @get:JsonProperty("previousCharacteristics") val previousCharacteristics: kotlin.collections.List<Cas1SpaceCharacteristic>? = null,

  @Schema(example = "null", description = "This is deprecated, use the schema version information on the enclosing Cas1TimelineEvent")
  @Deprecated(message = "")
  @get:JsonProperty("schemaVersion") val schemaVersion: kotlin.Int? = null,

  @get:JsonProperty("transferredTo") val transferredTo: Cas1TimelineEventTransferInfo? = null,
) : Cas1TimelineEventContentPayload
