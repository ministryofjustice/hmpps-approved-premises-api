package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.v2

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.v2.Cas3v2TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import java.time.OffsetDateTime
import java.util.UUID

class Cas3v2TurnaroundEntityFactory : Factory<Cas3v2TurnaroundEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var workingDayCount: Yielded<Int> = { randomInt(0, 14) }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().minusDays(14L).randomDateTimeBefore(14) }
  private var booking: Yielded<Cas3BookingEntity>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withWorkingDayCount(workingDayCount: Int) = apply {
    this.workingDayCount = { workingDayCount }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withBooking(booking: Cas3BookingEntity) = apply {
    this.booking = { booking }
  }

  fun withYieldedBooking(booking: Yielded<Cas3BookingEntity>) = apply {
    this.booking = booking
  }

  fun withDefaults() = apply {
    withBooking(Cas3BookingEntityFactory().withDefaults().produce())
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  override fun produce() = Cas3v2TurnaroundEntity(
    id = this.id(),
    workingDayCount = this.workingDayCount(),
    createdAt = this.createdAt(),
    booking = this.booking?.invoke() ?: throw RuntimeException("Must provide a Booking"),
  )
}
