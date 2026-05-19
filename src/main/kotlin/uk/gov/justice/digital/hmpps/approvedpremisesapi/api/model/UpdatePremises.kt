package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class UpdatePremises(

  @get:JsonProperty("addressLine1", required = true) val addressLine1: String,

  @get:JsonProperty("postcode", required = true) val postcode: String,

  @get:JsonProperty("probationRegionId", required = true) val probationRegionId: java.util.UUID,

  @get:JsonProperty("characteristicIds", required = true) val characteristicIds: List<java.util.UUID>,

  @get:JsonProperty("status", required = true) val status: PropertyStatus,

  @get:JsonProperty("addressLine2") val addressLine2: String? = null,

  @get:JsonProperty("town") val town: String? = null,

  @get:JsonProperty("notes") val notes: String? = null,

  @get:JsonProperty("localAuthorityAreaId") val localAuthorityAreaId: java.util.UUID? = null,

  @get:JsonProperty("pdu") val pdu: String? = null,

  @get:JsonProperty("probationDeliveryUnitId") val probationDeliveryUnitId: java.util.UUID? = null,

  @get:JsonProperty("turnaroundWorkingDayCount") val turnaroundWorkingDayCount: Int? = null,

  @get:JsonProperty("name") val name: String? = null,
)
