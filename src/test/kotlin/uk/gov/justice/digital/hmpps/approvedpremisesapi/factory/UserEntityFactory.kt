package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class UserEntityFactory : Factory<UserEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringUpperCase(12) }
  private var deliusUsername: Yielded<String> = { randomStringUpperCase(12) }
  private var deliusStaffIdentifier: Yielded<Long> = { randomInt(1000, 10000).toLong() }
  private var applications: Yielded<MutableList<ApplicationEntity>> = { mutableListOf() }
  private var roles: Yielded<MutableList<UserRoleAssignmentEntity>> = { mutableListOf() }
  private var qualifications: Yielded<MutableList<UserQualificationAssignmentEntity>> = { mutableListOf() }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withDeliusUsername(deliusUsername: String) = apply {
    this.deliusUsername = { deliusUsername }
  }

  fun withDeliusStaffIdentifier(deliusStaffIdentifier: Long) = apply {
    this.deliusStaffIdentifier = { deliusStaffIdentifier }
  }

  fun withApplications(applications: MutableList<ApplicationEntity>) = apply {
    this.applications = { applications }
  }

  fun withYieldedRoles(roles: Yielded<MutableList<UserRoleAssignmentEntity>>) = apply {
    this.roles = roles
  }

  fun withRoles(roles: MutableList<UserRoleAssignmentEntity>) = apply {
    this.roles = { roles }
  }

  fun withYieldedQualifications(qualifications: Yielded<MutableList<UserQualificationAssignmentEntity>>) = apply {
    this.qualifications = qualifications
  }

  fun withQualifications(qualifications: MutableList<UserQualificationAssignmentEntity>) = apply {
    this.qualifications = { qualifications }
  }

  override fun produce(): UserEntity = UserEntity(
    id = this.id(),
    name = this.name(),
    deliusUsername = this.deliusUsername(),
    deliusStaffIdentifier = this.deliusStaffIdentifier(),
    applications = this.applications(),
    roles = this.roles(),
    qualifications = this.qualifications()
  )
}
