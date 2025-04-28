package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CruManagementAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class ApAreaEntityFactory : Factory<ApAreaEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var identifier: Yielded<String> = { randomStringUpperCase(10) }
  private var defaultCruManagementArea: Yielded<Cas1CruManagementAreaEntity> = { Cas1CruManagementAreaEntityFactory().produce() }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withIdentifier(identifier: String) = apply {
    this.identifier = { identifier }
  }

  fun withDefaultCruManagementArea(defaultCruManagementArea: Cas1CruManagementAreaEntity) = apply {
    this.defaultCruManagementArea = { defaultCruManagementArea }
  }

  override fun produce(): ApAreaEntity = ApAreaEntity(
    id = this.id(),
    name = this.name(),
    identifier = this.identifier(),
    defaultCruManagementArea = this.defaultCruManagementArea(),
  )
}
