package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration.Cas2v2IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddStaffDetailResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.nomisUserRolesMockSuccessfulGetMeCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
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
  cruManagementArea: Cas1CruManagementAreaEntity? = null,
  cruManagementAreaOverride: Cas1CruManagementAreaEntity? = null,
): Pair<UserEntity, String> {
  val resolvedProbationRegion = probationRegion ?: probationRegionEntityFactory.produceAndPersist {
    withYieldedApArea { givenAnApArea() }
  }
  val apArea = resolvedProbationRegion.apArea!!

  val pdu = probationDeliveryUnitFactory.produceAndPersist {
    withProbationRegion(resolvedProbationRegion)
  }

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
    withCruManagementArea(cruManagementArea ?: apArea.defaultCruManagementArea)
    withCruManagementAreaOverride(cruManagementAreaOverride)
    withProbationDeliveryUnit { pdu }
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
  mockCallToGetMe: Boolean = true,
  id: UUID = UUID.randomUUID(),
  nomisUserDetailsConfigBlock: (NomisUserDetailFactory.() -> Unit)? = null,
  block: (cas2UserEntity: Cas2UserEntity, jwt: String) -> Unit,
) {
  val nomisUserDetailsFactory = NomisUserDetailFactory()

  if (nomisUserDetailsConfigBlock != null) {
    nomisUserDetailsConfigBlock(nomisUserDetailsFactory)
  }

  val nomisUserDetails = nomisUserDetailsFactory.produce()

  val user = cas2UserEntityFactory.produceAndPersist {
    withId(id)
    withUsername(nomisUserDetails.username)
    withEmail(nomisUserDetails.primaryEmail)
    withName("${nomisUserDetails.firstName} ${nomisUserDetails.lastName}")
    withActiveNomisCaseloadId(nomisUserDetails.activeCaseloadId!!)
  }

  val jwt = jwtAuthHelper.createValidNomisAuthorisationCodeJwt(nomisUserDetails.username)

  if (mockCallToGetMe) {
    nomisUserRolesMockSuccessfulGetMeCall(jwt, nomisUserDetails)
  }

  block(user, jwt)
}

@SuppressWarnings("LongParameterList")
fun Cas2v2IntegrationTestBase.givenACas2v2DeliusUser(
  id: UUID = UUID.randomUUID(),
  staffDetail: StaffDetail = StaffDetailFactory.staffDetail(),
  roles: List<UserRole> = emptyList(),
  qualifications: List<UserQualification> = emptyList(),
  probationRegion: ProbationRegionEntity? = null,
  isActive: Boolean = true,
  mockStaffUserDetailsCall: Boolean = true,
  block: (userEntity: Cas2UserEntity, jwt: String) -> Unit,
) {
  val (deliusUser, _) = givenAUser(
    id,
    staffDetail,
    roles,
    qualifications,
    probationRegion,
    isActive,
    mockStaffUserDetailsCall = mockStaffUserDetailsCall,
  )
//  val nomisUserDetailsFactory = NomisUserDetailFactory()

  val user = cas2UserEntityFactory.produceAndPersist {
    withId(id)
    withUsername(deliusUser.deliusUsername)
    withEmail(deliusUser.email)
    withName(deliusUser.name)
    withUserType(Cas2UserType.DELIUS)
    withServiceOrigin(Cas2ServiceOrigin.BAIL)
  }

  val jwt = jwtAuthHelper.createValidDeliusAuthorisationCodeJwt(deliusUser.deliusUsername)

  block(user, jwt)
}

fun Cas2v2IntegrationTestBase.givenACas2v2PomUser(
  id: UUID = UUID.randomUUID(),
  nomisUserDetailsConfigBlock: (NomisUserDetailFactory.() -> Unit)? = null,
  block: (userEntity: Cas2UserEntity, jwt: String) -> Unit,
) {
  val nomisUserDetailsFactory = NomisUserDetailFactory()

  if (nomisUserDetailsConfigBlock != null) {
    nomisUserDetailsConfigBlock(nomisUserDetailsFactory)
  }

  val nomisUserDetails = nomisUserDetailsFactory.produce()

  val user = cas2UserEntityFactory.produceAndPersist {
    withId(id)
    withUsername(nomisUserDetails.username)
    withEmail(nomisUserDetails.primaryEmail)
    withName("${nomisUserDetails.firstName} ${nomisUserDetails.lastName}")
    withActiveNomisCaseloadId(nomisUserDetails.activeCaseloadId!!)
    withServiceOrigin(Cas2ServiceOrigin.BAIL)
  }

  val jwt = jwtAuthHelper.createValidCas2v2NomisAuthorisationCodeJwt(nomisUserDetails.username)

  nomisUserRolesMockSuccessfulGetMeCall(jwt, nomisUserDetails)

  block(user, jwt)
}

