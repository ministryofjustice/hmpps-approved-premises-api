package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param staffMember
 * @param probationArea
 * @param cru
 */
data class ApplicationAssessedAssessedBy(

  val staffMember: StaffMember? = null,

  val probationArea: ProbationArea? = null,

  val cru: Cru? = null,
)
