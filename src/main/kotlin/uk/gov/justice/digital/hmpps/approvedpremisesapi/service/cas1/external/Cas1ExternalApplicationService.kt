package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPremisesDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1PlacementPairDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external.Cas1ExternalApplicationTransformer
import java.time.LocalDate
import java.util.UUID

@SuppressWarnings("TooGenericExceptionThrown")
@Service
class Cas1ExternalApplicationService(
  private val approvedPremisesApplicationRepository: ApprovedPremisesApplicationRepository,
  private val cas1PlacementPairService: Cas1PlacementPairService,
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

  fun getSuitableApplicationByCrn(crn: String): Cas1SuitableApplication? = approvedPremisesApplicationRepository
    .findByCrn(crn)
    .maxWithOrNull(applicationSuitabilityComparator)
    ?.let { toSuitableApplication(it) }

  private fun toSuitableApplication(application: ApprovedPremisesApplicationEntity): Cas1SuitableApplication {
    val placementPairs = cas1PlacementPairService.getPlacementPairs(application.id)

    val today = LocalDate.now()

    val pastPairs = placementPairs
      .filter { it.dateApplied < today }
      .sortedByDescending { it.dateApplied }

    val suitablePlacementPair = placementPairs
      .lastOrNull { it.dateApplied >= today }
      ?: pastPairs.firstOrNull()

    val placementHistory = pastPairs
      .filterNot { it == suitablePlacementPair }

    return transformer.transformToCas1SuitableApplication(
      application = application,
      suitablePlacementPair = suitablePlacementPair,
      placementHistory = placementHistory,
    )
  }

  fun getPlacementPairs(applicationId: UUID): List<Cas1PlacementPairDto> = cas1PlacementPairService.getPlacementPairs(applicationId)

  fun getCurrentPremisesByCrn(crn: String): Cas1ExternalPremisesDto? = approvedPremisesApplicationRepository
    .findByCrn(crn)
    .sortedWith(applicationSuitabilityComparator)
    .firstNotNullOfOrNull { application ->
      cas1PlacementPairService.getPlacementPairs(application.id).firstOrNull { it.placement?.status == Cas1SpaceBookingStatus.ARRIVED }?.placement?.premises
    }
}
