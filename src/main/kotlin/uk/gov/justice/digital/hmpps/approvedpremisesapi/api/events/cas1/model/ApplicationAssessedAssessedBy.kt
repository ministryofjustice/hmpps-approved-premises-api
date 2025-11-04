package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ApplicationAssessedAssessedBy(

  @get:JsonProperty("staffMember") val staffMember: StaffMember? = null,

  @get:JsonProperty("probationArea") val probationArea: ProbationArea? = null,

  @get:JsonProperty("cru") val cru: Cru? = null,
)
