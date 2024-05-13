package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PrisonReleaseTypeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class PrisonReleaseTypeEntityFactory : Factory<PrisonReleaseTypeEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var abbreviation: Yielded<String> = { randomStringMultiCaseWithNumbers(5) }
  private var isActive: Yielded<Boolean> = { true }
  private var serviceScope: Yielded<String> = { randomStringUpperCase(4) }
  private var sortOrder: Yielded<Int> = { randomInt(0, 1000) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withAbbreviation(abbreviation: String) = apply {
    this.abbreviation = { abbreviation }
  }
  fun withIsActive(isActive: Boolean) = apply {
    this.isActive = { isActive }
  }

  fun withServiceScope(serviceScope: String) = apply {
    this.serviceScope = { serviceScope }
  }

  fun withSortOrder(sortOrder: Int) = apply {
    this.sortOrder = { sortOrder }
  }

  override fun produce(): PrisonReleaseTypeEntity = PrisonReleaseTypeEntity(
    id = this.id(),
    name = this.name(),
    abbreviation = this.abbreviation(),
    isActive = this.isActive(),
    serviceScope = this.serviceScope(),
    sortOrder = this.sortOrder(),
  )
}
