package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.PersonTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class PersonEntityFactory(
  personTestRepository: PersonTestRepository
) : PersistedFactory<PersonEntity, UUID>(personTestRepository) {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var name: Yielded<String> = { randomStringUpperCase(12) }
  private var booking: Yielded<BookingEntity>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withYieldedBooking(booking: Yielded<BookingEntity>) = apply {
    this.booking = booking
  }

  fun withBooking(booking: BookingEntity) = apply {
    this.booking = { booking }
  }

  override fun produce(): PersonEntity = PersonEntity(
    id = this.id(),
    crn = this.crn(),
    name = this.name(),
    booking = this.booking?.invoke() ?: throw RuntimeException("Must provide a Booking")
  )
}
