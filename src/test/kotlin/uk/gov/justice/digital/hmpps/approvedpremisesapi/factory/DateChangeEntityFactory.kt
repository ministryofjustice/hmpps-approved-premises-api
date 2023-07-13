package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class DateChangeEntityFactory : Factory<DateChangeEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var changedAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(7) }
  private var previousArrivalDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(7) }
  private var previousDepartureDate: Yielded<LocalDate> = { LocalDate.now().randomDateAfter(7) }
  private var newArrivalDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(7) }
  private var newDepartureDate: Yielded<LocalDate> = { LocalDate.now().randomDateAfter(7) }
  private var booking: Yielded<BookingEntity>? = null
  private var changedByUser: Yielded<UserEntity>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withChangedAt(changedAt: OffsetDateTime) = apply {
    this.changedAt = { changedAt }
  }

  fun withPreviousArrivalDate(previousArrivalDate: LocalDate) = apply {
    this.previousArrivalDate = { previousArrivalDate }
  }

  fun withPreviousDepartureDate(previousDepartureDate: LocalDate) = apply {
    this.previousDepartureDate = { previousDepartureDate }
  }

  fun withNewArrivalDate(newArrivalDate: LocalDate) = apply {
    this.newArrivalDate = { newArrivalDate }
  }

  fun withNewDepartureDate(newDepartureDate: LocalDate) = apply {
    this.newDepartureDate = { newDepartureDate }
  }

  fun withBooking(booking: BookingEntity) = apply {
    this.booking = { booking }
  }

  fun withChangedByUser(changedByUser: UserEntity) = apply {
    this.changedByUser = { changedByUser }
  }

  override fun produce() = DateChangeEntity(
    id = this.id(),
    changedAt = this.changedAt(),
    previousArrivalDate = this.previousArrivalDate(),
    previousDepartureDate = this.previousDepartureDate(),
    newArrivalDate = this.newArrivalDate(),
    newDepartureDate = this.newDepartureDate(),
    booking = this.booking?.invoke() ?: throw RuntimeException("Must provide a booking"),
    changedByUser = this.changedByUser?.invoke() ?: throw RuntimeException("Must provide a changedByUser"),
  )
}
