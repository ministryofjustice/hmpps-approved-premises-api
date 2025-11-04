package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param roles
 * @param qualifications
 */
data class UserRolesAndQualifications(

  val roles: kotlin.collections.List<ApprovedPremisesUserRole>,

  val qualifications: kotlin.collections.List<UserQualification>,
)
