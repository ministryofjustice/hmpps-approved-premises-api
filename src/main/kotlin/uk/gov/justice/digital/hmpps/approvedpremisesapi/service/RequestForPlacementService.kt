package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RequestForPlacementTransformer
import java.util.UUID

@Component
class RequestForPlacementService(
  private val applicationService: ApplicationService,
  private val placementApplicationService: PlacementApplicationService,
  private val placementRequestService: PlacementRequestService,
  private val requestForPlacementTransformer: RequestForPlacementTransformer,
) {
  fun getRequestsForPlacementByApplication(applicationId: UUID): AuthorisableActionResult<List<RequestForPlacement>> {
    if (applicationService.getApplication(applicationId) == null) {
      return AuthorisableActionResult.NotFound("Application", applicationId.toString())
    }

    val placementApplications = placementApplicationService.getAllPlacementApplicationsForApplicationId(applicationId)
    val placementRequests = placementRequestService.getPlacementRequestForInitialApplicationDates(applicationId)

    val result = placementApplications.map(requestForPlacementTransformer::transformPlacementApplicationEntityToApi) +
      placementRequests.map(requestForPlacementTransformer::transformPlacementRequestEntityToApi)

    return AuthorisableActionResult.Success(result)
  }

  fun getRequestForPlacement(application: ApplicationEntity, requestForPlacementId: UUID): AuthorisableActionResult<RequestForPlacement> {
    val placementApplication = placementApplicationService.getApplicationOrNull(requestForPlacementId)
    val placementRequest = placementRequestService.getPlacementRequestOrNull(requestForPlacementId)

    return when {
      placementApplication == null && placementRequest == null ->
        AuthorisableActionResult.NotFound("RequestForPlacement", requestForPlacementId.toString())

      placementApplication != null && placementRequest != null ->
        throw RequestForPlacementServiceException.AmbiguousRequestForPlacementId()

      placementApplication != null -> when (placementApplication.application) {
        application ->
          AuthorisableActionResult.Success(requestForPlacementTransformer.transformPlacementApplicationEntityToApi(placementApplication))

        else ->
          AuthorisableActionResult.NotFound("RequestForPlacement", requestForPlacementId.toString())
      }

      else -> when (placementRequest!!.application) {
        application ->
          AuthorisableActionResult.Success(requestForPlacementTransformer.transformPlacementRequestEntityToApi(placementRequest))

        else ->
          AuthorisableActionResult.NotFound("RequestForPlacement", requestForPlacementId.toString())
      }
    }
  }

  private sealed class RequestForPlacementServiceException(message: String) : RuntimeException(message) {
    class AmbiguousRequestForPlacementId : RequestForPlacementServiceException("A placement application and placement request could not be distinguished as they share the same UUID")
  }
}
