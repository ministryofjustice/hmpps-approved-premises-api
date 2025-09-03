package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import java.util.UUID

data class Cas3NewPremises(

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
  val turnaroundWorkingDays: Int? = null,
)