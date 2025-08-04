package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProfileResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserWithWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.UserWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission as ApiUserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as ApiUserQualification

@Component
class UserTransformer(
  private val probationRegionTransformer: ProbationRegionTransformer,
  private val probationDeliveryUnitTransformer: ProbationDeliveryUnitTransformer,
  private val apAreaTransformer: ApAreaTransformer,
  private val environmentService: EnvironmentService,
) {

  fun transformJpaToAPIUserWithWorkload(jpa: UserEntity, userWorkload: UserWorkload): UserWithWorkload = UserWithWorkload(
    id = jpa.id,
    deliusUsername = jpa.deliusUsername,
    email = jpa.email,
    name = jpa.name,
    telephoneNumber = jpa.telephoneNumber,
    isActive = jpa.isActive,
    region = probationRegionTransformer.transformJpaToApi(jpa.probationRegion),
    service = ServiceName.approvedPremises.value,
    numTasksPending = userWorkload.numTasksPending,
    numTasksCompleted7Days = userWorkload.numTasksCompleted7Days,
    numTasksCompleted30Days = userWorkload.numTasksCompleted30Days,
    qualifications = jpa.qualifications.distinctBy { it.qualification }.map(::transformQualificationToApi),
    apArea = jpa.apArea?.let { apAreaTransformer.transformJpaToApi(it) },
    cruManagementArea = jpa.cruManagementArea?.toNamedId(),
  )

  fun transformJpaToSummaryApi(jpa: UserEntity) = UserSummary(
    id = jpa.id,
    name = jpa.name,
  )

  @Suppress("TooGenericExceptionThrown")
  fun transformJpaToApi(jpa: UserEntity, serviceName: ServiceName) = when (serviceName) {
    ServiceName.approvedPremises -> transformCas1JpaToApi(jpa)
    ServiceName.temporaryAccommodation -> transformCas3JpatoApi(jpa)
    ServiceName.cas2 -> throw RuntimeException("CAS2 not supported")
    ServiceName.cas2v2 -> throw RuntimeException("CAS2v2 not supported")
  }

  fun transformCas1JpaToApi(jpa: UserEntity): ApprovedPremisesUser {
    val apArea = jpa.apArea ?: throw InternalServerErrorProblem("CAS1 user ${jpa.id} should have AP Area Set")
    val cruManagementArea = jpa.cruManagementArea ?: throw InternalServerErrorProblem("CAS1 user ${jpa.id} should have CRU Management Area Set")
    return ApprovedPremisesUser(
      id = jpa.id,
      deliusUsername = jpa.deliusUsername,
      roles = jpa.roles.distinctBy { it.role }.mapNotNull(::transformApprovedPremisesRoleToApi),
      email = jpa.email,
      name = jpa.name,
      telephoneNumber = jpa.telephoneNumber,
      isActive = jpa.isActive,
      qualifications = jpa.qualifications.map(::transformQualificationToApi),
      permissions = jpa.roles.distinctBy { it.role }.map(::transformApprovedPremisesRoleToPermissionApi).flatten().distinct(),
      region = probationRegionTransformer.transformJpaToApi(jpa.probationRegion),
      service = "CAS1",
      apArea = apArea.let { apAreaTransformer.transformJpaToApi(it) },
      cruManagementArea = cruManagementArea.toNamedId(),
      cruManagementAreaDefault = apArea.defaultCruManagementArea.toNamedId(),
      cruManagementAreaOverride = jpa.cruManagementAreaOverride?.toNamedId(),
      version = UserEntity.getVersionHashCode((jpa.roles.map { it.role }), environmentService),
    )
  }

  fun transformCas3JpatoApi(jpa: UserEntity) = TemporaryAccommodationUser(
    id = jpa.id,
    deliusUsername = jpa.deliusUsername,
    email = jpa.email,
    name = jpa.name,
    telephoneNumber = jpa.telephoneNumber,
    isActive = jpa.isActive,
    roles = jpa.roles.distinctBy { it.role }.mapNotNull(::transformTemporaryAccommodationRoleToApi),
    region = probationRegionTransformer.transformJpaToApi(jpa.probationRegion),
    probationDeliveryUnit = jpa.probationDeliveryUnit?.let { probationDeliveryUnitTransformer.transformJpaToApi(it) },
    service = "CAS3",
  )

  fun Cas1CruManagementAreaEntity.toNamedId() = NamedId(id, name)

  @SuppressWarnings("TooGenericExceptionThrown")
  fun transformProfileResponseToApi(userName: String, userResponse: UserService.GetUserResponse, xServiceName: ServiceName): ProfileResponse = when (userResponse) {
    UserService.GetUserResponse.StaffRecordNotFound -> ProfileResponse(userName, ProfileResponse.LoadError.staffRecordNotFound)
    is UserService.GetUserResponse.StaffProbationRegionNotSupported -> throw RuntimeException("Probation region '${userResponse.unsupportedRegionId}' not supported for user '$userResponse'")
    is UserService.GetUserResponse.Success -> ProfileResponse(userName, user = transformJpaToApi(userResponse.user, xServiceName))
  }
  private fun transformApprovedPremisesRoleToApi(userRole: UserRoleAssignmentEntity): ApprovedPremisesUserRole? = when (userRole.role.service) {
    ServiceName.approvedPremises -> userRole.role.cas1ApiValue!!
    else -> null
  }

  private fun transformTemporaryAccommodationRoleToApi(userRole: UserRoleAssignmentEntity): TemporaryAccommodationUserRole? = when (userRole.role) {
    UserRole.CAS3_ASSESSOR -> TemporaryAccommodationUserRole.assessor
    UserRole.CAS3_REFERRER -> TemporaryAccommodationUserRole.referrer
    UserRole.CAS3_REPORTER -> TemporaryAccommodationUserRole.reporter
    else -> null
  }

  private fun transformQualificationToApi(userQualification: UserQualificationAssignmentEntity): ApiUserQualification = when (userQualification.qualification) {
    UserQualification.PIPE -> ApiUserQualification.pipe
    UserQualification.LAO -> ApiUserQualification.lao
    UserQualification.ESAP -> ApiUserQualification.esap
    UserQualification.EMERGENCY -> ApiUserQualification.emergency
    UserQualification.MENTAL_HEALTH_SPECIALIST -> ApiUserQualification.mentalHealthSpecialist
    UserQualification.RECOVERY_FOCUSED -> ApiUserQualification.recoveryFocused
  }

  @SuppressWarnings("CyclomaticComplexMethod")
  private fun transformApprovedPremisesRoleToPermissionApi(userRole: UserRoleAssignmentEntity): List<ApiUserPermission> = userRole.role.permissions
    .filter { it.isAvailable(environmentService) }
    .map { it.cas1ApiValue as ApiUserPermission }
}
