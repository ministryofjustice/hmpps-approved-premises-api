package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import java.util.UUID

data class Cas3BedspaceSearchResultPremisesSummary(
  val id: UUID,
  val name: String,
  val addressLine1: String,
  val postcode: String,
  val characteristics: List<Cas3CharacteristicPair>,
  val bedspaceCount: Int,
  val addressLine2: String? = null,
  val town: String? = null,
  val probationDeliveryUnitName: String? = null,
  val notes: String? = null,
  val bookedBedspaceCount: Int? = null,
)
