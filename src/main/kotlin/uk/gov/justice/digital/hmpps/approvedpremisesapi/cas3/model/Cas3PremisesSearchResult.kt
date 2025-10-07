package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import java.util.UUID

data class Cas3PremisesSearchResult(
  val id: UUID,
  val reference: String,
  val addressLine1: String,
  val postcode: String,
  val pdu: String,
  val addressLine2: String? = null,
  val town: String? = null,
  val localAuthorityAreaName: String? = null,
  val bedspaces: List<Cas3BedspacePremisesSearchResult>? = null,
  val totalArchivedBedspaces: Int? = null,
)
