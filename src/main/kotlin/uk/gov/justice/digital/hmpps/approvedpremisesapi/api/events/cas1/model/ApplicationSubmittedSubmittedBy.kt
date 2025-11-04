package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ApplicationSubmittedSubmittedBy(

  @get:JsonProperty("staffMember", required = true) val staffMember: StaffMember,

  @get:JsonProperty("probationArea", required = true) val probationArea: ProbationArea,

  @get:JsonProperty("team", required = true) val team: Team,

  @get:JsonProperty("ldu", required = true) val ldu: Ldu,

  @get:JsonProperty("region", required = true) val region: Region,
)
