package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingSummaryTransformer

class PlacementRequestBookingSummaryTransformer(
  private val bookingSummaryTransformer: BookingSummaryTransformer,
  private val cas1SpaceBookingSummaryTransformer: Cas1SpaceBookingSummaryTransformer,
) {

  fun getBookingSummary(placementRequest: PlacementRequestEntity): BookingSummary? {
    return placementRequest.booking?.let {
      getBookingSummary(placementRequest.booking!!)
    } ?: getSpaceBookingSummary(placementRequest.spaceBookings)
  }

  private fun getBookingSummary(booking: BookingEntity): BookingSummary? {
    if (!booking.isCancelled) {
      return bookingSummaryTransformer.transformJpaToApi(booking)
    }
    return null
  }

  private fun getSpaceBookingSummary(bookings: List<Cas1SpaceBookingEntity>): BookingSummary? =
    bookings.firstOrNull { !it.isCancelled() }?.let { booking -> cas1SpaceBookingSummaryTransformer.transformJpaToApi(booking) }
}
