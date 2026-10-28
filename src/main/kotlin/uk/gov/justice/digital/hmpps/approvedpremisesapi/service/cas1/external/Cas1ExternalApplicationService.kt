package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPremisesDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RequestForPlacementAndPlacementDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
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

  companion object {
    /**
     * High number = Higher Priority
     */
    @SuppressWarnings("MagicNumber")
    private val APPLICATION_STATUS_TO_SUITABILITY = mapOf(
      ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED to 11,
      ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT to 10,
      ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST to 9,
      ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION to 8,
      ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS to 7,
      ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT to 6,
      ApprovedPremisesApplicationStatus.UNALLOCATED_ASSESSMENT to 5,
      ApprovedPremisesApplicationStatus.STARTED to 4,
      ApprovedPremisesApplicationStatus.REJECTED to 3,
      ApprovedPremisesApplicationStatus.WITHDRAWN to 2,
      ApprovedPremisesApplicationStatus.EXPIRED to 1,
      ApprovedPremisesApplicationStatus.INAPPLICABLE to 0,
    )
  }

  private val applicationSuitabilityComparator = compareBy<ApprovedPremisesApplicationEntity> { APPLICATION_STATUS_TO_SUITABILITY[it.status] }
    .thenBy { it.submittedAt ?: it.createdAt }

  fun getRequestsForPlacementAndPlacements(applicationId: UUID): List<Cas1RequestForPlacementAndPlacementDto> {
    val rfps = (cas1RequestForPlacementService.getRequestsForPlacementByApplication(applicationId, requestingUser = null) as CasResult.Success).value

    return rfps.flatMap { rfp ->
      val withdrawalDate = if (rfp.isWithdrawn) rfp.withdrawalDate?.toLocalDate() else null

      if (rfp.placements.isEmpty()) {
        val rejectionReason = rfp.rejectionReason

        listOf(
          transformer.transformToRequestForPlacementAndPlacement(
            rfp = rfp,
            rejectionReason = rejectionReason,
            withdrawalDate = withdrawalDate,
          ),
        )
      } else {
        rfp.placements.map { placement ->
          val premisesEntity = cas1PremisesService.findPremisesById(placement.premises.id)

          transformer.transformToRequestForPlacementAndPlacement(
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
    .maxWithOrNull(applicationSuitabilityComparator)
    ?.let { application ->
      val requestsForPlacementAndPlacements = getRequestsForPlacementAndPlacements(application.id)

      val today = LocalDate.now()

      val pastRequestsForPlacementAndPlacements = requestsForPlacementAndPlacements
        .filter { it.dateApplied < today }
        .sortedByDescending { it.dateApplied }

      val suitableRequestForPlacementAndPlacement = requestsForPlacementAndPlacements
        .lastOrNull { it.dateApplied >= today }
        ?: pastRequestsForPlacementAndPlacements.firstOrNull()

      val placementHistory = pastRequestsForPlacementAndPlacements
        .filterNot { it == suitableRequestForPlacementAndPlacement }

      transformer.transformToCas1SuitableApplication(
        application = application,
        suitableRequestForPlacementAndPlacement = suitableRequestForPlacementAndPlacement,
        placementHistory = placementHistory,
      )
    }

  fun getCurrentPremisesByCrn(crn: String): Cas1ExternalPremisesDto? = approvedPremisesApplicationRepository.findByCrn(crn)
    .sortedWith(applicationSuitabilityComparator).firstNotNullOfOrNull { application ->
      getRequestsForPlacementAndPlacements(application.id)
        .firstOrNull { it.placement?.status == Cas1SpaceBookingStatus.ARRIVED }?.placement?.premises
    }
}
