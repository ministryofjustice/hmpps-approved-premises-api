package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.UUID

data class Cas1KeyWorkerAllocation(
  @Deprecated("This will be removed in a future release")
  val keyWorker: StaffMember,
  val allocatedAt: java.time.LocalDate? = null,
  val name: String,
  val userId: UUID?,
  val emailAddress: String?,
)
