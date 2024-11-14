package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus

@Service
class Cas1ApplicationStatusService(
  val applicationRepository: ApplicationRepository,
  val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  val bookingRepository: BookingRepository,
) {

  fun bookingMade(booking: BookingEntity) {
    val application = booking.application
    if (application != null) {
      bookingMade(application as ApprovedPremisesApplicationEntity)
    }
  }

  fun spaceBookingMade(spaceBooking: Cas1SpaceBookingEntity) {
    bookingMade(spaceBooking.application!!)
  }

  fun lastBookingCancelled(
    booking: BookingEntity,
    isUserRequestedWithdrawal: Boolean,
  ) {
    if (!isUserRequestedWithdrawal || booking.application == null) {
      return
    }
    val application = booking.application!!
    val bookings = bookingRepository.findAllByApplication(application)
    val anyActiveBookings = bookings.any { it.isActive() }
    if (!anyActiveBookings) {
      lastBookingCancelled(booking.application!! as ApprovedPremisesApplicationEntity)
    }
  }

  fun spaceBookingCancelled(spaceBooking: Cas1SpaceBookingEntity, isUserRequestedWithdrawal: Boolean = true) {
    if (!isUserRequestedWithdrawal || spaceBooking.application == null) {
      return
    }
    val application = spaceBooking.application!!
    val spaceBookingsForApplication = cas1SpaceBookingRepository.findAllByApplication(application)
    val anyActiveBookings = spaceBookingsForApplication.any { it.isActive() }
    if (!anyActiveBookings) {
      lastBookingCancelled(spaceBooking.application!!)
    }
  }

  private fun lastBookingCancelled(application: ApprovedPremisesApplicationEntity) {
    application.status = ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT
    applicationRepository.save(application)
  }

  private fun bookingMade(application: ApprovedPremisesApplicationEntity) {
    application.status = ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED
    applicationRepository.save(application)
  }
}
