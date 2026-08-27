package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RequestsForPlacementDurationsCalculationResponseDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.TierService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RequestForPlacementTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import java.time.Period
import java.util.UUID

@Component
class Cas1RequestForPlacementService(
  private val applicationService: Cas1ApplicationService,
  private val cas1PlacementApplicationService: Cas1PlacementApplicationService,
  private val placementRequestService: Cas1PlacementRequestService,
  private val requestForPlacementTransformer: RequestForPlacementTransformer,
  private val cas1WithdrawableService: Cas1WithdrawableService,
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  private val cas1SpaceBookingTransformer: Cas1SpaceBookingTransformer,
  private val domainEventRepository: DomainEventRepository,
  private val tierService: TierService,
  private val jsonMapper: JsonMapper,
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

  @SuppressWarnings("UnusedParameter")
  fun defaultDurations(
    applicationId: UUID,
    apType: ApType,
    sentenceType: String,
    exceptionalApplication: Boolean = false,
  ): CasResult<Cas1RequestsForPlacementDurationsCalculationResponseDto> {
    val application = applicationService.getApplication(applicationId) ?: return CasResult.NotFound("Application", applicationId.toString())
    val tier = tierService.getTier(application.crn) ?: return CasResult.NotFound("Tier associated with case CRN", application.crn)

    return when (tier.version) {
      TierVersionDto.V2 -> defaultDurationTierV2(apType)
      TierVersionDto.V3 -> defaultDurationTierV3(apType, application, sentenceType, tier)
    }
  }

  @SuppressWarnings("MagicNumber")
  private fun defaultDurationTierV2(
    apType: ApType,
  ) = when (apType) {
    ApType.pipe -> createDurationCalculation(Period.ofWeeks(26), null)
    ApType.esap -> createDurationCalculation(Period.ofWeeks(52), null)
    ApType.normal,
    ApType.rfap,
    ApType.mhapStJosephs,
    ApType.mhapElliottHouse,
    -> createDurationCalculation(Period.ofWeeks(12), null)
  }

  @SuppressWarnings("MagicNumber", "CyclomaticComplexMethod")
  private fun defaultDurationTierV3(
    apType: ApType,
    application: ApprovedPremisesApplicationEntity,
    sentenceType: String,
    liveTier: TierDto,
  ): CasResult<Cas1RequestsForPlacementDurationsCalculationResponseDto> {
    val tierScore = liveTier.tierScore

    return when (apType) {
      ApType.pipe -> createDurationCalculation(Period.ofWeeks(26))
      ApType.esap -> createDurationCalculation(Period.ofWeeks(62))
      ApType.mhapStJosephs,
      ApType.mhapElliottHouse,
      -> if (application.isWomensApplication == true) {
        CasResult.GeneralValidationError("MHAP not supported for women's applications")
      } else {
        createDurationCalculation(Period.ofWeeks(26))
      }

      ApType.normal,
      ApType.rfap,
      -> if (application.isWomensApplication == true) {
        createDurationCalculation(Period.ofWeeks(16))
      } else {
        if (sentenceType == SentenceTypeOption.life.value || sentenceType == SentenceTypeOption.ipp.value) {
          if (tierScore in listOf("A", "B", "C")) {
            createDurationCalculation(Period.ofWeeks(16))
          } else {
            CasResult.GeneralValidationError("Only tier A, B or C is eligible for life and ipp sentence type")
          }
        } else {
          when (tierScore) {
            "A" -> createDurationCalculation(Period.ofWeeks(16))
            "B" -> createDurationCalculation(Period.ofWeeks(12))
            "C", "D" -> createDurationCalculation(Period.ofWeeks(8))
            "E", "F", "G" -> CasResult.GeneralValidationError("Cannot calculate duration for ap type $apType, sentence type $sentenceType, tier score $tierScore")
            else -> CasResult.GeneralValidationError("Cannot calculate duration for ap type $apType, sentence type $sentenceType, tier score $tierScore")
          }
        }
      }
    }
  }

  fun getRequestForPlacementWithdrawalDate(rfp: RequestForPlacement) = domainEventRepository.findWithdrawnRequestForPlacement(rfp.id)?.occurredAt?.toLocalDate()

  fun getRequestForPlacementRejectionReason(rfp: RequestForPlacement) = domainEventRepository.findAssessedRequestForPlacement(rfp.id)?.let {
    jsonMapper.readValue(it.data, RequestForPlacementAssessedEnvelope::class.java)
  }?.eventDetails?.decisionSummary

  @SuppressWarnings("MaxLineLength")
  private fun createDurationCalculation(period: Period, maxDurationDays: Int? = null): CasResult.Success<Cas1RequestsForPlacementDurationsCalculationResponseDto> = CasResult.Success(Cas1RequestsForPlacementDurationsCalculationResponseDto(period.days, maxDurationDays))

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
