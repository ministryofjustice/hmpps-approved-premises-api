package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JpaApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostcodeDistrictRepository
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1PlacementRequirementsService(
  private val postcodeDistrictRepository: PostcodeDistrictRepository,
  private val characteristicRepository: CharacteristicRepository,
  private val placementRequirementsRepository: PlacementRequirementsRepository,
) {
  @SuppressWarnings("TooGenericExceptionThrown")
  fun createPlacementRequirements(
    assessment: ApprovedPremisesAssessmentEntity,
    requirements: PlacementRequirements,
  ): PlacementRequirementsEntity {
    val outcode = requirements.location
    val postcodeDistrict = postcodeDistrictRepository.findByOutcode(outcode)
      ?: error("Postcode outcode '$outcode' not found")

    val desirableCriteria = toCharacteristics(requirements.desirableCriteria)
    val essentialCriteria = toCharacteristics(requirements.essentialCriteria)

    val placementRequirementsEntity = placementRequirementsRepository.save(
      PlacementRequirementsEntity(
        id = UUID.randomUUID(),
        apType = JpaApType.fromApiType(requirements.type),
        postcodeDistrict = postcodeDistrict,
        radius = requirements.radius,
        desirableCriteria = desirableCriteria,
        essentialCriteria = essentialCriteria,
        createdAt = OffsetDateTime.now(),
        application = assessment.application as ApprovedPremisesApplicationEntity,
        assessment = assessment,
      ),
    )

    return placementRequirementsEntity
  }

  private fun toCharacteristics(criteria: List<PlacementCriteria>) = characteristicRepository.findAllWherePropertyNameIn(
    names = criteria.map { it.toString() },
    serviceName = ServiceName.approvedPremises.value,
  )
}
