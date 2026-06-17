package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import java.util.UUID

data class Cas1KeyWorkerAllocation(
  val allocatedAt: java.time.LocalDate? = null,
  val name: String,
  val userId: UUID?,
  val emailAddress: String?,
)
