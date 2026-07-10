package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.givenABooking(
  crn: String,
  application: ApplicationEntity? = null,
  premises: PremisesEntity? = null,
  arrivalDate: LocalDate = LocalDate.now().randomDateBefore(14),
  departureDate: LocalDate = LocalDate.now().randomDateBefore(14),
  adhoc: Boolean = false,
  actualArrivalDate: LocalDateTime? = null,
): BookingEntity {
  val booking = bookingEntityFactory.produceAndPersist {
    withCrn(crn)
    withPremises(premises ?: approvedPremisesEntityFactory.produceAndPersist())
    withApplication(application)
    withArrivalDate(arrivalDate)
    withDepartureDate(departureDate)
    withAdhoc(adhoc)
  }

  actualArrivalDate?.let {
    booking.arrivals.add(
      arrivalEntityFactory.produceAndPersist {
        withBooking(booking)
        withArrivalDate(actualArrivalDate.toLocalDate())
        withArrivalDateTime(actualArrivalDate.toInstant(ZoneOffset.UTC))
      },
    )
  }

  return booking
}
