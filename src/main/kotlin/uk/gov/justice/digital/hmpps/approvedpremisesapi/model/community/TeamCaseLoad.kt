package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community

import java.time.LocalDate

data class TeamCaseLoad(
  val managedOffenders: List<ManagedOffender>
)

data class ManagedOffender(
  val offenderCrn: String,
  val allocationDate: LocalDate,
  val staffIdentifier: Long,
  val teamIdentifier: Long
)
