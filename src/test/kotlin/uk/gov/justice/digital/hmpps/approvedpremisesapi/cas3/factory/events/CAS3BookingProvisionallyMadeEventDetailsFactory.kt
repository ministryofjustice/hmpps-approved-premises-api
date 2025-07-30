package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingProvisionallyMadeEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import java.net.URI
import java.time.Instant
import java.util.UUID

class CAS3BookingProvisionallyMadeEventDetailsFactory : Factory<CAS3BookingProvisionallyMadeEventDetails> {
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var bookingId: Yielded<UUID> = { UUID.randomUUID() }
  private var premisesId: Yielded<UUID> = { UUID.randomUUID() }
  private var expectedArrivedAt: Yielded<Instant> = { Instant.now() }
  private var notes: Yielded<String> = { randomStringLowerCase(20) }
  private var applicationId: Yielded<UUID?> = { null }
  private var bookedBy: Yielded<StaffMember?> = { StaffMemberFactory().produce() }

  fun withPersonReference(configuration: PersonReferenceFactory.() -> Unit) = apply {
    this.personReference = { PersonReferenceFactory().apply(configuration).produce() }
  }

  fun withBookingId(bookingId: UUID) = apply {
    this.bookingId = { bookingId }
  }

  fun withPremisesId(premisesId: UUID) = apply {
    this.premisesId = { premisesId }
  }

  fun withExpectedArrivedAt(expectedArrivedAt: Instant) = apply {
    this.expectedArrivedAt = { expectedArrivedAt }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withApplicationId(applicationId: UUID?) = apply {
    this.applicationId = { applicationId }
  }

  fun withBookedBy(staffMember: StaffMember?) = apply {
    this.bookedBy = { staffMember }
  }

  override fun produce(): CAS3BookingProvisionallyMadeEventDetails {
    val bookingId = this.bookingId()
    val applicationId = this.applicationId()

    return CAS3BookingProvisionallyMadeEventDetails(
      personReference = this.personReference(),
      bookingId = bookingId,
      bookingUrl = URI("http://api/premises/${this.premisesId()}/bookings/$bookingId"),
      expectedArrivedAt = this.expectedArrivedAt(),
      notes = this.notes(),
      applicationId = applicationId,
      applicationUrl = applicationId?.let { URI("http://api/applications/$it") },
      bookedBy = this.bookedBy(),
    )
  }
}
