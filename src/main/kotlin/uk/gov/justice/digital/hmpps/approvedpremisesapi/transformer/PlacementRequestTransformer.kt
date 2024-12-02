package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingSummaryTransformer

@Component
class PlacementRequestTransformer(
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  private val assessmentTransformer: AssessmentTransformer,
  private val userTransformer: UserTransformer,
  bookingSummaryTransformer: BookingSummaryTransformer,
  cas1SpaceBookingSummaryTransformer: Cas1SpaceBookingSummaryTransformer,
) {

  val placementRequestBookingSummaryTransformer = PlacementRequestBookingSummaryTransformer(
    bookingSummaryTransformer,
    cas1SpaceBookingSummaryTransformer,
  )

  fun transformJpaToApi(jpa: PlacementRequestEntity, personInfo: PersonInfoResult): PlacementRequest {
    return PlacementRequest(
      id = jpa.id,
      gender = jpa.placementRequirements.gender,
      type = jpa.placementRequirements.apType,
      expectedArrival = jpa.expectedArrival,
      duration = jpa.duration,
      location = jpa.placementRequirements.postcodeDistrict.outcode,
      radius = jpa.placementRequirements.radius,
      essentialCriteria = jpa.placementRequirements.essentialCriteria.mapNotNull { characteristicToCriteria(it) },
      desirableCriteria = jpa.placementRequirements.desirableCriteria.mapNotNull { characteristicToCriteria(it) },
      person = personTransformer.transformModelToPersonApi(personInfo),
      risks = risksTransformer.transformDomainToApi(jpa.application.riskRatings!!, jpa.application.crn),
      applicationId = jpa.application.id,
      assessmentId = jpa.assessment.id,
      releaseType = getReleaseType(jpa.application.releaseType),
      status = getStatus(jpa),
      assessmentDecision = assessmentTransformer.transformJpaDecisionToApi(jpa.assessment.decision)!!,
      assessmentDate = jpa.assessment.submittedAt?.toInstant()!!,
      applicationDate = jpa.application.submittedAt?.toInstant()!!,
      assessor = userTransformer.transformJpaToApi(jpa.assessment.allocatedToUser!!, ServiceName.approvedPremises) as ApprovedPremisesUser,
      notes = jpa.notes,
      isParole = jpa.isParole,
      booking = placementRequestBookingSummaryTransformer.getBookingSummary(jpa),
      requestType = if (jpa.isParole) PlacementRequestRequestType.parole else PlacementRequestRequestType.standardRelease,
      isWithdrawn = jpa.isWithdrawn,
      withdrawalReason = getWithdrawalReason(jpa.withdrawalReason),
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

  fun transformToWithdrawable(jpa: PlacementRequestEntity) = Withdrawable(
    jpa.id,
    WithdrawableType.placementRequest,
    listOf(DatePeriod(jpa.expectedArrival, jpa.expectedDeparture())),
  )

  private fun characteristicToCriteria(characteristic: CharacteristicEntity): PlacementCriteria? {
    return try {
      PlacementCriteria.valueOf(characteristic.propertyName!!)
    } catch (exception: Exception) {
      null
    }
  }

  fun getReleaseType(releaseType: String?): ReleaseTypeOption = when (releaseType) {
    "licence" -> ReleaseTypeOption.LICENCE
    "rotl" -> ReleaseTypeOption.ROTL
    "hdc" -> ReleaseTypeOption.HDC
    "pss" -> ReleaseTypeOption.PSS
    "inCommunity" -> ReleaseTypeOption.IN_COMMUNITY
    "notApplicable" -> ReleaseTypeOption.NOT_APPLICABLE
    "extendedDeterminateLicence" -> ReleaseTypeOption.EXTENDED_DETERMINATE_LICENCE
    "paroleDirectedLicence" -> ReleaseTypeOption.PAROLE_DIRECTED_LICENCE
    "reReleasedPostRecall" -> ReleaseTypeOption.RE_RELEASED_POST_RECALL
    else -> throw RuntimeException("Unrecognised releaseType: $releaseType")
  }

  fun getWithdrawalReason(withdrawalReason: PlacementRequestWithdrawalReason?): WithdrawPlacementRequestReason? = when (withdrawalReason) {
    PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST -> WithdrawPlacementRequestReason.DUPLICATE_PLACEMENT_REQUEST
    PlacementRequestWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED -> WithdrawPlacementRequestReason.ALTERNATIVE_PROVISION_IDENTIFIED
    PlacementRequestWithdrawalReason.WITHDRAWN_BY_PP -> WithdrawPlacementRequestReason.WITHDRAWN_BY_PP
    PlacementRequestWithdrawalReason.CHANGE_IN_CIRCUMSTANCES -> WithdrawPlacementRequestReason.CHANGE_IN_CIRCUMSTANCES
    PlacementRequestWithdrawalReason.CHANGE_IN_RELEASE_DECISION -> WithdrawPlacementRequestReason.CHANGE_IN_RELEASE_DECISION
    PlacementRequestWithdrawalReason.NO_CAPACITY_DUE_TO_LOST_BED -> WithdrawPlacementRequestReason.NO_CAPACITY_DUE_TO_LOST_BED
    PlacementRequestWithdrawalReason.NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION -> WithdrawPlacementRequestReason.NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION
    PlacementRequestWithdrawalReason.NO_CAPACITY -> WithdrawPlacementRequestReason.NO_CAPACITY
    PlacementRequestWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST -> WithdrawPlacementRequestReason.ERROR_IN_PLACEMENT_REQUEST
    PlacementRequestWithdrawalReason.RELATED_APPLICATION_WITHDRAWN -> WithdrawPlacementRequestReason.RELATED_APPLICATION_WITHDRAWN
    PlacementRequestWithdrawalReason.RELATED_PLACEMENT_APPLICATION_WITHDRAWN -> WithdrawPlacementRequestReason.RELATED_PLACEMENT_REQUEST_WITHDRAWN
    null -> null
  }
}
