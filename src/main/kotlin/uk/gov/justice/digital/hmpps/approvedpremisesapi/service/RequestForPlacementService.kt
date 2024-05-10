package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RequestForPlacementTransformer
import java.util.UUID

@Component
class RequestForPlacementService(
  private val applicationService: ApplicationService,
  private val placementApplicationService: PlacementApplicationService,
  private val placementRequestService: PlacementRequestService,
  private val requestForPlacementTransformer: RequestForPlacementTransformer,
  private val cas1WithdrawableService: Cas1WithdrawableService,
) {
  fun getRequestsForPlacementByApplication(applicationId: UUID, requestingUser: UserEntity): AuthorisableActionResult<List<RequestForPlacement>> {
    val application = applicationService.getApplication(applicationId)
      ?: return AuthorisableActionResult.NotFound("Application", applicationId.toString())

    check(application is ApprovedPremisesApplicationEntity) { "Unsupported Application type: ${application::class.qualifiedName}" }

    val placementApplications = placementApplicationService.getAllSubmittedNonReallocatedApplications(applicationId)
    val placementRequests = placementRequestService.getPlacementRequestForInitialApplicationDates(applicationId)

    val result =
      placementApplications.map { toRequestForPlacement(it, requestingUser) } +
        placementRequests.map { toRequestForPlacement(it, requestingUser) }

    return AuthorisableActionResult.Success(result)
  }

  fun getRequestForPlacement(application: ApplicationEntity, requestForPlacementId: UUID, requestingUser: UserEntity): AuthorisableActionResult<RequestForPlacement> {
    check(application is ApprovedPremisesApplicationEntity) { "Unsupported Application type: ${application::class.qualifiedName}" }

    val placementApplication = placementApplicationService.getApplicationOrNull(requestForPlacementId)
    val placementRequest = placementRequestService.getPlacementRequestOrNull(requestForPlacementId)

    return when {
      placementApplication == null && placementRequest == null ->
        AuthorisableActionResult.NotFound("RequestForPlacement", requestForPlacementId.toString())

      placementApplication != null && placementRequest != null ->
        throw RequestForPlacementServiceException.AmbiguousRequestForPlacementId()

      placementApplication != null -> when (placementApplication.application) {
        application ->
          AuthorisableActionResult.Success(toRequestForPlacement(placementApplication, requestingUser))

        else ->
          AuthorisableActionResult.NotFound("RequestForPlacement", requestForPlacementId.toString())
      }

      else -> when (placementRequest!!.application) {
        application ->
          AuthorisableActionResult.Success(toRequestForPlacement(placementRequest, requestingUser))

        else ->
          AuthorisableActionResult.NotFound("RequestForPlacement", requestForPlacementId.toString())
      }
    }
  }

  private fun toRequestForPlacement(placementApplication: PlacementApplicationEntity, user: UserEntity) =
    requestForPlacementTransformer.transformPlacementApplicationEntityToApi(
      placementApplication,
      cas1WithdrawableService.isDirectlyWithdrawable(placementApplication, user),
    )

  private fun toRequestForPlacement(placementRequest: PlacementRequestEntity, user: UserEntity) =
    requestForPlacementTransformer.transformPlacementRequestEntityToApi(
      placementRequest,
      cas1WithdrawableService.isDirectlyWithdrawable(placementRequest, user),
    )

  private sealed class RequestForPlacementServiceException(message: String) : RuntimeException(message) {
    class AmbiguousRequestForPlacementId : RequestForPlacementServiceException("A placement application and placement request could not be distinguished as they share the same UUID")
  }
}
