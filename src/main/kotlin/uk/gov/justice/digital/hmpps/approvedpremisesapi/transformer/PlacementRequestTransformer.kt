package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail

@Component
class PlacementRequestTransformer(
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  private val assessmentTransformer: AssessmentTransformer,
  private val userTransformer: UserTransformer,
) {
  fun transformJpaToApi(jpa: PlacementRequestEntity, offenderDetailSummary: OffenderDetailSummary, inmateDetail: InmateDetail): PlacementRequest {
    return PlacementRequest(
      id = jpa.id,
      gender = jpa.gender,
      type = jpa.apType,
      expectedArrival = jpa.expectedArrival,
      duration = jpa.duration,
      location = jpa.postcodeDistrict.outcode,
      radius = jpa.radius,
      essentialCriteria = jpa.essentialCriteria.mapNotNull { characteristicToCriteria(it) },
      desirableCriteria = jpa.desirableCriteria.mapNotNull { characteristicToCriteria(it) },
      person = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail),
      risks = risksTransformer.transformDomainToApi(jpa.application.riskRatings!!, jpa.application.crn),
      applicationId = jpa.application.id,
      assessmentId = jpa.assessment.id,
      releaseType = getReleaseType(jpa.application.releaseType)!!,
      status = getStatus(jpa),
      assessmentDecision = assessmentTransformer.transformJpaDecisionToApi(jpa.assessment.decision)!!,
      assessmentDate = jpa.assessment.submittedAt?.toInstant()!!,
      assessor = userTransformer.transformJpaToApi(jpa.assessment.allocatedToUser, ServiceName.approvedPremises) as ApprovedPremisesUser,
      notes = jpa.notes,
    )
  }

  private fun getStatus(placementRequest: PlacementRequestEntity): PlacementRequestStatus {
    if (placementRequest.booking == null) {
      if (placementRequest.bookingNotMades.any()) {
        return PlacementRequestStatus.unableToMatch
      }

      return PlacementRequestStatus.notMatched
    }

    return PlacementRequestStatus.matched
  }

  private fun characteristicToCriteria(characteristic: CharacteristicEntity): PlacementCriteria? {
    return try {
      PlacementCriteria.valueOf(characteristic.propertyName!!)
    } catch (exception: Exception) {
      null
    }
  }

  private fun getReleaseType(releaseType: String?): ReleaseTypeOption? = when (releaseType) {
    "licence" -> ReleaseTypeOption.licence
    "rotl" -> ReleaseTypeOption.rotl
    "hdc" -> ReleaseTypeOption.hdc
    "pss" -> ReleaseTypeOption.pss
    "in_community" -> ReleaseTypeOption.inCommunity
    else -> { null }
  }
}
