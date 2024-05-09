package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomEmailAddress
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class NomisUserEntityFactory : Factory<NomisUserEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var nomisUsername: Yielded<String> = { randomStringUpperCase(12) }
  private var nomisStaffId: Yielded<Long> = { randomInt(100000, 900000).toLong() }
  private var name: Yielded<String> = { randomStringUpperCase(12) }
  private var email: Yielded<String?> = { randomEmailAddress() }
  private var accountType: Yielded<String> = { "GENERAL" }
  private var isEnabled: Yielded<Boolean> = { true }
  private var isActive: Yielded<Boolean> = { true }
  private var activeCaseloadId: Yielded<String?> = { null }
  private var applications: Yielded<MutableList<Cas2ApplicationEntity>> = { mutableListOf() }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withActiveCaseloadId(activeCaseloadId: String) = apply {
    this.activeCaseloadId = { activeCaseloadId }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withNomisUsername(nomisUsername: String) = apply {
    this.nomisUsername = { nomisUsername }
  }

  fun withNomisStaffCode(nomisStaffId: Long) = apply {
    this.nomisStaffId = { nomisStaffId }
  }

  fun withNomisStaffIdentifier(nomisStaffId: Long) = apply {
    this.nomisStaffId = { nomisStaffId }
  }

  fun withApplications(applications: MutableList<Cas2ApplicationEntity>) = apply {
    this.applications = { applications }
  }

  fun withEmail(email: String?) = apply {
    this.email = { email }
  }

  override fun produce(): NomisUserEntity = NomisUserEntity(
    id = this.id(),
    nomisUsername = this.nomisUsername(),
    nomisStaffId = this.nomisStaffId(),
    name = this.name(),
    accountType = this.accountType(),
    isEnabled = this.isEnabled(),
    isActive = this.isActive(),
    email = this.email(),
    applications = this.applications(),
    activeCaseloadId = this.activeCaseloadId(),
  )
}