fun Cas2v2IntegrationTestBase.givenACas2v2NomisUser(
  id: UUID = UUID.randomUUID(),
  nomisUserDetailsConfigBlock: (NomisUserDetailFactory.() -> Unit)? = null,
  block: (userEntity: Cas2UserEntity, jwt: String) -> Unit,
) {
  val nomisUserDetailsFactory = NomisUserDetailFactory()

  if (nomisUserDetailsConfigBlock != null) {
    nomisUserDetailsConfigBlock(nomisUserDetailsFactory)
  }

  val nomisUserDetails = nomisUserDetailsFactory.produce()

  val user = cas2UserEntityFactory.produceAndPersist {
    withId(id)
    withUsername(nomisUserDetails.username)
    withEmail(nomisUserDetails.primaryEmail)
    withName("${nomisUserDetails.firstName} ${nomisUserDetails.lastName}")
    withActiveNomisCaseloadId(nomisUserDetails.activeCaseloadId!!)
    withServiceOrigin(Cas2ServiceOrigin.BAIL)
  }

  val jwt = jwtAuthHelper.createValidCas2v2NomisAuthorisationCodeJwt(nomisUserDetails.username)

  nomisUserRolesMockSuccessfulGetMeCall(jwt, nomisUserDetails)

  block(user, jwt)
}

fun IntegrationTestBase.givenACas2LicenceCaseAdminUser(
  id: UUID = UUID.randomUUID(),
  nomisUserDetailsConfigBlock: (NomisUserDetailFactory.() -> Unit)? = null,
  block: (cas2UserEntity: Cas2UserEntity, jwt: String) -> Unit,
) {
  val nomisUserDetailsFactory = NomisUserDetailFactory()

  if (nomisUserDetailsConfigBlock != null) {
    nomisUserDetailsConfigBlock(nomisUserDetailsFactory)
  }

  val nomisUserDetails = nomisUserDetailsFactory.produce()

  val user = cas2UserEntityFactory.produceAndPersist {
    withId(id)
    withUsername(nomisUserDetails.username)
    withEmail(nomisUserDetails.primaryEmail)
    withName("${nomisUserDetails.firstName} ${nomisUserDetails.lastName}")
    withActiveNomisCaseloadId(nomisUserDetails.activeCaseloadId!!)
  }

  val jwt = jwtAuthHelper.createValidNomisAuthorisationCodeJwt(nomisUserDetails.username, listOf("ROLE_LICENCE_CA"))

  nomisUserRolesMockSuccessfulGetMeCall(jwt, nomisUserDetails)

  block(user, jwt)
}

fun IntegrationTestBase.givenACas2Assessor(
  id: UUID = UUID.randomUUID(),
  block: (externalUserEntity: Cas2UserEntity, jwt: String) -> Unit,
) {
  val user = cas2UserEntityFactory.produceAndPersist {
    withUserType(Cas2UserType.EXTERNAL)
    withId(id)
    withUsername("CAS2_ASSESSOR_USER")
  }

  val jwt = jwtAuthHelper.createValidExternalAuthorisationCodeJwt("CAS2_ASSESSOR_USER")

  block(user, jwt)
}

fun Cas2v2IntegrationTestBase.givenACas2v2Assessor(
  id: UUID = UUID.randomUUID(),
  block: (userEntity: Cas2UserEntity, jwt: String) -> Unit,
) {
  val user = cas2UserEntityFactory.produceAndPersist {
    withId(id)
    withUsername("CAS2_ASSESSOR_USER")
    withUserType(Cas2UserType.EXTERNAL)
    withServiceOrigin(Cas2ServiceOrigin.BAIL)
  }

  val jwt = jwtAuthHelper.createValidExternalAuthorisationCodeJwt("CAS2_ASSESSOR_USER")

  block(user, jwt)
}

fun IntegrationTestBase.givenACas2Admin(
  id: UUID = UUID.randomUUID(),
  nomisUserDetailsConfigBlock: (NomisUserDetailFactory.() -> Unit)? = null,
  block: (nomisUserEntity: Cas2UserEntity, jwt: String) -> Unit,
) {
  val nomisUserDetailsFactory = NomisUserDetailFactory()

  if (nomisUserDetailsConfigBlock != null) {
    nomisUserDetailsConfigBlock(nomisUserDetailsFactory)
  }

  val nomisUserDetails = nomisUserDetailsFactory.produce()

  val user = cas2UserEntityFactory.produceAndPersist {
    withId(id)
    withUsername(nomisUserDetails.username)
    withEmail(nomisUserDetails.primaryEmail)
    withName("${nomisUserDetails.firstName} ${nomisUserDetails.lastName}")
  }

  val jwt = jwtAuthHelper.createValidAdminAuthorisationCodeJwt(nomisUserDetails.username)

  nomisUserRolesMockSuccessfulGetMeCall(jwt, nomisUserDetails)

  block(user, jwt)
}
