package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddStaffDetailResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.communityAPIMockSuccessfulStaffUserDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.nomisUserRolesMockSuccessfulGetUserDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffNames
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import java.util.UUID

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.givenAUser(
  id: UUID = UUID.randomUUID(),
  staffDetail: StaffDetail = StaffDetailFactory.staffDetail(),
  roles: List<UserRole> = emptyList(),
  qualifications: List<UserQualification> = emptyList(),
  probationRegion: ProbationRegionEntity? = null,
  isActive: Boolean = true,
  mockStaffUserDetailsCall: Boolean = true,
  cruManagementAreaEntity: Cas1CruManagementAreaEntity? = null,
): Pair<UserEntity, String> {
  val resolvedProbationRegion = probationRegion ?: probationRegionEntityFactory.produceAndPersist {
    withYieldedApArea { givenAnApArea() }
  }
  val apArea = resolvedProbationRegion.apArea!!

  val user = userEntityFactory.produceAndPersist {
    withId(id)
    withDeliusUsername(staffDetail.username)
    withDeliusStaffCode(staffDetail.code)
    withEmail(staffDetail.email)
    withTelephoneNumber(staffDetail.telephoneNumber)
    withName(staffDetail.name.deliusName())
    withIsActive(isActive)
    withYieldedProbationRegion { resolvedProbationRegion }
    withYieldedApArea { apArea }
    withCruManagementArea(cruManagementAreaEntity ?: apArea.defaultCruManagementArea)
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

  val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(staffDetail.username!!)

  if (mockStaffUserDetailsCall) {
    // this will be removed when the staff code call is added to the ap-delius-integration
    val temp = StaffUserDetails(
      username = staffDetail.username!!,
      email = staffDetail.email,
      telephoneNumber = staffDetail.telephoneNumber,
      staffCode = staffDetail.code,
      staffIdentifier = staffDetail.staffIdentifier,
      staff = StaffNames(
        forenames = staffDetail.name.forenames(),
        surname = staffDetail.name.surname,
      ),
      teams = staffDetail.teams.map { team ->
        StaffUserTeamMembershipFactory().withCode(team.code).withDescription(team.name).produce()
      },
      probationArea = StaffProbationArea(
        staffDetail.probationArea.code,
        staffDetail.probationArea.description,
      ),
    )
    communityAPIMockSuccessfulStaffUserDetailsCall(temp)
    apDeliusContextAddStaffDetailResponse(staffDetail)
  } else {
    mockOAuth2ClientCredentialsCallIfRequired {}
  }

  return Pair(user, jwt)
}

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.givenAUser(
  id: UUID = UUID.randomUUID(),
  staffDetail: StaffDetail = StaffDetailFactory.staffDetail(),
  roles: List<UserRole> = emptyList(),
  qualifications: List<UserQualification> = emptyList(),
  probationRegion: ProbationRegionEntity? = null,
  isActive: Boolean = true,
  mockStaffUserDetailsCall: Boolean = true,
  block: (userEntity: UserEntity, jwt: String) -> Unit,
) {
  val (user, jwt) = givenAUser(
    id,
    staffDetail,
    roles,
    qualifications,
    probationRegion,
    isActive,
    mockStaffUserDetailsCall = mockStaffUserDetailsCall,
  )

  return block(user, jwt)
}

fun IntegrationTestBase.givenACas2PomUser(
  id: UUID = UUID.randomUUID(),
  nomisUserDetailsConfigBlock: (NomisUserDetailFactory.() -> Unit)? = null,
  block: (nomisUserEntity: NomisUserEntity, jwt: String) -> Unit,
) {
  val nomisUserDetailsFactory = NomisUserDetailFactory()

  if (nomisUserDetailsConfigBlock != null) {
    nomisUserDetailsConfigBlock(nomisUserDetailsFactory)
  }

  val nomisUserDetails = nomisUserDetailsFactory.produce()

  val user = nomisUserEntityFactory.produceAndPersist {
    withId(id)
    withNomisUsername(nomisUserDetails.username)
    withEmail(nomisUserDetails.primaryEmail)
    withName("${nomisUserDetails.firstName} ${nomisUserDetails.lastName}")
    withActiveCaseloadId(nomisUserDetails.activeCaseloadId!!)
  }

  val jwt = jwtAuthHelper.createValidNomisAuthorisationCodeJwt(nomisUserDetails.username)

  nomisUserRolesMockSuccessfulGetUserDetailsCall(jwt, nomisUserDetails)

  block(user, jwt)
}

fun IntegrationTestBase.givenACas2LicenceCaseAdminUser(
  id: UUID = UUID.randomUUID(),
  nomisUserDetailsConfigBlock: (NomisUserDetailFactory.() -> Unit)? = null,
  block: (nomisUserEntity: NomisUserEntity, jwt: String) -> Unit,
) {
  val nomisUserDetailsFactory = NomisUserDetailFactory()

  if (nomisUserDetailsConfigBlock != null) {
    nomisUserDetailsConfigBlock(nomisUserDetailsFactory)
  }

  val nomisUserDetails = nomisUserDetailsFactory.produce()

  val user = nomisUserEntityFactory.produceAndPersist {
    withId(id)
    withNomisUsername(nomisUserDetails.username)
    withEmail(nomisUserDetails.primaryEmail)
    withName("${nomisUserDetails.firstName} ${nomisUserDetails.lastName}")
    withActiveCaseloadId(nomisUserDetails.activeCaseloadId!!)
  }

  val jwt = jwtAuthHelper.createValidNomisAuthorisationCodeJwt(nomisUserDetails.username, listOf("ROLE_LICENCE_CA"))

  nomisUserRolesMockSuccessfulGetUserDetailsCall(jwt, nomisUserDetails)

  block(user, jwt)
}

fun IntegrationTestBase.givenACas2Assessor(
  id: UUID = UUID.randomUUID(),
  block: (externalUserEntity: ExternalUserEntity, jwt: String) -> Unit,
) {
  val user = externalUserEntityFactory.produceAndPersist {
    withId(id)
    withUsername("CAS2_ASSESSOR_USER")
  }

  val jwt = jwtAuthHelper.createValidExternalAuthorisationCodeJwt("CAS2_ASSESSOR_USER")

  block(user, jwt)
}

fun IntegrationTestBase.givenACas2Admin(
  id: UUID = UUID.randomUUID(),
  nomisUserDetailsConfigBlock: (NomisUserDetailFactory.() -> Unit)? = null,
  block: (nomisUserEntity: NomisUserEntity, jwt: String) -> Unit,
) {
  val nomisUserDetailsFactory = NomisUserDetailFactory()

  if (nomisUserDetailsConfigBlock != null) {
    nomisUserDetailsConfigBlock(nomisUserDetailsFactory)
  }

  val nomisUserDetails = nomisUserDetailsFactory.produce()

  val user = nomisUserEntityFactory.produceAndPersist {
    withId(id)
    withNomisUsername(nomisUserDetails.username)
    withEmail(nomisUserDetails.primaryEmail)
    withName("${nomisUserDetails.firstName} ${nomisUserDetails.lastName}")
  }

  val jwt = jwtAuthHelper.createValidAdminAuthorisationCodeJwt(nomisUserDetails.username)

  nomisUserRolesMockSuccessfulGetUserDetailsCall(jwt, nomisUserDetails)

  block(user, jwt)
}
