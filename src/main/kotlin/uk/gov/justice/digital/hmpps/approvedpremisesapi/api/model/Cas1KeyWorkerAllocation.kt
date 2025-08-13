package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1KeyWorkerAllocation(
  val keyWorker: StaffMember,
  val keyWorkerUser: UserSummary? = null,
  val allocatedAt: java.time.LocalDate? = null,
)
