package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1SpaceBookingEntityFactory : Factory<Cas1SpaceBookingEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var premises: Yielded<ApprovedPremisesEntity> = { ApprovedPremisesEntityFactory().withDefaults().produce() }
  private var placementRequest: Yielded<PlacementRequestEntity> = { PlacementRequestEntityFactory().withDefaults().produce() }
  private var application: Yielded<ApprovedPremisesApplicationEntity?> = { null }
  private var createdBy: Yielded<UserEntity> = { UserEntityFactory().withDefaults().produce() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var arrivalDate: Yielded<LocalDate> = { LocalDate.now() }
  private var departureDate: Yielded<LocalDate> = { LocalDate.now() }
  private val actualArrivalDateTime = { null }
  private val actualDepartureDateTime = { null }
  private val canonicalArrivalDate = { LocalDate.now() }
  private val canonicalDepartureDate = { LocalDate.now() }
  private val crn = { randomStringUpperCase(6) }
  private val keyWorkerStaffCode = { null }
  private val keyWorkerName = { null }
  private val keyWorkerAssignedAt = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withPremises(premises: ApprovedPremisesEntity) = apply {
    this.premises = { premises }
  }

  fun withPremises(configuration: ApprovedPremisesEntityFactory.() -> Unit) = apply {
    this.premises = { ApprovedPremisesEntityFactory().apply(configuration).produce() }
  }

  fun withYieldedPremises(premises: Yielded<ApprovedPremisesEntity>) = apply {
    this.premises = premises
  }

  fun withPlacementRequest(placementRequest: PlacementRequestEntity) = apply {
    this.placementRequest = { placementRequest }
  }

  fun withPlacementRequest(configuration: PlacementRequestEntityFactory.() -> Unit) = apply {
    this.placementRequest = { PlacementRequestEntityFactory().apply(configuration).produce() }
  }

  fun withYieldedPlacementRequest(placementRequest: Yielded<PlacementRequestEntity>) = apply {
    this.placementRequest = placementRequest
  }

  fun withCreatedBy(createdBy: UserEntity) = apply {
    this.createdBy = { createdBy }
  }

  fun withCreatedBy(configuration: UserEntityFactory.() -> Unit) = apply {
    this.createdBy = { UserEntityFactory().apply(configuration).produce() }
  }

  fun withYieldedCreatedBy(createdBy: Yielded<UserEntity>) = apply {
    this.createdBy = createdBy
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withArrivalDate(arrivalDate: LocalDate) = apply {
    this.arrivalDate = { arrivalDate }
  }

  fun withDepartureDate(departureDate: LocalDate) = apply {
    this.departureDate = { departureDate }
  }

  override fun produce() = Cas1SpaceBookingEntity(
    id = this.id(),
    premises = this.premises(),
    placementRequest = this.placementRequest(),
    createdBy = this.createdBy(),
    createdAt = this.createdAt(),
    expectedArrivalDate = this.arrivalDate(),
    expectedDepartureDate = this.departureDate(),
    actualArrivalDateTime = this.actualArrivalDateTime(),
    actualDepartureDateTime = this.actualDepartureDateTime(),
    canonicalArrivalDate = this.canonicalArrivalDate(),
    canonicalDepartureDate = this.canonicalDepartureDate(),
    crn = this.crn(),
    keyWorkerStaffCode = this.keyWorkerStaffCode(),
    keyWorkerName = this.keyWorkerName(),
    keyWorkerAssignedAt = this.keyWorkerAssignedAt(),
    application = this.application(),
  )
}
