package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPremisesDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1PlacementPairDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external.Cas1ExternalApplicationTransformer
import java.time.LocalDate
import java.util.UUID

@SuppressWarnings("TooGenericExceptionThrown")
@Service
class Cas1ExternalApplicationService(
  private val approvedPremisesApplicationRepository: ApprovedPremisesApplicationRepository,
  private val cas1RequestForPlacementService: Cas1RequestForPlacementService,
  private val cas1PremisesService: Cas1PremisesService,
  private val transformer: Cas1ExternalApplicationTransformer,
) {

  private val mostSuitableApplication = compareBy<ApprovedPremisesApplicationEntity> { suitableStatusesAsc[it.status] }
    .thenBy { it.submittedAt ?: it.createdAt }

  fun getPlacementPairs(applicationId: UUID): List<Cas1PlacementPairDto> {
    val rfps = (cas1RequestForPlacementService.getRequestsForPlacementByApplication(applicationId, requestingUser = null) as CasResult.Success).value

    return rfps.flatMap { rfp ->
      val withdrawalDate = if (rfp.isWithdrawn) cas1RequestForPlacementService.getRequestForPlacementWithdrawalDate(rfp) else null

      if (rfp.placements.isEmpty()) {
        val rejectionReason = if (rfp.decision == PlacementApplicationDecision.REJECTED) cas1RequestForPlacementService.getRequestForPlacementRejectionReason(rfp) else null

        listOf(
          transformer.transformToCas1PlacementPair(
            rfp = rfp,
            rejectionReason = rejectionReason,
            withdrawalDate = withdrawalDate,
          ),
        )
      } else {
        rfp.placements.map { placement ->
          val premisesEntity = cas1PremisesService.findPremisesById(placement.premises.id)

          transformer.transformToCas1PlacementPair(
            rfp = rfp,
            rejectionReason = null,
            withdrawalDate = withdrawalDate,
            placement = placement,
            premises = premisesEntity,
          )
        }
      }
    }.sortedByDescending { it.dateApplied }
  }

  fun getSuitableApplicationByCrn(crn: String): Cas1SuitableApplication? = approvedPremisesApplicationRepository.findByCrn(crn)
    .maxWithOrNull(mostSuitableApplication)
    ?.let { application ->
      val placementPairs = getPlacementPairs(application.id)

      val today = LocalDate.now()

      val pastPairs = placementPairs
        .filter { it.dateApplied < today }
        .sortedByDescending { it.dateApplied }

      val suitablePlacementPair = placementPairs
        .lastOrNull { it.dateApplied >= today }
        ?: pastPairs.firstOrNull()

      val placementHistory = pastPairs
        .filterNot { it == suitablePlacementPair }

      transformer.transformToCas1SuitableApplication(
        application = application,
        suitablePlacementPair = suitablePlacementPair,
        placementHistory = placementHistory,
      )
    }

  fun getCurrentPremisesByCrn(crn: String): Cas1ExternalPremisesDto? = approvedPremisesApplicationRepository.findByCrn(crn)
    .sortedWith(mostSuitableApplication).firstNotNullOfOrNull { application ->
      getPlacementPairs(application.id)
        .firstOrNull { it.placement?.status == Cas1SpaceBookingStatus.ARRIVED }?.placement?.premises
    }

  @SuppressWarnings("MagicNumber")
  private val suitableStatusesAsc = mapOf(
    ApprovedPremisesApplicationStatus.INAPPLICABLE to 0,
    ApprovedPremisesApplicationStatus.EXPIRED to 1,
    ApprovedPremisesApplicationStatus.WITHDRAWN to 2,
    ApprovedPremisesApplicationStatus.REJECTED to 3,
    ApprovedPremisesApplicationStatus.STARTED to 4,
    ApprovedPremisesApplicationStatus.UNALLOCATED_ASSESSMENT to 5,
    ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT to 6,
    ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS to 7,
    ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION to 8,
    ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST to 9,
    ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT to 10,
    ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED to 11,
  )
}
