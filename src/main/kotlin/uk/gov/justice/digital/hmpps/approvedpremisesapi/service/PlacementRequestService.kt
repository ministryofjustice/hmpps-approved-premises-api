package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostcodeDistrictRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.time.OffsetDateTime
import java.util.UUID

@Service
class PlacementRequestService(
  private val placementRequestRepository: PlacementRequestRepository,
  private val postcodeDistrictRepository: PostcodeDistrictRepository,
  private val characteristicRepository: CharacteristicRepository,
  private val userRepository: UserRepository,
) {

  fun createPlacementRequest(assessment: AssessmentEntity, requirements: NewPlacementRequest): ValidatableActionResult<PlacementRequestEntity> =
    validated {
      val postcodeDistrict = postcodeDistrictRepository.findByOutcode(requirements.location)

      if (postcodeDistrict == null) {
        "$.postcodeDistrict" hasValidationError "doesNotExist"
      }

      val user = userRepository.findRandomMatcher() ?: throw RuntimeException("No Matchers could be found")

      val desirableCriteria = characteristicRepository.findAllWherePropertyNameIn(requirements.desirableCriteria.map { it.toString() })
      val essentialCriteria = characteristicRepository.findAllWherePropertyNameIn(requirements.essentialCriteria.map { it.toString() })

      val application = (assessment.application as? ApprovedPremisesApplicationEntity) ?: throw RuntimeException("Only Approved Premises Assessments are currently supported for Placement Requests")

      val placementRequestEntity = placementRequestRepository.save(
        PlacementRequestEntity(
          id = UUID.randomUUID(),
          apType = requirements.type,
          gender = requirements.gender,
          expectedArrival = requirements.expectedArrival,
          duration = requirements.duration,
          postcodeDistrict = postcodeDistrict!!,
          radius = requirements.radius,
          desirableCriteria = desirableCriteria,
          essentialCriteria = essentialCriteria,
          mentalHealthSupport = requirements.mentalHealthSupport,
          createdAt = OffsetDateTime.now(),
          application = application,
          allocatedToUser = user,
          booking = null,
          reallocatedAt = null,
        )
      )

      return success(placementRequestEntity)
    }
}
