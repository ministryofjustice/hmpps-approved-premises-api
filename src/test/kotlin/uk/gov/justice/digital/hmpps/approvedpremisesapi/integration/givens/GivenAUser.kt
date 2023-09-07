package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulStaffUserDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.util.UUID

fun IntegrationTestBase.`Given a User`(
  id: UUID = UUID.randomUUID(),
  staffUserDetailsConfigBlock: (StaffUserDetailsFactory.() -> Unit)? = null,
  roles: List<UserRole> = emptyList(),
  qualifications: List<UserQualification> = emptyList(),
  probationRegion: ProbationRegionEntity? = null,
  isActive: Boolean = true,
  block: (userEntity: UserEntity, jwt: String) -> Unit,
) {
  val staffUserDetailsFactory = StaffUserDetailsFactory()

  if (staffUserDetailsConfigBlock != null) {
    staffUserDetailsConfigBlock(staffUserDetailsFactory)
  }

  val staffUserDetails = staffUserDetailsFactory.produce()

  val user = userEntityFactory.produceAndPersist {
    withId(id)
    withDeliusUsername(staffUserDetails.username)
    withEmail(staffUserDetails.email)
    withTelephoneNumber(staffUserDetails.telephoneNumber)
    withName("${staffUserDetails.staff.forenames} ${staffUserDetails.staff.surname}")
    withIsActive(isActive)
    if (probationRegion == null) {
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
    } else {
      withProbationRegion(probationRegion)
    }
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
    staffUserDetails,
  )

  block(user, jwt)
}
