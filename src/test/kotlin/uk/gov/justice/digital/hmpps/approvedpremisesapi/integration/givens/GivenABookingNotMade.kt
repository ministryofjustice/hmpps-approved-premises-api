package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity

fun IntegrationTestBase.givenABookingNotMade(
  placementRequest: PlacementRequestEntity,
): BookingNotMadeEntity = bookingNotMadeFactory.produceAndPersist {
  withPlacementRequest(placementRequest)
}
