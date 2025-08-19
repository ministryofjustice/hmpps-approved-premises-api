package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity

class PlacementRequestBookingSummariesTransformer(
  private val bookingSummaryTransformer: PlacementRequestBookingSummaryTransformer,
) {

  fun getBookingSummary(placementRequest: PlacementRequestEntity): PlacementRequestBookingSummary? = placementRequest.spaceBookings
    .firstOrNull { !it.isCancelled() }
    ?.let { booking -> bookingSummaryTransformer.transformJpaToApi(booking) }
}
