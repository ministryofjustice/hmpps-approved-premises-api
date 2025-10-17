package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import java.util.UUID

data class Cas3UpdatePremises(
  val reference: String,
  val addressLine1: String,
  val postcode: String,
  val probationRegionId: UUID,
  val probationDeliveryUnitId: UUID,
  val characteristicIds: List<UUID>,
  val addressLine2: String? = null,
  val town: String? = null,
  val localAuthorityAreaId: UUID? = null,
  val notes: String? = null,
  @Deprecated("Will be replaced with turnaroundWorkingDays for v2")
  val turnaroundWorkingDayCount: Int?,
  val turnaroundWorkingDays: Int?,
)
