package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.UUID

data class ProbationRegion(
  val id: UUID,
  val name: String,
  val hptEmail: String,
)
