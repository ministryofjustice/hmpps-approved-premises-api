package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RequestsForPlacementDurationsCalculationResponseDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CaseService
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
  private val applicationService: Cas1ApplicationService,
  private val cas1PlacementApplicationService: Cas1PlacementApplicationService,
  private val placementRequestService: Cas1PlacementRequestService,
  private val requestForPlacementTransformer: RequestForPlacementTransformer,
  private val cas1WithdrawableService: Cas1WithdrawableService,
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  private val cas1SpaceBookingTransformer: Cas1SpaceBookingTransformer,
  private val caseService: CaseService,
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
    applicationId: UUID,
    apType: ApType,
  ): CasResult<Cas1RequestsForPlacementDurationsCalculationResponseDto> {

    val application = applicationService.getApplication(applicationId)
      ?: return CasResult.NotFound("Application", applicationId.toString())
    val liveTier = (caseService.getCase(application.crn) ?: return CasResult.NotFound("Case", application.crn)).tier
    val liveTierVersion = liveTier?.version
      ?: return CasResult.NotFound("Version for live tier associated with case CRN", application.crn)

    return when (liveTierVersion) {
      TierVersionDto.V2 -> CasResult.Success(
        when (apType) {
          ApType.pipe -> Cas1RequestsForPlacementDurationsCalculationResponseDto(Period.ofWeeks(26).days, maxDurationDays = null)
          ApType.esap -> Cas1RequestsForPlacementDurationsCalculationResponseDto(Period.ofWeeks(52).days, maxDurationDays = null)
          ApType.normal,
          ApType.rfap,
          ApType.mhapStJosephs,
          ApType.mhapElliottHouse,
          -> Cas1RequestsForPlacementDurationsCalculationResponseDto(Period.ofWeeks(12).days, maxDurationDays = null)
        },
      )

      TierVersionDto.V3 -> {
        when (apType) {
          ApType.pipe -> CasResult.Success(
            Cas1RequestsForPlacementDurationsCalculationResponseDto(Period.ofWeeks(26).days, maxDurationDays = null),
          )

          ApType.esap -> CasResult.Success(
            Cas1RequestsForPlacementDurationsCalculationResponseDto(Period.ofWeeks(62).days, maxDurationDays = null),
          )

          ApType.mhapStJosephs,
          ApType.mhapElliottHouse,
          -> if (application.isWomensApplication == true) {
            CasResult.GeneralValidationError("MHAP not supported for women's applications")
          } else {
            CasResult.Success(
              Cas1RequestsForPlacementDurationsCalculationResponseDto(Period.ofWeeks(26).days, maxDurationDays = null),
            )
          }

          ApType.normal,
          ApType.rfap,
          -> if (application.isWomensApplication== true) {
            CasResult.Success(
              Cas1RequestsForPlacementDurationsCalculationResponseDto(Period.ofWeeks(16).days, maxDurationDays = null),
            )
          } else {
            if (application.sentenceType == SentenceTypeOption.life.value || application.sentenceType == SentenceTypeOption.ipp.value) {
              if (liveTier.tierScore in listOf("A", "B", "C"))
              CasResult.Success(
                Cas1RequestsForPlacementDurationsCalculationResponseDto(Period.ofWeeks(16).days, maxDurationDays = null),
              )
              else {
                CasResult.GeneralValidationError("Only tier A, B or C is eligible for life and ipp sentence type")
              }
            }
            else if (
              application.sentenceType == SentenceTypeOption.standardDeterminate.value
              || application.sentenceType == SentenceTypeOption.extendedDeterminate.value
              || application.sentenceType == SentenceTypeOption.communityOrder.value
              || application.sentenceType == SentenceTypeOption.bailPlacement.value
              || application.sentenceType == SentenceTypeOption.nonStatutory.value) {
              if (liveTier.tierScore == "A") {
                CasResult.Success(
                  Cas1RequestsForPlacementDurationsCalculationResponseDto(Period.ofWeeks(16).days, maxDurationDays = null),
                )
              } else if (liveTier.tierScore == "B") {
                CasResult.Success(
                  Cas1RequestsForPlacementDurationsCalculationResponseDto(Period.ofWeeks(12).days, maxDurationDays = null),
                )
              } else if (liveTier.tierScore == "C" || liveTier.tierScore == "D") {
                CasResult.Success(
                  Cas1RequestsForPlacementDurationsCalculationResponseDto(Period.ofWeeks(8).days, maxDurationDays = null),
                )
              } else if (liveTier.tierScore in listOf("E", "F", "G")){
                CasResult.GeneralValidationError("Cannot calculate duration for ap type $apType, sentence type ${application.sentenceType}, tier score ${liveTier.tierScore}")
              }
              else {
                TODO()
              }
            }
            else {TODO()}
          }
        }
      }
    }
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
