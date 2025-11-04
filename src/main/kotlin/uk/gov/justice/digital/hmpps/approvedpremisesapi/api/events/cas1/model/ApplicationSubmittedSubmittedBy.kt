package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param staffMember
 * @param probationArea
 * @param team
 * @param ldu
 * @param region
 */
data class ApplicationSubmittedSubmittedBy(

  val staffMember: StaffMember,

  val probationArea: ProbationArea,

  val team: Team,

  val ldu: Ldu,

  val region: Region,
)
