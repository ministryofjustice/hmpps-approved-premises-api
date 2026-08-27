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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierV3Score
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.TierService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
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
  private val sentryService: SentryService,
) {
  companion object {
    val TIER_SCORE_ABC = listOf(
      TierV3Score.A,
      TierV3Score.B,
      TierV3Score.C,
    )

    val TIER_SCORE_D_OR_BELOW = listOf(
      TierV3Score.D,
      TierV3Score.E,
      TierV3Score.F,
      TierV3Score.G,
    )
  }

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
    sentenceType: String,
    exceptionalApplication: Boolean = false,
  ): CasResult<Cas1RequestsForPlacementDurationsCalculationResponseDto> {
    val application = applicationService.getApplication(applicationId) ?: return CasResult.NotFound("Application", applicationId.toString())
    val tier = tierService.getTier(application.crn) ?: return CasResult.NotFound("Tier associated with case CRN", application.crn)
    val durationCriteria = DurationCriteria(apType, application, sentenceType, tier, exceptionalApplication)

    return CasResult.Success(
      when (tier.version) {
        TierVersionDto.V2 -> defaultDurationTierV2(durationCriteria)
        TierVersionDto.V3 -> defaultDurationTierV3(durationCriteria)
      },
    )
  }

  private data class DurationCriteria(
    val apType: ApType,
    val application: ApprovedPremisesApplicationEntity,
    val sentenceType: String,
    val liveTier: TierDto,
    val exceptionalApplication: Boolean,
  )

  @SuppressWarnings("MagicNumber")
  private fun defaultDurationTierV2(
    criteria: DurationCriteria,
  ): Cas1RequestsForPlacementDurationsCalculationResponseDto {
    val period = when (criteria.apType) {
      ApType.pipe -> Period.ofWeeks(26)
      ApType.esap -> Period.ofWeeks(52)
      ApType.normal,
      ApType.rfap,
      ApType.mhapStJosephs,
      ApType.mhapElliottHouse,
      -> Period.ofWeeks(12)
    }

    return Cas1RequestsForPlacementDurationsCalculationResponseDto(period?.days, null)
  }

  @SuppressWarnings("MagicNumber", "CyclomaticComplexMethod")
  private fun defaultDurationTierV3(
    criteria: DurationCriteria,
  ): Cas1RequestsForPlacementDurationsCalculationResponseDto {
    val apType = criteria.apType
    val tierScore = TierV3Score.entries.firstOrNull { it.name == criteria.liveTier.tierScore }
      ?: error("Could not resolve tier value ${criteria.liveTier.tierScore}")
    val isIpp = criteria.sentenceType == SentenceTypeOption.ipp.value
    val male = !(criteria.application.isWomensApplication!!)

    val tierAbc = tierScore in TIER_SCORE_ABC
    val tierDOrBelowWithException = tierScore in TIER_SCORE_D_OR_BELOW && criteria.exceptionalApplication

    fun maleIppRestrictedRule(period: Period) = if (isIpp && male) {
      if (tierAbc) {
        period
      } else {
        null
      }
    } else if (tierAbc || tierDOrBelowWithException) {
      period
    } else {
      null
    }

    val period = when (apType) {
      ApType.pipe -> maleIppRestrictedRule(Period.ofWeeks(26))
      ApType.esap -> maleIppRestrictedRule(Period.ofWeeks(52))

      ApType.mhapStJosephs,
      ApType.mhapElliottHouse,
      -> {
        if (male) {
          maleIppRestrictedRule(Period.ofWeeks(26))
        } else {
          null
        }
      }

      ApType.normal,
      ApType.rfap,
      -> {
        if (isIpp) {
          maleIppRestrictedRule(Period.ofWeeks(16))
        } else {
          if (male) {
            if (tierScore == TierV3Score.A) {
              Period.ofWeeks(16)
            } else if (tierScore == TierV3Score.B) {
              Period.ofWeeks(12)
            } else if (tierScore == TierV3Score.C) {
              Period.ofWeeks(8)
            } else if (tierDOrBelowWithException) {
              Period.ofWeeks(8)
            } else {
              null
            }
          } else {
            if (tierAbc || tierDOrBelowWithException) {
              Period.ofWeeks(16)
            } else {
              null
            }
          }
        }
      }
    }

    if (period == null) {
      sentryService.captureErrorMessage("Could not calculate duration for criteria $criteria")
    }

    return Cas1RequestsForPlacementDurationsCalculationResponseDto(period?.days, null)
  }

  fun getRequestForPlacementWithdrawalDate(rfp: RequestForPlacement) = domainEventRepository.findWithdrawnRequestForPlacement(rfp.id)?.occurredAt?.toLocalDate()

  fun getRequestForPlacementRejectionReason(rfp: RequestForPlacement) = domainEventRepository.findAssessedRequestForPlacement(rfp.id)?.let {
    jsonMapper.readValue(it.data, RequestForPlacementAssessedEnvelope::class.java)
  }?.eventDetails?.decisionSummary

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
