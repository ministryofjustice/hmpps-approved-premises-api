package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserWithWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.UserWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as ApiUserQualification

@Component
class UserTransformer(
  private val probationRegionTransformer: ProbationRegionTransformer,
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
    )
  }

  fun transformJpaToApi(jpa: UserEntity, serviceName: ServiceName) = when (serviceName) {
    ServiceName.approvedPremises -> ApprovedPremisesUser(
      id = jpa.id,
      deliusUsername = jpa.deliusUsername,
      roles = jpa.roles.distinctBy { it.role }.mapNotNull(::transformApprovedPremisesRoleToApi),
      email = jpa.email,
      name = jpa.name,
      telephoneNumber = jpa.telephoneNumber,
      isActive = jpa.isActive,
      qualifications = jpa.qualifications.map(::transformQualificationToApi),
      region = probationRegionTransformer.transformJpaToApi(jpa.probationRegion),
      service = "CAS1",
      apArea = jpa.apArea?.let { apAreaTransformer.transformJpaToApi(it) } ?: throw InternalServerErrorProblem("CAS1 user ${jpa.id} should have AP Area Set"),
    )
    ServiceName.temporaryAccommodation -> TemporaryAccommodationUser(
      id = jpa.id,
      deliusUsername = jpa.deliusUsername,
      email = jpa.email,
      name = jpa.name,
      telephoneNumber = jpa.telephoneNumber,
      isActive = jpa.isActive,
      roles = jpa.roles.distinctBy { it.role }.mapNotNull(::transformTemporaryAccommodationRoleToApi),
      region = probationRegionTransformer.transformJpaToApi(jpa.probationRegion),
      service = ServiceName.temporaryAccommodation.value,
    )
    ServiceName.cas2 -> throw RuntimeException("CAS2 not supported")
  }

  private fun transformApprovedPremisesRoleToApi(userRole: UserRoleAssignmentEntity): ApprovedPremisesUserRole? = when (userRole.role) {
    UserRole.CAS1_ADMIN -> ApprovedPremisesUserRole.roleAdmin
    UserRole.CAS1_ASSESSOR -> ApprovedPremisesUserRole.assessor
    UserRole.CAS1_MATCHER -> ApprovedPremisesUserRole.matcher
    UserRole.CAS1_MANAGER -> ApprovedPremisesUserRole.manager
    UserRole.CAS1_WORKFLOW_MANAGER -> ApprovedPremisesUserRole.workflowManager
    UserRole.CAS1_APPLICANT -> ApprovedPremisesUserRole.applicant
    UserRole.CAS1_EXCLUDED_FROM_MATCH_ALLOCATION -> ApprovedPremisesUserRole.excludedFromMatchAllocation
    UserRole.CAS1_EXCLUDED_FROM_ASSESS_ALLOCATION -> ApprovedPremisesUserRole.excludedFromAssessAllocation
    UserRole.CAS1_EXCLUDED_FROM_PLACEMENT_APPLICATION_ALLOCATION -> ApprovedPremisesUserRole.excludedFromPlacementApplicationAllocation
    UserRole.CAS1_REPORT_VIEWER -> ApprovedPremisesUserRole.reportViewer
    UserRole.CAS1_APPEALS_MANAGER -> ApprovedPremisesUserRole.appealsManager
    UserRole.CAS3_ASSESSOR, UserRole.CAS3_REFERRER, UserRole.CAS3_REPORTER -> null
  }

  private fun transformTemporaryAccommodationRoleToApi(userRole: UserRoleAssignmentEntity): TemporaryAccommodationUserRole? = when (userRole.role) {
    UserRole.CAS3_ASSESSOR -> TemporaryAccommodationUserRole.assessor
    UserRole.CAS3_REFERRER -> TemporaryAccommodationUserRole.referrer
    UserRole.CAS3_REPORTER -> TemporaryAccommodationUserRole.reporter
    else -> null
  }

  private fun transformQualificationToApi(userQualification: UserQualificationAssignmentEntity): ApiUserQualification = when (userQualification.qualification) {
    UserQualification.PIPE -> ApiUserQualification.pipe
    UserQualification.WOMENS -> ApiUserQualification.womens
    UserQualification.LAO -> ApiUserQualification.lao
    UserQualification.ESAP -> ApiUserQualification.esap
    UserQualification.EMERGENCY -> ApiUserQualification.emergency
  }
}
