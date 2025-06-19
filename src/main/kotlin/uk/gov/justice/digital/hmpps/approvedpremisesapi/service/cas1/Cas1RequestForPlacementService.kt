package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.Cas1PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RequestForPlacementTransformer
import java.util.UUID

@Component
class Cas1RequestForPlacementService(
  private val applicationService: ApplicationService,
  private val cas1PlacementApplicationService: Cas1PlacementApplicationService,
  private val placementRequestService: PlacementRequestService,
  private val requestForPlacementTransformer: RequestForPlacementTransformer,
  private val cas1WithdrawableService: Cas1WithdrawableService,
) {
  fun getRequestsForPlacementByApplication(applicationId: UUID, requestingUser: UserEntity): CasResult<List<RequestForPlacement>> {
    val application = applicationService.getApplication(applicationId)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    check(application is ApprovedPremisesApplicationEntity) { "Unsupported Application type: ${application::class.qualifiedName}" }

    val placementApplications = cas1PlacementApplicationService.getAllSubmittedNonReallocatedApplications(applicationId)
    val placementRequests = placementRequestService.getPlacementRequestForInitialApplicationDates(applicationId)

    val result =
      placementApplications.map { toRequestForPlacement(it, requestingUser) } +
        placementRequests.map { toRequestForPlacement(it, requestingUser) }

    return CasResult.Success(result)
  }

  private fun toRequestForPlacement(placementApplication: PlacementApplicationEntity, user: UserEntity) = requestForPlacementTransformer.transformPlacementApplicationEntityToApi(
    placementApplication,
    cas1WithdrawableService.isDirectlyWithdrawable(placementApplication, user),
  )

  private fun toRequestForPlacement(placementRequest: PlacementRequestEntity, user: UserEntity) = requestForPlacementTransformer.transformPlacementRequestEntityToApi(
    placementRequest,
    cas1WithdrawableService.isDirectlyWithdrawable(placementRequest, user),
  )

  private sealed class RequestForPlacementServiceException(message: String) : RuntimeException(message) {
    class AmbiguousRequestForPlacementId : RequestForPlacementServiceException("A placement application and placement request could not be distinguished as they share the same UUID")
  }
}
