package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementRequestDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ChangeRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer

@Component
class PlacementRequestDetailTransformer(
  private val placementRequestTransformer: PlacementRequestTransformer,
  bookingSummaryTransformer: PlacementRequestBookingSummaryTransformer,
  private val applicationTransformer: ApplicationsTransformer,
  private val personTransformer: PersonTransformer,
  private val cas1SpaceBookingTransformer: Cas1SpaceBookingTransformer,
  private val cas1ChangeRequestTransformer: Cas1ChangeRequestTransformer,
) {
  val placementRequestBookingSummaryTransformer = PlacementRequestBookingSummariesTransformer(
    bookingSummaryTransformer,
  )

  fun transformJpaToCas1PlacementRequestDetail(
    jpa: PlacementRequestEntity,
    personInfo: PersonInfoResult,
    changeRequests: List<Cas1ChangeRequestEntity>,
  ): Cas1PlacementRequestDetail {
    val placementRequest = placementRequestTransformer.transformJpaToApi(jpa, personInfo)
    val personSummaryInfo = personTransformer.personInfoResultToPersonSummaryInfoResult(personInfo)
    val placementRequestBookingSummary = placementRequestBookingSummaryTransformer.getBookingSummary(jpa)
    val openChangeRequests = cas1ChangeRequestTransformer.transformToChangeRequestSummaries(changeRequests, personInfo)

    return Cas1PlacementRequestDetail(
      id = placementRequest.id,
      type = placementRequest.type,
      expectedArrival = placementRequest.expectedArrival,
      duration = placementRequest.duration,
      location = placementRequest.location,
      radius = placementRequest.radius,
      essentialCriteria = placementRequest.essentialCriteria,
      desirableCriteria = placementRequest.desirableCriteria,
      person = placementRequest.person,
      risks = placementRequest.risks,
      applicationId = placementRequest.applicationId,
      assessmentId = placementRequest.assessmentId,
      releaseType = placementRequest.releaseType,
      status = placementRequest.status,
      assessmentDecision = placementRequest.assessmentDecision,
      assessmentDate = placementRequest.assessmentDate,
      applicationDate = placementRequest.applicationDate,
      assessor = placementRequest.assessor,
      notes = placementRequest.notes,
      booking = placementRequestBookingSummary,
      legacyBooking = if (placementRequestBookingSummary?.type == PlacementRequestBookingSummary.Type.legacy) {
        placementRequestBookingSummary
      } else {
        null
      },
      isParole = jpa.isParole,
      isWithdrawn = jpa.isWithdrawn,
      application = applicationTransformer.transformJpaToCas1Application(jpa.application, personInfo),
      spaceBookings = jpa.spaceBookings.filter { it.isActive() }
        .map { cas1SpaceBookingTransformer.transformToSummary(it, personSummaryInfo) },
      openChangeRequests = openChangeRequests,
      requestedPlacementPeriod = requestedPlacementPeriod(jpa),
      authorisedPlacementPeriod = authorisedPlacementPeriod(jpa),
    )
  }

  private fun requestedPlacementPeriod(placementRequestEntity: PlacementRequestEntity): Cas1RequestedPlacementPeriod {
    val placementApplication = placementRequestEntity.placementApplication
    return if (placementRequestEntity.isForLegacyInitialRequestForPlacement()) {
      Cas1RequestedPlacementPeriod(
        arrival = placementRequestEntity.expectedArrival,
        duration = placementRequestEntity.duration,
        arrivalFlexible = null,
      )
    } else {
      Cas1RequestedPlacementPeriod(
        arrival = placementApplication!!.expectedArrival!!,
        duration = placementApplication.requestedDuration!!,
        arrivalFlexible = placementApplication.expectedArrivalFlexible,
      )
    }
  }

  private fun authorisedPlacementPeriod(placementRequestEntity: PlacementRequestEntity): Cas1RequestedPlacementPeriod {
    val placementApplication = placementRequestEntity.placementApplication
    return if (placementRequestEntity.isForLegacyInitialRequestForPlacement()) {
      Cas1RequestedPlacementPeriod(
        arrival = placementRequestEntity.expectedArrival,
        duration = placementRequestEntity.duration,
        arrivalFlexible = null,
      )
    } else {
      Cas1RequestedPlacementPeriod(
        arrival = placementApplication!!.expectedArrival!!,
        duration = placementApplication.authorisedDuration!!,
        arrivalFlexible = placementApplication.expectedArrivalFlexible,
      )
    }
  }
}
