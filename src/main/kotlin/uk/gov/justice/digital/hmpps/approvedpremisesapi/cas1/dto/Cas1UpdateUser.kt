package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification
import java.util.UUID

data class Cas1UpdateUser(

  @get:JsonProperty("roles", required = true) val roles: List<ApprovedPremisesUserRole>,

  @get:JsonProperty("qualifications", required = true) val qualifications: List<UserQualification>,

  @get:JsonProperty("cruManagementAreaOverrideId") val cruManagementAreaOverrideId: UUID? = null,
)
