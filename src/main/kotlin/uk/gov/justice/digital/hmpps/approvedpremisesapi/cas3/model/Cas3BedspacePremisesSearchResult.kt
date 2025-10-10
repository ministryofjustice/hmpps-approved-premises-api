package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import java.util.UUID

data class Cas3BedspacePremisesSearchResult(
  val id: UUID,
  val reference: String,
  val status: Cas3BedspaceStatus,
)
