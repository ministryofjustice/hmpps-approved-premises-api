package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class LocalAuthorityAreaEntityFactory : Factory<LocalAuthorityAreaEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var identifier: Yielded<String> = { randomStringUpperCase(10) }
  private var name: Yielded<String> = { randomStringUpperCase(10) }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  override fun produce(): LocalAuthorityAreaEntity = LocalAuthorityAreaEntity(
    id = this.id(),
    identifier = this.identifier(),
    name = this.name(),
    premises = mutableListOf(),
  )
}
