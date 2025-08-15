package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1KeyWorkerAllocation(
  @Deprecated("Once keyWorkerUser is non optional, this will be removed")
  val keyWorker: StaffMember,
  val keyWorkerUser: UserSummary? = null,
  val allocatedAt: java.time.LocalDate? = null,
)
