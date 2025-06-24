package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class PlacementDateEntityFactory : Factory<PlacementDateEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var placementApplication: Yielded<PlacementApplicationEntity?> = { null }
  private var expectedArrival: Yielded<LocalDate> = { LocalDate.now() }
  private var duration: Yielded<Int> = { 12 }
  private var placementRequest: Yielded<PlacementRequestEntity?> = { null }

  fun withPlacementApplication(placementApplication: PlacementApplicationEntity) = apply {
    this.placementApplication = { placementApplication }
  }

  fun withExpectedArrival(expectedArrival: LocalDate) = apply {
    this.expectedArrival = { expectedArrival }
  }

  fun withDuration(duration: Int) = apply {
    this.duration = { duration }
  }

  fun withPlacementRequest(placementRequest: PlacementRequestEntity?) = apply {
    this.placementRequest = { placementRequest }
  }

  override fun produce(): PlacementDateEntity = PlacementDateEntity(
    id = this.id(),
    createdAt = this.createdAt(),
    placementApplication = this.placementApplication() ?: throw RuntimeException("Must provide a placementApplication"),
    expectedArrival = this.expectedArrival(),
    duration = this.duration(),
    placementRequest = this.placementRequest(),
  )
}
