package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

/**
 *
 * @param staffMember
 * @param probationArea
 */
data class WithdrawnBy(

  val staffMember: StaffMember,

  val probationArea: ProbationArea,
)
