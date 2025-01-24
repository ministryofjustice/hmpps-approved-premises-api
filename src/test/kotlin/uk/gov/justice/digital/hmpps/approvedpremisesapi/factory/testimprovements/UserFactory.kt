package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.testimprovements

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CruManagementAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.UserTestRepository
import java.time.OffsetDateTime
import java.util.UUID

class UserEntityFactoryProduce : Produce<UserEntity> {
  fun produceWithValues(
    id: UUID = UUID.randomUUID(),
    name: String = "TODO()",
    deliusUsername: String = "",
    deliusStaffCode: String = "",
    email: String = "",
    telephoneNumber: String = "",
    isActive: Boolean = true,
    applications: MutableList<ApplicationEntity> = mutableListOf(),
    roles: MutableList<UserRoleAssignmentEntity> = mutableListOf(),
    qualifications: MutableList<UserQualificationAssignmentEntity> = mutableListOf(),
    probationRegion: ProbationRegionEntity = ProbationRegionEntityFactory().produce(),
    probationDeliveryUnit: ProbationDeliveryUnitEntity = ProbationDeliveryUnitEntityFactory().produce(),
    apArea: ApAreaEntity = ApAreaEntityFactory().produce(),
    cruManagementArea: Cas1CruManagementAreaEntity? = Cas1CruManagementAreaEntityFactory().produce(),
    cruManagementAreaOverride: Cas1CruManagementAreaEntity? = Cas1CruManagementAreaEntityFactory().produce(),
    teamCodes: List<String> = listOf(),
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    updatedAt: OffsetDateTime = OffsetDateTime.now(),
  ): UserEntity {
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

  override fun produce(): UserEntity {
    println("producing entity")
    return produceWithValues()
  }
}

@Component
class UserEntityFactoryProduceAndPersist(override val repository: UserTestRepository) :
  ProduceAndPersist<UserEntity, UUID>() {

  override fun produce(): UserEntity {
    // remove the factory and bring code in here
    println("producing entity")
    // replace this with the actual code
    return UserEntityFactory().withDefaults().produce()
  }

  fun produceAndPersist(entity: ProbationRegionEntity): UserEntity {
    return repository.saveAndFlush(UserEntityFactory().withDefaults().withProbationRegion(entity).produce())
  }
}
