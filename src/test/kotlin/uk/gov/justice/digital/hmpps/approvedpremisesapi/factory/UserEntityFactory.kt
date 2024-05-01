package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomEmailAddress
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomNumberChars
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.OffsetDateTime
import java.util.UUID

class UserEntityFactory : Factory<UserEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringUpperCase(12) }
  private var email: Yielded<String?> = { randomEmailAddress() }
  private var telephoneNumber: Yielded<String?> = { randomNumberChars(12) }
  private var deliusUsername: Yielded<String> = { randomStringUpperCase(12) }
  private var deliusStaffCode: Yielded<String> = { randomStringUpperCase(6) }
  private var deliusStaffIdentifier: Yielded<Long> = { randomInt(1000, 10000).toLong() }
  private var applications: Yielded<MutableList<ApplicationEntity>> = { mutableListOf() }
  private var qualifications: Yielded<MutableList<UserQualificationAssignmentEntity>> = { mutableListOf() }
  private var probationRegion: Yielded<ProbationRegionEntity>? = null
  private var probationDeliveryUnit: Yielded<ProbationDeliveryUnitEntity>? = null
  private var isActive: Yielded<Boolean> = { true }
  private var apArea: Yielded<ApAreaEntity?> = { null }
  private var teamCodes: Yielded<List<String>?> = { null }
  private var createdAt: Yielded<OffsetDateTime?> = { null }
  private var updatedAt: Yielded<OffsetDateTime?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withDeliusUsername(deliusUsername: String) = apply {
    this.deliusUsername = { deliusUsername }
  }

  fun withDeliusStaffCode(deliusStaffCode: String) = apply {
    this.deliusStaffCode = { deliusStaffCode }
  }

  fun withDeliusStaffIdentifier(deliusStaffIdentifier: Long) = apply {
    this.deliusStaffIdentifier = { deliusStaffIdentifier }
  }

  fun withApplications(applications: MutableList<ApplicationEntity>) = apply {
    this.applications = { applications }
  }

  fun withYieldedQualifications(qualifications: Yielded<MutableList<UserQualificationAssignmentEntity>>) = apply {
    this.qualifications = qualifications
  }

  fun withQualifications(qualifications: MutableList<UserQualificationAssignmentEntity>) = apply {
    this.qualifications = { qualifications }
  }

  fun withEmail(email: String?) = apply {
    this.email = { email }
  }

  fun withCreatedAt(createdAt: OffsetDateTime?) = apply {
    this.createdAt = { createdAt }
  }

  fun withTelephoneNumber(telephoneNumber: String?) = apply {
    this.telephoneNumber = { telephoneNumber }
  }

  fun withProbationRegion(probationRegion: ProbationRegionEntity) = apply {
    this.probationRegion = { probationRegion }
  }

  fun withYieldedProbationRegion(probationRegion: Yielded<ProbationRegionEntity>) = apply {
    this.probationRegion = probationRegion
  }

  fun withProbationDeliveryUnit(probationDeliveryUnit: Yielded<ProbationDeliveryUnitEntity>) = apply {
    this.probationDeliveryUnit = probationDeliveryUnit
  }

  fun withDefaults() = withDefaultProbationRegion()
  fun withDefaultProbationRegion() =
    withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }

  fun withIsActive(isActive: Boolean) = apply {
    this.isActive = { isActive }
  }

  fun withUnitTestControlProbationRegion() = apply {
    this.withProbationRegion(
      ProbationRegionEntityFactory()
        .withDeliusCode("REGION")
        .withApArea(
          ApAreaEntityFactory()
            .withIdentifier("APAREA")
            .produce(),
        )
        .produce(),
    )
  }

  fun withYieldedApArea(apArea: Yielded<ApAreaEntity>) = apply {
    this.apArea = apArea
  }

  fun withApArea(apArea: ApAreaEntity?) = apply {
    this.apArea = { apArea }
  }

  fun withTeamCodes(teamCodes: List<String>) = apply {
    this.teamCodes = { teamCodes }
  }

  override fun produce(): UserEntity = UserEntity(
    id = this.id(),
    name = this.name(),
    email = this.email(),
    telephoneNumber = this.telephoneNumber(),
    deliusUsername = this.deliusUsername(),
    deliusStaffCode = this.deliusStaffCode(),
    deliusStaffIdentifier = this.deliusStaffIdentifier(),
    applications = this.applications(),
    roles = mutableListOf(),
    qualifications = this.qualifications(),
    probationRegion = this.probationRegion?.invoke() ?: throw RuntimeException("A probation region must be provided"),
    probationDeliveryUnit = this.probationDeliveryUnit?.invoke(),
    isActive = this.isActive(),
    apArea = this.apArea(),
    teamCodes = this.teamCodes(),
    createdAt = this.createdAt(),
    updatedAt = this.updatedAt(),
  )
}
