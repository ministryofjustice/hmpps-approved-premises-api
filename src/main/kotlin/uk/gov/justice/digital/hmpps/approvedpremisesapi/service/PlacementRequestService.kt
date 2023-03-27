package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostcodeDistrictRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
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

  fun getVisiblePlacementRequestsForUser(user: UserEntity): List<PlacementRequestEntity> {
    return placementRequestRepository.findAllByAllocatedToUser_IdAndReallocatedAtNull(user.id)
  }

  fun getPlacementRequestForUser(user: UserEntity, id: UUID): AuthorisableActionResult<PlacementRequestEntity> {
    val placementRequest = placementRequestRepository.findByIdOrNull(id)
      ?: return AuthorisableActionResult.NotFound()

    if (placementRequest.allocatedToUser.id != user.id && !user.hasRole(UserRole.WORKFLOW_MANAGER)) {
      return AuthorisableActionResult.Unauthorised()
    }

    return AuthorisableActionResult.Success(placementRequest)
  }

  fun getPlacementRequestForUserAndApplication(user: UserEntity, applicationID: UUID): AuthorisableActionResult<PlacementRequestEntity> {
    val placementRequest = placementRequestRepository.findByApplication_IdAndReallocatedAtNull(applicationID)
      ?: return AuthorisableActionResult.NotFound()

    if (!user.hasRole(UserRole.WORKFLOW_MANAGER) && placementRequest.allocatedToUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    return AuthorisableActionResult.Success(placementRequest)
  }

  fun reallocatePlacementRequest(assigneeUser: UserEntity, application: ApprovedPremisesApplicationEntity): AuthorisableActionResult<ValidatableActionResult<PlacementRequestEntity>> {
    val currentPlacementRequest = placementRequestRepository.findByApplication_IdAndReallocatedAtNull(application.id)
      ?: return AuthorisableActionResult.NotFound()

    if (currentPlacementRequest.booking != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This placement request has already been completed")
      )
    }

    if (!assigneeUser.hasRole(UserRole.MATCHER)) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(ValidationErrors().apply { this["$.userId"] = "lackingMatcherRole" })
      )
    }

    currentPlacementRequest.reallocatedAt = OffsetDateTime.now()
    placementRequestRepository.save(currentPlacementRequest)

    // Make the timestamp precision less precise, so we don't have any issues with microsecond resolution in tests
    val dateTimeNow = OffsetDateTime.now().withNano(0)

    val newPlacementRequest = placementRequestRepository.save(
      currentPlacementRequest.copy(
        id = UUID.randomUUID(),
        reallocatedAt = null,
        allocatedToUser = assigneeUser,
        createdAt = dateTimeNow
      )
    )

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        newPlacementRequest
      )
    )
  }

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
