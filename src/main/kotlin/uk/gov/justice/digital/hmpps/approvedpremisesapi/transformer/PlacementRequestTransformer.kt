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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult

@Component
class PlacementRequestTransformer(
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  private val assessmentTransformer: AssessmentTransformer,
  private val userTransformer: UserTransformer,
  private val bookingSummaryTransformer: BookingSummaryTransformer,
) {
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
      booking = jpa.booking?.let { bookingSummaryTransformer.transformJpaToApi(jpa.booking!!) },
      requestType = if (jpa.isParole) PlacementRequestRequestType.parole else PlacementRequestRequestType.standardRelease,
    )
  }

  fun getStatus(placementRequest: PlacementRequestEntity): PlacementRequestStatus {
    if (placementRequest.booking == null || placementRequest.booking?.cancellations?.any() == true) {
      if (placementRequest.bookingNotMades.any()) {
        return PlacementRequestStatus.unableToMatch
      }

      return PlacementRequestStatus.notMatched
    }

    return PlacementRequestStatus.matched
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
    "licence" -> ReleaseTypeOption.licence
    "rotl" -> ReleaseTypeOption.rotl
    "hdc" -> ReleaseTypeOption.hdc
    "pss" -> ReleaseTypeOption.pss
    "inCommunity" -> ReleaseTypeOption.inCommunity
    "notApplicable" -> ReleaseTypeOption.notApplicable
    else -> throw RuntimeException("Unrecognised releaseType: $releaseType")
  }
}
