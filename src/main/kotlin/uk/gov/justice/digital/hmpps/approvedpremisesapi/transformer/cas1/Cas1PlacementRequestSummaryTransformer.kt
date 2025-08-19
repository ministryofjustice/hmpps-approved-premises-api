package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementRequestSummary.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer

@Component
class Cas1PlacementRequestSummaryTransformer(
  private val personTransformer: PersonTransformer,
) {

  fun transformCas1PlacementRequestSummaryJpaToApi(
    jpa: uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1PlacementRequestSummary,
    personInfo: PersonInfoResult,
  ): Cas1PlacementRequestSummary = Cas1PlacementRequestSummary(
    requestedPlacementDuration = jpa.getRequestedPlacementDuration(),
    requestedPlacementArrivalDate = jpa.getRequestedPlacementArrivalDate(),
    id = jpa.getId(),
    person = personTransformer.transformModelToPersonApi(personInfo),
    placementRequestStatus = jpa.getPlacementRequestStatus(),
    isParole = jpa.getIsParole(),
    personTier = jpa.getPersonTier(),
    applicationId = jpa.getApplicationId(),
    applicationSubmittedDate = jpa.getApplicationSubmittedDate(),
    firstBookingPremisesName = jpa.getBookingPremisesName(),
    firstBookingArrivalDate = jpa.getBookingArrivalDate(),
  )

  fun getStatus(placementRequest: PlacementRequestEntity): PlacementRequestStatus {
    if (placementRequest.hasActiveBooking()) {
      return PlacementRequestStatus.matched
    }

    if (placementRequest.bookingNotMades.any()) {
      return PlacementRequestStatus.unableToMatch
    }

    return PlacementRequestStatus.notMatched
  }
}
