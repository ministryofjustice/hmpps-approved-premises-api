package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventContentPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventTransferInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
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

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("premises", required = true) val premises: NamedId,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("expectedArrival", required = true) val expectedArrival: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("expectedDeparture", required = true) val expectedDeparture: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) override val type: Cas1TimelineEventType,

    @Schema(example = "null", description = "Only populated if the new value is different, and where schema version = 2")
    @get:JsonProperty("previousExpectedArrival") val previousExpectedArrival: java.time.LocalDate? = null,

    @Schema(example = "null", description = "Only populated if the new value is different, and where schema version = 2")
    @get:JsonProperty("previousExpectedDeparture") val previousExpectedDeparture: java.time.LocalDate? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("characteristics") val characteristics: kotlin.collections.List<Cas1SpaceCharacteristic>? = null,

    @Schema(example = "null", description = "Only populated if the new value is different, and where schema version = 2")
    @get:JsonProperty("previousCharacteristics") val previousCharacteristics: kotlin.collections.List<Cas1SpaceCharacteristic>? = null,

    @Schema(example = "null", description = "This is deprecated, use the schema version information on the enclosing Cas1TimelineEvent")
    @Deprecated(message = "")
    @get:JsonProperty("schemaVersion") val schemaVersion: kotlin.Int? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("transferredTo") val transferredTo: Cas1TimelineEventTransferInfo? = null
    ) : Cas1TimelineEventContentPayload{

}

