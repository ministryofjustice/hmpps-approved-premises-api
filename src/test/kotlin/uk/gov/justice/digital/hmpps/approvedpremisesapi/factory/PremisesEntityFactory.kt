package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.PremisesTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.lang.RuntimeException
import java.util.UUID

class PremisesEntityFactory(
  premisesTestRepository: PremisesTestRepository
) : PersistedFactory<PremisesEntity, UUID>(premisesTestRepository) {
  private var probationRegion: Yielded<ProbationRegionEntity>? = null
  private var apArea: Yielded<ApAreaEntity>? = null
  private var localAuthorityArea: Yielded<LocalAuthorityAreaEntity>? = null
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var apCode: Yielded<String> = { randomStringUpperCase(5) }
  private var postcode: Yielded<String> = { randomPostCode() }
  private var totalBeds: Yielded<Int> = { randomInt(1, 100) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withApCode(apCode: String) = apply {
    this.apCode = { apCode }
  }

  fun withPostcode(postcode: String) = apply {
    this.postcode = { postcode }
  }

  fun withTotalBeds(totalBeds: Int) = apply {
    this.totalBeds = { totalBeds }
  }

  fun withProbationRegion(probationRegion: ProbationRegionEntity) = apply {
    this.probationRegion = { probationRegion }
  }

  fun withYieldedProbationRegion(probationRegion: Yielded<ProbationRegionEntity>) = apply {
    this.probationRegion = probationRegion
  }

  fun withApArea(apAreaEntity: ApAreaEntity) = apply {
    this.apArea = { apAreaEntity }
  }

  fun withYieldedApArea(apAreaEntity: Yielded<ApAreaEntity>) = apply {
    this.apArea = apAreaEntity
  }

  fun withLocalAuthorityArea(localAuthorityAreaEntity: LocalAuthorityAreaEntity) = apply {
    this.localAuthorityArea = { localAuthorityAreaEntity }
  }

  fun withYieldedLocalAuthorityArea(localAuthorityAreaEntity: Yielded<LocalAuthorityAreaEntity>) = apply {
    this.localAuthorityArea = localAuthorityAreaEntity
  }

  override fun produce(): PremisesEntity = PremisesEntity(
    id = this.id(),
    name = this.name(),
    apCode = this.apCode(),
    postcode = this.postcode(),
    totalBeds = this.totalBeds(),
    probationRegion = this.probationRegion?.invoke() ?: throw RuntimeException("Must provide a probation region"),
    apArea = this.apArea?.invoke() ?: throw RuntimeException("Must provide an ApArea"),
    localAuthorityArea = this.localAuthorityArea?.invoke() ?: throw RuntimeException("Must provide a local authority area")
  )
}
