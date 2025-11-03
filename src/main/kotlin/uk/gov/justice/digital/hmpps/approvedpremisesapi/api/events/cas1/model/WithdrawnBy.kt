package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param staffMember
 * @param probationArea
 */
data class WithdrawnBy(

  @get:JsonProperty("staffMember", required = true) val staffMember: StaffMember,

  @get:JsonProperty("probationArea", required = true) val probationArea: ProbationArea,
)
