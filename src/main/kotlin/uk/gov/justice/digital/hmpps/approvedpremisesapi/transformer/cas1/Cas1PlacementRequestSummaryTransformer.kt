package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult

@Component
class Cas1PlacementRequestSummaryTransformer {

  fun transformCas1PlacementRequestSummaryJpaToApi(jpa: uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1PlacementRequestSummary, isFullPerson: Boolean): Cas1PlacementRequestSummary {
    return Cas1PlacementRequestSummary(
      placementDatesDuration = jpa.getPlacementDatesDuration(),
      placementDatesExpectedArrivalDate = jpa.getPlacementDatesExpectedArrivalDate(),
      id = jpa.getId(),
      personCrn = jpa.getPersonCrn(),
      personIsFullPerson = isFullPerson,
      placementRequestStatus = jpa.getPlacementRequestStatus(),
      isParole = jpa.getIsParole(),
      personRiskTierEnvelopeValueLevel = jpa.getPersonRiskTierEnvelopeValueLevel(),
      applicationId = jpa.getApplicationId(),
      applicationDate = jpa.getApplicationDate(),
      bookingPremisesName = jpa.getBookingPremisesName(),
      bookingArrivalDate = jpa.getBookingArrivalDate(),
    )
  }

  fun transformPlacementRequestJpaToApi(jpa: PlacementRequestEntity, personInfo: PersonInfoResult): Cas1PlacementRequestSummary {
    return Cas1PlacementRequestSummary(
      placementDatesDuration = jpa.duration,
      placementDatesExpectedArrivalDate = jpa.expectedArrival,
      id = jpa.id,
      personCrn = personInfo.crn,
      personIsFullPerson = personInfo is PersonInfoResult.Success.Full,
      placementRequestStatus = getStatus(jpa).value,
      isParole = jpa.isParole,
      personRiskTierEnvelopeValueLevel = jpa.application.riskRatings?.tier?.value?.level,
      applicationId = jpa.application.id,
      applicationDate = jpa.application.submittedAt?.toLocalDate(),
      bookingPremisesName = jpa.booking?.premises?.name,
      bookingArrivalDate = jpa.booking?.arrivalDate,
    )
  }

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
