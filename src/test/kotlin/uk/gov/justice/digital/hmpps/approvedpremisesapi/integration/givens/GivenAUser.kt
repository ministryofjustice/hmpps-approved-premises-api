package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulStaffUserDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole

fun IntegrationTestBase.`Given a User`(
  staffUserDetailsConfigBlock: (StaffUserDetailsFactory.() -> Unit)? = null,
  roles: List<UserRole> = emptyList(),
  qualifications: List<UserQualification> = emptyList(),
  block: (userEntity: UserEntity, jwt: String) -> Unit
) {
  val staffUserDetailsFactory = StaffUserDetailsFactory()

  if (staffUserDetailsConfigBlock != null) {
    staffUserDetailsConfigBlock(staffUserDetailsFactory)
  }

  val staffUserDetails = staffUserDetailsFactory.produce()

  val user = userEntityFactory.produceAndPersist {
    withDeliusUsername(staffUserDetails.username)
  }

  roles.forEach { role ->
    user.roles += userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(user)
      withRole(role)
    }
  }

  qualifications.forEach { qualification ->
    user.qualifications += userQualificationAssignmentEntityFactory.produceAndPersist {
      withUser(user)
      withQualification(qualification)
    }
  }

  val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(staffUserDetails.username)

  CommunityAPI_mockSuccessfulStaffUserDetailsCall(
    staffUserDetails
  )

  block(user, jwt)
}
