package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RequestsForPlacementDurationsCalculationRequestDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RequestsForPlacementDurationsCalculationResponseDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RequestForPlacementTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import java.time.Period
import java.util.UUID

@Component
class Cas1RequestForPlacementService(
  private val applicationService: ApplicationService,
  private val cas1PlacementApplicationService: Cas1PlacementApplicationService,
  private val placementRequestService: Cas1PlacementRequestService,
  private val requestForPlacementTransformer: RequestForPlacementTransformer,
  private val cas1WithdrawableService: Cas1WithdrawableService,
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  private val cas1SpaceBookingTransformer: Cas1SpaceBookingTransformer,
) {
  fun getRequestsForPlacementByApplication(applicationId: UUID, requestingUser: UserEntity?): CasResult<List<RequestForPlacement>> {
    val application = applicationService.getApplication(applicationId)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    check(application is ApprovedPremisesApplicationEntity) { "Unsupported Application type: ${application::class.qualifiedName}" }

    val placementApplications = cas1PlacementApplicationService.getAllSubmittedNonReallocatedApplications(applicationId)
    val placementRequests = placementRequestService.getPlacementRequestForInitialApplicationDates(applicationId)

    val result =
      placementApplications.map { toRequestForPlacement(it, requestingUser) } +
        placementRequests.map { toRequestForPlacement(it, requestingUser) }

    return CasResult.Success(result.sortedByDescending { it.submittedAt })
  }

  fun defaultDurations(
    requestDto: Cas1RequestsForPlacementDurationsCalculationRequestDto,
  ): CasResult<Cas1RequestsForPlacementDurationsCalculationResponseDto> {
    if (requestDto.tier.version == TierVersionDto.V3) return CasResult.GeneralValidationError("Tier version V3 is not supported for duration calculations")
    val responseDto = when (requestDto.apType) {
      ApType.pipe -> Cas1RequestsForPlacementDurationsCalculationResponseDto(Period.ofWeeks(26).days, maxDurationDays = null)
      ApType.esap -> Cas1RequestsForPlacementDurationsCalculationResponseDto(Period.ofWeeks(52).days, maxDurationDays = null)
      ApType.normal,
      ApType.rfap,
      ApType.mhapStJosephs,
      ApType.mhapElliottHouse,
      -> Cas1RequestsForPlacementDurationsCalculationResponseDto(Period.ofWeeks(12).days, maxDurationDays = null)
    }
    return CasResult.Success(responseDto)
  }

  private fun toRequestForPlacement(placementApplication: PlacementApplicationEntity, user: UserEntity?) = requestForPlacementTransformer.transformPlacementApplicationEntityToApi(
    placementApplication,
    user?.let { cas1WithdrawableService.isDirectlyWithdrawable(placementApplication, user) } ?: false,
  ).withPlacementsFrom(placementApplication.placementRequest)

  private fun toRequestForPlacement(placementRequest: PlacementRequestEntity, user: UserEntity?): RequestForPlacement = requestForPlacementTransformer
    .transformPlacementRequestEntityToApi(
      placementRequest,
      user?.let { cas1WithdrawableService.isDirectlyWithdrawable(placementRequest, user) } ?: false,
    ).withPlacementsFrom(placementRequest)

  private fun RequestForPlacement.withPlacementsFrom(placementRequest: PlacementRequestEntity?) = apply {
    placementRequest?.let { pr ->
      placements = cas1SpaceBookingRepository
        .findByPlacementRequestId(pr.id)
        .map { cas1SpaceBookingTransformer.transformToCas1SpaceBookingShortSummary(it) }
    }
  }
}
