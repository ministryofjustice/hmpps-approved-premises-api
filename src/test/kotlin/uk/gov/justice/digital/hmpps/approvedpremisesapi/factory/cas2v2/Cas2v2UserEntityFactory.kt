package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomEmailAddress
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class Cas2v2UserEntityFactory : Factory<Cas2v2UserEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var username: Yielded<String> = { randomStringUpperCase(12) }
  private var name: Yielded<String> = { randomStringUpperCase(12) }
  private var email: Yielded<String?> = { randomEmailAddress() }
  private var nomisStaffId: Yielded<Long?> = { randomInt(100000, 900000).toLong() }
  private var activeNomisCaseloadId: Yielded<String?> = { null }
  private var userType: Yielded<Cas2v2UserType> = { Cas2v2UserType.NOMIS }
  private var deliusStaffCode: Yielded<String?> = { null }
  private var deliusTeamCodes: Yielded<List<String>?> = { null }
  private var isEnabled: Yielded<Boolean> = { true }
  private var isActive: Yielded<Boolean> = { true }
  private var applications: Yielded<MutableList<Cas2v2ApplicationEntity>> = { mutableListOf() }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withActiveNomisCaseloadId(activeCaseloadId: String) = apply {
    this.activeNomisCaseloadId = { activeCaseloadId }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withUsername(username: String) = apply {
    this.username = { username }
  }

  fun withNomisStaffCode(nomisStaffId: Long) = apply {
    this.nomisStaffId = { nomisStaffId }
  }

  fun withDeliusStaffCode(deliusStaffCode: String) = apply {
    this.deliusStaffCode = { deliusStaffCode }
  }

  fun withNomisStaffIdentifier(nomisStaffId: Long) = apply {
    this.nomisStaffId = { nomisStaffId }
  }

  fun withApplications(applications: MutableList<Cas2v2ApplicationEntity>) = apply {
    this.applications = { applications }
  }

  fun withEmail(email: String?) = apply {
    this.email = { email }
  }

  fun withUserType(t: Cas2v2UserType) = apply {
    this.userType = { t }
  }

  override fun produce(): Cas2v2UserEntity = Cas2v2UserEntity(
    id = this.id(),
    username = this.username(),
    name = this.name(),
    email = this.email(),
    nomisStaffId = this.nomisStaffId(),
    activeNomisCaseloadId = this.activeNomisCaseloadId(),
    deliusStaffCode = this.deliusStaffCode(),
    deliusTeamCodes = this.deliusTeamCodes(),
    userType = this.userType(),
    isEnabled = this.isEnabled(),
    isActive = this.isActive(),
    applications = this.applications(),
  )
}
