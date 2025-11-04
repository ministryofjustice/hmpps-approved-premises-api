package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class UpdatePremises(

  val addressLine1: kotlin.String,

  val postcode: kotlin.String,

  val probationRegionId: java.util.UUID,

  val characteristicIds: kotlin.collections.List<java.util.UUID>,

  val status: PropertyStatus,

  val addressLine2: kotlin.String? = null,

  val town: kotlin.String? = null,

  val notes: kotlin.String? = null,

  val localAuthorityAreaId: java.util.UUID? = null,

  val pdu: kotlin.String? = null,

  val probationDeliveryUnitId: java.util.UUID? = null,

  val turnaroundWorkingDayCount: kotlin.Int? = null,

  val name: kotlin.String? = null,
)
