package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomEmailAddress
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.OffsetDateTime
import java.util.UUID

class Cas2UserEntityFactory : Factory<Cas2UserEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var username: Yielded<String> = { randomStringUpperCase(12) }
  private var name: Yielded<String> = { randomStringUpperCase(12) }
  private var externalType: Yielded<String?> = { null }
  private var email: Yielded<String?> = { randomEmailAddress() }
  private var nomisStaffId: Yielded<Long?> = { randomInt(100000, 900000).toLong() }
  private var activeNomisCaseloadId: Yielded<String?> = { null }
  private var userType: Yielded<Cas2UserType> = { Cas2UserType.NOMIS }
  private var deliusStaffCode: Yielded<String?> = { null }
  private var deliusTeamCodes: Yielded<List<String>?> = { null }
  private var isEnabled: Yielded<Boolean> = { true }
  private var isActive: Yielded<Boolean> = { true }
  private var applications: Yielded<MutableList<Cas2ApplicationEntity>> = { mutableListOf() }
  private var serviceOrigin: Yielded<Cas2ServiceOrigin> = { Cas2ServiceOrigin.HDC }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withActiveNomisCaseloadId(activeNomisCaseloadId: String) = apply {
    this.activeNomisCaseloadId = { activeNomisCaseloadId }
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

  fun withApplications(applications: MutableList<Cas2ApplicationEntity>) = apply {
    this.applications = { applications }
  }

  fun withEmail(email: String?) = apply {
    this.email = { email }
  }

  fun withExternalType(externalType: String?) = apply {
    this.externalType = { externalType }
  }

  fun withUserType(t: Cas2UserType) = apply {
    this.userType = { t }
  }

  fun withIsActive(isActive: Boolean) = apply {
    this.isActive = { isActive }
  }

  fun withDeliusTeamCodes(deliusTeamCodes: List<String>?) = apply {
    this.deliusTeamCodes = { deliusTeamCodes }
  }

  fun withServiceOrigin(serviceOrigin: Cas2ServiceOrigin) = apply {
    this.serviceOrigin = { serviceOrigin }
  }

  override fun produce(): Cas2UserEntity = Cas2UserEntity(
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
    createdAt = OffsetDateTime.now(),
    externalType = this.externalType(),
    serviceOrigin = this.serviceOrigin(),
  )
}
