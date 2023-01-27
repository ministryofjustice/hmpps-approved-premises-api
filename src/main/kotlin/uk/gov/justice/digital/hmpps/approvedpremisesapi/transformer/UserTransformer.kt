package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as ApiUserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserRole as ApiUserRole

@Component
class UserTransformer(
  private val probationRegionTransformer: ProbationRegionTransformer,
) {
  fun transformJpaToApi(jpa: UserEntity, serviceName: ServiceName) = when (serviceName) {
    ServiceName.approvedPremises -> ApprovedPremisesUser(
      id = jpa.id,
      deliusUsername = jpa.deliusUsername,
      roles = jpa.roles.map(::transformRoleToApi),
      email = jpa.email,
      name = jpa.name,
      telephoneNumber = jpa.telephoneNumber,
      qualifications = jpa.qualifications.map(::transformQualificationToApi),
      region = probationRegionTransformer.transformJpaToApi(jpa.probationRegion),
      service = ServiceName.approvedPremises.value,
    )
    ServiceName.temporaryAccommodation -> TemporaryAccommodationUser(
      id = jpa.id,
      roles = jpa.roles.map(::transformRoleToApi),
      region = probationRegionTransformer.transformJpaToApi(jpa.probationRegion),
      service = ServiceName.temporaryAccommodation.value,
    )
  }

  private fun transformRoleToApi(userRole: UserRoleAssignmentEntity): ApiUserRole = when (userRole.role) {
    UserRole.ADMIN -> ApiUserRole.admin
    UserRole.ASSESSOR -> ApiUserRole.assessor
    UserRole.MATCHER -> ApiUserRole.matcher
    UserRole.MANAGER -> ApiUserRole.manager
    UserRole.WORKFLOW_MANAGER -> ApiUserRole.workflowManager
    UserRole.APPLICANT -> ApiUserRole.applicant
  }

  private fun transformQualificationToApi(userQualification: UserQualificationAssignmentEntity): ApiUserQualification = when (userQualification.qualification) {
    UserQualification.PIPE -> ApiUserQualification.pipe
    UserQualification.WOMENS -> ApiUserQualification.womens
  }
}
