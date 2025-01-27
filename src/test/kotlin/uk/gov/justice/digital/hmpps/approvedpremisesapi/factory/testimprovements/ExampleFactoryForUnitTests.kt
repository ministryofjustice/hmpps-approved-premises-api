package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.testimprovements

import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CruManagementAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity

data class UserTestExample(
  val id: UUID = UUID.randomUUID(),
  val name: String = "TODO()",
  val deliusUsername: String = "",
  val deliusStaffCode: String = "",
  val email: String = "",
  val telephoneNumber: String = "",
  val isActive: Boolean = true,
  val applications: MutableList<ApplicationEntity> = mutableListOf(),
  val roles: MutableList<UserRoleAssignmentEntity> = mutableListOf(),
  val qualifications: MutableList<UserQualificationAssignmentEntity> = mutableListOf(),
  val probationRegion: ProbationRegionEntity = ProbationRegionEntityFactory().produce(),
  val probationDeliveryUnit: ProbationDeliveryUnitEntity = ProbationDeliveryUnitEntityFactory().withProbationRegion(
    probationRegion,
  ).produce(),
  val apArea: ApAreaEntity = ApAreaEntityFactory().produce(),
  val cruManagementArea: Cas1CruManagementAreaEntity? = Cas1CruManagementAreaEntityFactory().produce(),
  val cruManagementAreaOverride: Cas1CruManagementAreaEntity? = Cas1CruManagementAreaEntityFactory().produce(),
  val teamCodes: List<String> = listOf(),
  val createdAt: OffsetDateTime = OffsetDateTime.now(),
  val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) {
  fun toEntity(): UserEntity {
    return UserEntity(
      id = id,
      name = name,
      deliusUsername = deliusUsername,
      deliusStaffCode = deliusStaffCode,
      email = email,
      telephoneNumber = telephoneNumber,
      isActive = isActive,
      applications = applications,
      roles = roles,
      qualifications = qualifications,
      probationRegion = probationRegion,
      probationDeliveryUnit = probationDeliveryUnit,
      apArea = apArea,
      cruManagementArea = cruManagementArea,
      cruManagementAreaOverride = cruManagementAreaOverride,
      teamCodes = teamCodes,
      createdAt = createdAt,
      updatedAt = updatedAt,
    )
  }
}
