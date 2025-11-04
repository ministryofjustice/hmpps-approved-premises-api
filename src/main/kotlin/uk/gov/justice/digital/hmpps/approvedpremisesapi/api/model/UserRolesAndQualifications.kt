package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class UserRolesAndQualifications(

  @get:JsonProperty("roles", required = true) val roles: kotlin.collections.List<ApprovedPremisesUserRole>,

  @get:JsonProperty("qualifications", required = true) val qualifications: kotlin.collections.List<UserQualification>,
)
