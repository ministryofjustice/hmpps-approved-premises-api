package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
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
 * @param name
 */
data class UpdatePremises(

  @get:JsonProperty("addressLine1", required = true) val addressLine1: kotlin.String,

  @get:JsonProperty("postcode", required = true) val postcode: kotlin.String,

  @get:JsonProperty("probationRegionId", required = true) val probationRegionId: java.util.UUID,

  @get:JsonProperty("characteristicIds", required = true) val characteristicIds: kotlin.collections.List<java.util.UUID>,

  @get:JsonProperty("status", required = true) val status: PropertyStatus,

  @get:JsonProperty("addressLine2") val addressLine2: kotlin.String? = null,

  @get:JsonProperty("town") val town: kotlin.String? = null,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @get:JsonProperty("localAuthorityAreaId") val localAuthorityAreaId: java.util.UUID? = null,

  @get:JsonProperty("pdu") val pdu: kotlin.String? = null,

  @get:JsonProperty("probationDeliveryUnitId") val probationDeliveryUnitId: java.util.UUID? = null,

  @get:JsonProperty("turnaroundWorkingDayCount") val turnaroundWorkingDayCount: kotlin.Int? = null,

  @get:JsonProperty("name") val name: kotlin.String? = null,
)
