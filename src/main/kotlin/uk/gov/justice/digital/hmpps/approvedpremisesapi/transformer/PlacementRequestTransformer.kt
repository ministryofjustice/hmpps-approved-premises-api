package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail

@Component
class PlacementRequestTransformer(
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
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
      mentalHealthSupport = jpa.mentalHealthSupport,
      person = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail),
      risks = risksTransformer.transformDomainToApi(jpa.application.riskRatings!!, jpa.application.crn),
      applicationId = jpa.application.id,
      assessmentId = jpa.assessment.id,
      releaseType = getReleaseType(jpa.application.releaseType),
    )
  }

  private fun characteristicToCriteria(characteristic: CharacteristicEntity): PlacementCriteria? = when (characteristic.propertyName) {
    "isSemiSpecialistMentalHealth" -> PlacementCriteria.isSemiSpecialistMentalHealth
    "isRecoveryFocussed" -> PlacementCriteria.isRecoveryFocussed
    "isSuitableForVulnerable" -> PlacementCriteria.isSuitableForVulnerable
    "acceptsSexOffenders" -> PlacementCriteria.acceptsSexOffenders
    "acceptsChildSexOffenders" -> PlacementCriteria.acceptsChildSexOffenders
    "acceptsNonSexualChildOffenders" -> PlacementCriteria.acceptsNonSexualChildOffenders
    "acceptsHateCrimeOffenders" -> PlacementCriteria.acceptsHateCrimeOffenders
    "isCatered" -> PlacementCriteria.isCatered
    "hasWideStepFreeAccess" -> PlacementCriteria.hasWideStepFreeAccess
    "hasWideAccessToCommunalAreas" -> PlacementCriteria.hasWideAccessToCommunalAreas
    "hasStepFreeAccessToCommunalAreas" -> PlacementCriteria.hasStepFreeAccessToCommunalAreas
    "hasWheelChairAccessibleBathrooms" -> PlacementCriteria.hasWheelChairAccessibleBathrooms
    "hasLift" -> PlacementCriteria.hasLift
    "hasTactileFlooring" -> PlacementCriteria.hasTactileFlooring
    "hasBrailleSignage" -> PlacementCriteria.hasBrailleSignage
    "hasHearingLoop" -> PlacementCriteria.hasHearingLoop
    else -> { null }
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
