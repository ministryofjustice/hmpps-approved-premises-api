package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import java.time.LocalDate
import java.util.UUID

class EventBookingSummaryFactory : Factory<EventBookingSummary> {
  private var bookingId = { UUID.randomUUID() }
  private var premises = { EventPremisesFactory().produce() }
  private var arrivalOn = { LocalDate.now() }
  private var departureOn = { LocalDate.now() }

  fun withBookingId(bookingId: UUID) = apply { this.bookingId = { bookingId } }
  fun withPremises(premises: Premises) = apply { this.premises = { premises } }
  fun withArrivalOn(arrivalOn: LocalDate) = apply { this.arrivalOn = { arrivalOn } }
  fun withDepartureOn(departureOn: LocalDate) = apply { this.departureOn = { departureOn } }

  override fun produce() = EventBookingSummary(
    bookingId = bookingId(),
    premises = premises(),
    arrivalDate = arrivalOn(),
    departureDate = departureOn(),
  )
}
