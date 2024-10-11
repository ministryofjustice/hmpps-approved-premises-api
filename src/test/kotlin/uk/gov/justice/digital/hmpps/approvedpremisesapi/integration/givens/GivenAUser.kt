package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ApDeliusContext_addStaffDetailResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulStaffUserDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.NomisUserRoles_mockSuccessfulGetUserDetailsCall
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
import java.util.UUID

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.`Given a User`(
  id: UUID = UUID.randomUUID(),
  staffUserDetailsConfigBlock: (StaffDetailFactory.() -> Unit)? = null,
  roles: List<UserRole> = emptyList(),
  qualifications: List<UserQualification> = emptyList(),
  probationRegion: ProbationRegionEntity? = null,
  isActive: Boolean = true,
  mockStaffUserDetailsCall: Boolean = true,
  cruManagementAreaEntity: Cas1CruManagementAreaEntity? = null,
): Pair<UserEntity, String> {
  val staffUserDetailsFactory = StaffDetailFactory

  if (staffUserDetailsConfigBlock != null) {
    staffUserDetailsConfigBlock(staffUserDetailsFactory)
  }

  val staffUserDetails = staffUserDetailsFactory.staffDetail()

  val resolvedProbationRegion = probationRegion ?: probationRegionEntityFactory.produceAndPersist {
    withYieldedApArea { `Given an AP Area`() }
  }
  val apArea = resolvedProbationRegion.apArea!!

  val user = userEntityFactory.produceAndPersist {
    withId(id)
    withDeliusUsername(staffUserDetails.username)
    withDeliusStaffCode(staffUserDetails.code)
    withEmail(staffUserDetails.email)
    withTelephoneNumber(staffUserDetails.telephoneNumber)
    withName(staffUserDetails.name.deliusName())
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

  val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(staffUserDetails.username)

  if (mockStaffUserDetailsCall) {
    val temp = StaffUserDetails(
      username = staffUserDetails.username,
      email = staffUserDetails.email,
      telephoneNumber = staffUserDetails.telephoneNumber,
      staffCode = staffUserDetails.code,
      staffIdentifier = staffUserDetails.staffIdentifier,
      staff = StaffNames(
        forenames = staffUserDetails.name.forenames(),
        surname = staffUserDetails.name.surname,
      ),
      teams = staffUserDetails.teams.map { x ->
        StaffUserTeamMembershipFactory().withCode(x.code).withDescription(x.name).produce()
      },
      probationArea = StaffProbationArea(
        staffUserDetails.probationArea.code,
        staffUserDetails.probationArea.description,
      ),
    )
    CommunityAPI_mockSuccessfulStaffUserDetailsCall(temp)
    ApDeliusContext_addStaffDetailResponse(staffUserDetails)
  } else {
    mockOAuth2ClientCredentialsCallIfRequired {}
  }

  return Pair(user, jwt)
}

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.`Given a User`(
  id: UUID = UUID.randomUUID(),
  staffUserDetailsConfigBlock: (StaffDetailFactory.() -> Unit)? = null,
  roles: List<UserRole> = emptyList(),
  qualifications: List<UserQualification> = emptyList(),
  probationRegion: ProbationRegionEntity? = null,
  isActive: Boolean = true,
  mockStaffUserDetailsCall: Boolean = true,
  block: (userEntity: UserEntity, jwt: String) -> Unit,
) {
  val (user, jwt) = `Given a User`(
    id,
    staffUserDetailsConfigBlock,
    roles,
    qualifications,
    probationRegion,
    isActive,
    mockStaffUserDetailsCall = mockStaffUserDetailsCall,
  )

  return block(user, jwt)
}

fun IntegrationTestBase.`Given a CAS2 POM User`(
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

  NomisUserRoles_mockSuccessfulGetUserDetailsCall(jwt, nomisUserDetails)

  block(user, jwt)
}

fun IntegrationTestBase.`Given a CAS2 Licence Case Admin User`(
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

  NomisUserRoles_mockSuccessfulGetUserDetailsCall(jwt, nomisUserDetails)

  block(user, jwt)
}

fun IntegrationTestBase.`Given a CAS2 Assessor`(
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

fun IntegrationTestBase.`Given a CAS2 Admin`(
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

  NomisUserRoles_mockSuccessfulGetUserDetailsCall(jwt, nomisUserDetails)

  block(user, jwt)
}
