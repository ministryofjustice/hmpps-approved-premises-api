package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomEmailAddress
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class OffenderManagementUnitEntityFactory : Factory<OffenderManagementUnitEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var prisonCode: Yielded<String> = { randomStringUpperCase(3) }
  private var prisonName: Yielded<String> = { randomStringUpperCase(8) }
  private var email: Yielded<String> = { randomEmailAddress() }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withPrisonName(prisonName: String) = apply {
    this.prisonName = { prisonName }
  }

  fun withPrisonCode(prisonCode: String) = apply {
    this.prisonCode = { prisonCode }
  }

  fun withEmail(email: String) = apply {
    this.email = { email }
  }

  override fun produce(): OffenderManagementUnitEntity = OffenderManagementUnitEntity(
    id = this.id(),
    prisonName = this.prisonName(),
    prisonCode = this.prisonCode(),
    email = this.email(),
  )
}
