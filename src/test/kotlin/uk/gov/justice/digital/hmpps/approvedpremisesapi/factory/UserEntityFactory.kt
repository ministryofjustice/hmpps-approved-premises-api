package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class UserEntityFactory : Factory<UserEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringUpperCase(12) }
  private var distinguishedName: Yielded<String> = { randomStringUpperCase(12) }
  private var isActive: Yielded<Boolean> = { false }
  private var applications: Yielded<MutableList<ApplicationEntity>> = { mutableListOf() }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withDistinguishedName(distinguishedName: String) = apply {
    this.distinguishedName = { distinguishedName }
  }

  fun withIsActive(isActive: Boolean) = apply {
    this.isActive = { isActive }
  }

  fun withApplications(applications: MutableList<ApplicationEntity>) = apply {
    this.applications = { applications }
  }

  override fun produce(): UserEntity = UserEntity(
    id = this.id(),
    name = this.name(),
    distinguishedName = this.distinguishedName(),
    isActive = this.isActive(),
    applications = this.applications()
  )
}
