package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.`Given a Booking`(
  crn: String,
  application: ApplicationEntity,
  premises: PremisesEntity? = null,
  arrivalDate: LocalDate = LocalDate.now().randomDateBefore(14),
  departureDate: LocalDate = LocalDate.now().randomDateBefore(14),
  adhoc: Boolean = false,
  actualArrivalDate: LocalDateTime? = null,
  actualDepartureDate: LocalDateTime? = null,
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
    arrivalEntityFactory.produceAndPersist {
      withBooking(booking)
      withArrivalDate(actualArrivalDate.toLocalDate())
      withArrivalDateTime(actualArrivalDate.toInstant(ZoneOffset.UTC))
    }
  }

  actualDepartureDate?.let {
    departureEntityFactory.produceAndPersist {
      withBooking(booking)
      withDateTime(actualDepartureDate.atOffset(ZoneOffset.UTC))
      withReason(departureReasonEntityFactory.produceAndPersist())
      withMoveOnCategory(moveOnCategoryEntityFactory.produceAndPersist())
    }
  }

  return booking
}

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.`Given a Booking for an Offline Application`(
  crn: String,
  offlineApplication: OfflineApplicationEntity,
  premises: PremisesEntity? = null,
  arrivalDate: LocalDate = LocalDate.now().randomDateBefore(14),
  departureDate: LocalDate = LocalDate.now().randomDateBefore(14),
) = bookingEntityFactory.produceAndPersist {
  withCrn(crn)
  withPremises(premises ?: approvedPremisesEntityFactory.produceAndPersist())
  withApplication(null)
  withOfflineApplication(offlineApplication)
  withArrivalDate(arrivalDate)
  withDepartureDate(departureDate)
  withAdhoc(true)
}
