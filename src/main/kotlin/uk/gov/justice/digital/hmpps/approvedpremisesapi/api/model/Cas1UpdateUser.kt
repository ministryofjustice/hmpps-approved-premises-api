package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1UpdateUser(

  val roles: kotlin.collections.List<ApprovedPremisesUserRole>,

  val qualifications: kotlin.collections.List<UserQualification>,

  val cruManagementAreaOverrideId: java.util.UUID? = null,
)
