package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

data class ApplicationSubmittedSubmittedBy(

  val staffMember: StaffMember,

  val probationArea: ProbationArea,

  val team: Team,

  val ldu: Ldu,

  val region: Region,
)
