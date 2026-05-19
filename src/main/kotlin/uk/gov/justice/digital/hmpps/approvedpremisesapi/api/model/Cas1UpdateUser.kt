package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1UpdateUser(

  @get:JsonProperty("roles", required = true) val roles: List<ApprovedPremisesUserRole>,

  @get:JsonProperty("qualifications", required = true) val qualifications: List<UserQualification>,

  @get:JsonProperty("cruManagementAreaOverrideId") val cruManagementAreaOverrideId: java.util.UUID? = null,
)
