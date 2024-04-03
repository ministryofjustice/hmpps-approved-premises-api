package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class LocalAuthorityAreaEntityFactory : Factory<LocalAuthorityAreaEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var identifier: Yielded<String> = { randomStringUpperCase(5) }
  private var name: Yielded<String> = { randomStringUpperCase(5) }

  override fun produce(): LocalAuthorityAreaEntity = LocalAuthorityAreaEntity(
    id = this.id(),
    identifier = this.identifier(),
    name = this.name(),
    premises = mutableListOf(),
  )
}
