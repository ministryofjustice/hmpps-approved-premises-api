package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas3

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.net.URI
import java.time.LocalDate
import java.util.UUID

class CAS3BookingCancelledEventDetailsFactory : Factory<CAS3BookingCancelledEventDetails> {
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var bookingId: Yielded<UUID> = { UUID.randomUUID() }
  private var premisesId: Yielded<UUID> = { UUID.randomUUID() }
  private var cancellationReason: Yielded<String> = { randomStringMultiCaseWithNumbers(12) }
  private var notes: Yielded<String?> = { randomStringMultiCaseWithNumbers(12) }
  private var applicationId: Yielded<UUID?> = { null }
  private var cancelledAt: Yielded<LocalDate?> = { LocalDate.now().randomDateBefore(14) }
  private var cancelledBy: Yielded<StaffMember?> = { StaffMemberFactory().produce() }

  fun withPersonReference(configuration: PersonReferenceFactory.() -> Unit) = apply {
    this.personReference = { PersonReferenceFactory().apply(configuration).produce() }
  }

  fun withBookingId(bookingId: UUID) = apply {
    this.bookingId = { bookingId }
  }

  fun withPremisesId(premisesId: UUID) = apply {
    this.premisesId = { premisesId }
  }

  fun withCancellationReason(cancellationReason: String) = apply {
    this.cancellationReason = { cancellationReason }
  }

  fun withNotes(cancellationContext: String?) = apply {
    this.notes = { cancellationContext }
  }

  fun withApplicationId(applicationId: UUID?) = apply {
    this.applicationId = { applicationId }
  }

  fun withCancelledAt(date: LocalDate) = apply {
    this.cancelledAt = { date }
  }

  fun withCancelledBy(staffMember: StaffMember?) = apply {
    this.cancelledBy = { staffMember }
  }

  override fun produce(): CAS3BookingCancelledEventDetails {
    val bookingId = this.bookingId()
    val applicationId = this.applicationId()

    return CAS3BookingCancelledEventDetails(
      personReference = this.personReference(),
      bookingId = bookingId,
      bookingUrl = URI("http://api/premises/${this.premisesId()}/bookings/$bookingId"),
      cancellationReason = this.cancellationReason(),
      notes = this.notes(),
      applicationId = applicationId,
      applicationUrl = applicationId?.let { URI("http://api/applications/$it") },
      cancelledAt = this.cancelledAt(),
      cancelledBy = this.cancelledBy(),
    )
  }
}
