package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PlacementApplicationsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromAuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromValidatableActionResult
import java.util.UUID

@Service
class PlacementApplicationsController(
  private val userService: UserService,
  private val applicationService: ApplicationService,
  private val placementApplicationService: PlacementApplicationService,
  private val placementApplicationTransformer: PlacementApplicationTransformer,
  private val objectMapper: ObjectMapper,
) : PlacementApplicationsApiDelegate {
  override fun placementApplicationsPost(newPlacementApplication: NewPlacementApplication): ResponseEntity<PlacementApplication> {
    val user = userService.getUserForRequest()

    val application = extractEntityFromAuthorisableActionResult(
      applicationService.getApplicationForUsername(newPlacementApplication.applicationId, user.deliusUsername),
      newPlacementApplication.applicationId.toString(),
      "Placement Application",
    )

    val placementApplication = extractEntityFromValidatableActionResult(
      placementApplicationService.createApplication(application, user),
    )

    return ResponseEntity.ok(placementApplicationTransformer.transformJpaToApi(placementApplication))
  }

  override fun placementApplicationsIdGet(id: UUID): ResponseEntity<PlacementApplication> {
    val result = placementApplicationService.getApplication(id)
    val placementApplication = extractEntityFromAuthorisableActionResult(result, id.toString(), "Application")

    return ResponseEntity.ok(placementApplicationTransformer.transformJpaToApi(placementApplication))
  }

  override fun placementApplicationsIdPut(
    id: UUID,
    updatePlacementApplication: UpdatePlacementApplication,
  ): ResponseEntity<PlacementApplication> {
    val serializedData = objectMapper.writeValueAsString(updatePlacementApplication.data)

    val result = placementApplicationService.updateApplication(id, serializedData)

    val validationResult = extractEntityFromAuthorisableActionResult(result, id.toString(), "Placement Application")
    val placementApplication = extractEntityFromValidatableActionResult(validationResult)

    return ResponseEntity.ok(placementApplicationTransformer.transformJpaToApi(placementApplication))
  }

  override fun placementApplicationsIdSubmissionPost(
    id: UUID,
    submitPlacementApplication: SubmitPlacementApplication,
  ): ResponseEntity<PlacementApplication> {
    val serializedData = objectMapper.writeValueAsString(submitPlacementApplication.translatedDocument)

    val result = placementApplicationService.submitApplication(id, serializedData)

    val validationResult = extractEntityFromAuthorisableActionResult(result, id.toString(), "Placement Application")
    val placementApplication = extractEntityFromValidatableActionResult(validationResult)

    return ResponseEntity.ok(placementApplicationTransformer.transformJpaToApi(placementApplication))
  }
}
