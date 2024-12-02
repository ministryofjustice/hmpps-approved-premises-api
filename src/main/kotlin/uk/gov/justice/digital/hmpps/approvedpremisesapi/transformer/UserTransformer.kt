package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProfileResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserWithWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.UserWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission as ApiUserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as ApiUserQualification

@Component
class UserTransformer(
  private val probationRegionTransformer: ProbationRegionTransformer,
  private val probationDeliveryUnitTransformer: ProbationDeliveryUnitTransformer,
  private val apAreaTransformer: ApAreaTransformer,
) {

  fun transformJpaToAPIUserWithWorkload(jpa: UserEntity, userWorkload: UserWorkload): UserWithWorkload {
    return UserWithWorkload(
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
      roles = jpa.roles.distinctBy { it.role }.mapNotNull(::transformApprovedPremisesRoleToApi),
      apArea = jpa.apArea?.let { apAreaTransformer.transformJpaToApi(it) },
      cruManagementArea = jpa.cruManagementArea?.toNamedId(),
    )
  }

  fun transformJpaToSummaryApi(jpa: UserEntity) =
    UserSummary(
      id = jpa.id,
      name = jpa.name,
    )

  fun transformJpaToApi(jpa: UserEntity, serviceName: ServiceName) = when (serviceName) {
    ServiceName.approvedPremises -> transformCas1JpaToApi(jpa)
    ServiceName.temporaryAccommodation -> transformCas3JpatoApi(jpa)
    ServiceName.cas2 -> throw RuntimeException("CAS2 not supported")
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
      version = UserEntity.getVersionHashCode((jpa.roles.map { it.role })),
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

  fun transformProfileResponseToApi(userName: String, userResponse: UserService.GetUserResponse, xServiceName: ServiceName): ProfileResponse {
    return when (userResponse) {
      UserService.GetUserResponse.StaffRecordNotFound -> ProfileResponse(userName, ProfileResponse.LoadError.STAFF_RECORD_NOT_FOUND)
      is UserService.GetUserResponse.Success -> ProfileResponse(userName, user = transformJpaToApi(userResponse.user, xServiceName))
    }
  }

  private fun transformApprovedPremisesRoleToApi(userRole: UserRoleAssignmentEntity): ApprovedPremisesUserRole? =
    when (userRole.role.service) {
      ServiceName.approvedPremises -> userRole.role.cas1ApiValue!!
      else -> null
    }

  private fun transformTemporaryAccommodationRoleToApi(userRole: UserRoleAssignmentEntity): TemporaryAccommodationUserRole? = when (userRole.role) {
    UserRole.CAS3_ASSESSOR -> TemporaryAccommodationUserRole.ASSESSOR
    UserRole.CAS3_REFERRER -> TemporaryAccommodationUserRole.REFERRER
    UserRole.CAS3_REPORTER -> TemporaryAccommodationUserRole.REPORTER
    else -> null
  }

  private fun transformQualificationToApi(userQualification: UserQualificationAssignmentEntity): ApiUserQualification = when (userQualification.qualification) {
    UserQualification.PIPE -> ApiUserQualification.PIPE
    UserQualification.LAO -> ApiUserQualification.LAO
    UserQualification.ESAP -> ApiUserQualification.ESAP
    UserQualification.EMERGENCY -> ApiUserQualification.EMERGENCY
    UserQualification.MENTAL_HEALTH_SPECIALIST -> ApiUserQualification.MENTAL_HEALTH_SPECIALIST
    UserQualification.RECOVERY_FOCUSED -> ApiUserQualification.RECOVERY_FOCUSED
  }

  @SuppressWarnings("CyclomaticComplexMethod")
  private fun transformApprovedPremisesRoleToPermissionApi(userRole: UserRoleAssignmentEntity): List<ApiUserPermission> {
    return userRole.role.permissions.map {
      when (it) {
        UserPermission.CAS1_ADHOC_BOOKING_CREATE -> ApiUserPermission.ADHOC_BOOKING_CREATE
        UserPermission.CAS1_ASSESS_APPEALED_APPLICATION -> ApiUserPermission.ASSESS_APPEALED_APPLICATION
        UserPermission.CAS1_ASSESS_APPLICATION -> ApiUserPermission.ASSESS_APPLICATION
        UserPermission.CAS1_ASSESS_PLACEMENT_APPLICATION -> ApiUserPermission.ASSESS_PLACEMENT_APPLICATION
        UserPermission.CAS1_ASSESS_PLACEMENT_REQUEST -> ApiUserPermission.ASSESS_PLACEMENT_REQUEST
        UserPermission.CAS1_BOOKING_CREATE -> ApiUserPermission.BOOKING_CREATE
        UserPermission.CAS1_BOOKING_CHANGE_DATES -> ApiUserPermission.BOOKING_CHANGE_DATES
        UserPermission.CAS1_BOOKING_WITHDRAW -> ApiUserPermission.BOOKING_WITHDRAW
        UserPermission.CAS1_OUT_OF_SERVICE_BED_CREATE -> ApiUserPermission.OUT_OF_SERVICE_BED_CREATE
        UserPermission.CAS1_PROCESS_AN_APPEAL -> ApiUserPermission.PROCESS_AN_APPEAL
        UserPermission.CAS1_USER_LIST -> ApiUserPermission.USER_LIST
        UserPermission.CAS1_USER_MANAGEMENT -> ApiUserPermission.USER_MANAGEMENT
        UserPermission.CAS1_VIEW_ASSIGNED_ASSESSMENTS -> ApiUserPermission.VIEW_ASSIGNED_ASSESSMENTS
        UserPermission.CAS1_VIEW_CRU_DASHBOARD -> ApiUserPermission.VIEW_CRU_DASHBOARD
        UserPermission.CAS1_VIEW_MANAGE_TASKS -> ApiUserPermission.VIEW_MANAGE_TASKS
        UserPermission.CAS1_VIEW_OUT_OF_SERVICE_BEDS -> ApiUserPermission.VIEW_OUT_OF_SERVICE_BEDS
        UserPermission.CAS1_SPACE_BOOKING_CREATE -> ApiUserPermission.SPACE_BOOKING_CREATE
        UserPermission.CAS1_SPACE_BOOKING_LIST -> ApiUserPermission.SPACE_BOOKING_LIST
        UserPermission.CAS1_SPACE_BOOKING_RECORD_ARRIVAL -> ApiUserPermission.SPACE_BOOKING_RECORD_ARRIVAL
        UserPermission.CAS1_SPACE_BOOKING_RECORD_DEPARTURE -> ApiUserPermission.SPACE_BOOKING_RECORD_DEPARTURE
        UserPermission.CAS1_SPACE_BOOKING_RECORD_NON_ARRIVAL -> ApiUserPermission.SPACE_BOOKING_RECORD_NON_ARRIVAL
        UserPermission.CAS1_SPACE_BOOKING_RECORD_KEYWORKER -> ApiUserPermission.SPACE_BOOKING_RECORD_KEYWORKER
        UserPermission.CAS1_SPACE_BOOKING_VIEW -> ApiUserPermission.SPACE_BOOKING_VIEW
        UserPermission.CAS1_SPACE_BOOKING_WITHDRAW -> ApiUserPermission.SPACE_BOOKING_WITHDRAW
        UserPermission.CAS1_PREMISES_VIEW_CAPACITY -> ApiUserPermission.PREMISES_VIEW_CAPACITY
        UserPermission.CAS1_PREMISES_VIEW_SUMMARY -> ApiUserPermission.PREMISES_VIEW_SUMMARY
        UserPermission.CAS1_APPLICATION_WITHDRAW_OTHERS -> ApiUserPermission.APPLICATION_WITHDRAW_OTHERS
        UserPermission.CAS1_REQUEST_FOR_PLACEMENT_WITHDRAW_OTHERS -> ApiUserPermission.REQUEST_FOR_PLACEMENT_WITHDRAW_OTHERS
        UserPermission.CAS1_REPORTS_VIEW -> ApiUserPermission.REPORTS_VIEW
      }
    }
  }
}
