package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class UserRolesAndQualifications(

  val roles: kotlin.collections.List<ApprovedPremisesUserRole>,

  val qualifications: kotlin.collections.List<UserQualification>,
)
