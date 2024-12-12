package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param name 
 * @param addressLine1 
 * @param postcode 
 * @param probationRegionId 
 * @param characteristicIds 
 * @param status 
 * @param addressLine2 
 * @param town 
 * @param notes 
 * @param localAuthorityAreaId 
 * @param pdu 
 * @param probationDeliveryUnitId 
 * @param turnaroundWorkingDayCount 
 */
data class NewPremises(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true) val name: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("addressLine1", required = true) val addressLine1: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("postcode", required = true) val postcode: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("probationRegionId", required = true) val probationRegionId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("characteristicIds", required = true) val characteristicIds: kotlin.collections.List<java.util.UUID>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("status", required = true) val status: PropertyStatus,

    @Schema(example = "null", description = "")
    @get:JsonProperty("addressLine2") val addressLine2: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("town") val town: kotlin.String? = null,

    @Schema(example = "some notes about this property", description = "")
    @get:JsonProperty("notes") val notes: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("localAuthorityAreaId") val localAuthorityAreaId: java.util.UUID? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("pdu") val pdu: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("probationDeliveryUnitId") val probationDeliveryUnitId: java.util.UUID? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("turnaroundWorkingDayCount") val turnaroundWorkingDayCount: kotlin.Int? = null
) {

}

