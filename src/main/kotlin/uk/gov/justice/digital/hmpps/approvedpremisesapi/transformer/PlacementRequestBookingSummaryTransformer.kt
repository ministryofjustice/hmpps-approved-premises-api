package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity

class PlacementRequestBookingSummaryTransformer(
  private val bookingSummaryTransformer: BookingSummaryTransformer,
) {

  fun getBookingSummary(placementRequest: PlacementRequestEntity): BookingSummary? {
    val booking = placementRequest.booking

    if (booking != null && !booking.isCancelled) {
      return bookingSummaryTransformer.transformJpaToApi(booking)
    }

    return null
  }
}
