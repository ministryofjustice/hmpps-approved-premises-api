package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult

@Component
class PlacementRequestDetailTransformer(
  private val placementRequestTransformer: PlacementRequestTransformer,
  private val cancellationTransformer: CancellationTransformer,
  private val bookingSummaryTransformer: BookingSummaryTransformer,
  private val applicationTransformer: ApplicationsTransformer,
) {
  fun transformJpaToApi(jpa: PlacementRequestEntity, personInfo: PersonInfoResult, cancellations: List<CancellationEntity>): PlacementRequestDetail {
    val placementRequest = placementRequestTransformer.transformJpaToApi(jpa, personInfo)

    return PlacementRequestDetail(
      id = placementRequest.id,
      gender = placementRequest.gender,
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
      cancellations = cancellations.mapNotNull { cancellationTransformer.transformJpaToApi(it) },
      booking = getBookingSummary(jpa),
      isWithdrawn = jpa.isWithdrawn,
      isParole = jpa.isParole,
      application = applicationTransformer.transformJpaToApi(jpa.application, personInfo),
    )
  }

  private fun getBookingSummary(placementRequest: PlacementRequestEntity): BookingSummary? {
    return placementRequest.booking?.let {
      bookingSummaryTransformer.transformJpaToApi(placementRequest.booking!!)
    }
  }
}
