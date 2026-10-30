package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPlacementDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPremisesDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RequestForPlacementOutcomeDto
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
  private val cas1PlacementPairService: Cas1RequestForPlacementOutcomeService,
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

  fun getSuitableApplicationByCrn(crn: String): Cas1SuitableApplication? = getSuitableApplication(crn)
    ?.let { toSuitableApplication(it) }

  private fun toSuitableApplication(application: ApprovedPremisesApplicationEntity): Cas1SuitableApplication {
    val placementPairs = cas1PlacementPairService.getPlacementPairs(application.id).sortedByDescending { it.dateApplied }

    val today = LocalDate.now()

    // this filters out any with a pending arrival in the future or that have been updated today
    val pastPairs = placementPairs
      .filter { it.dateApplied < today }

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

  private fun getSuitableApplicationByCrnV2(crn: String) = getSuitableApplication(crn) ?.let {
    toSuitableApplicationV2(it)
  }

  /*
  Some unexpected scenarios this creates (matching existing logic)

  - if you have a pending arrival in the past (e.g. they didn't turn up on the day), this goes into the 'history' list
  - history won't include anything that doesn't have a pending arrival that has been updated today
   */
  private fun toSuitableApplicationV2(application: ApprovedPremisesApplicationEntity): Cas1SuitableApplication {
    val all = cas1PlacementPairService.getPlacementPairs(application.id)

    val today = LocalDate.now()

    val allWithFuturePendingArrival = all.filter { rfpOutcome ->
      rfpOutcome.pendingArrivalDate()?.let {
        it >= today
      } ?: false
    }

    val nextPendingArrival = allWithFuturePendingArrival.sortedBy { it.pendingArrivalDate() }.firstOrNull()

    val allWithoutAFuturePendingArrival = all - allWithFuturePendingArrival.toSet()

    return transformer.transformToCas1SuitableApplication(
      application = application,
      suitablePlacementPair = nextPendingArrival ?: allWithoutAFuturePendingArrival.firstOrNull(),
      placementHistory = allWithoutAFuturePendingArrival.sortedByDescending { it.statusLastUpdated },
    )
  }

  private fun Cas1RequestForPlacementOutcomeDto.pendingArrivalDate() = placement?.pendingArrivalDate() ?: requestForPlacement.pendingArrivalDate()

  private fun Cas1ExternalRequestForPlacementDto.pendingArrivalDate() = if (
    listOf(
      RequestForPlacementStatus.requestSubmitted,
      RequestForPlacementStatus.awaitingMatch,
    ).contains(status)
  ) {
    expectedArrivalDate
  } else {
    null
  }

  private fun Cas1ExternalPlacementDto.pendingArrivalDate() = if (status == Cas1SpaceBookingStatus.UPCOMING) {
    expectedArrivalDate
  } else {
    null
  }

  private fun getSuitableApplication(crn: String) = approvedPremisesApplicationRepository
    .findByCrn(crn)
    .maxWithOrNull(applicationSuitabilityComparator)

  fun getPlacementPairsNewestFirst(applicationId: UUID): List<Cas1RequestForPlacementOutcomeDto> = cas1PlacementPairService
    .getPlacementPairs(applicationId)
    .sortedByDescending { it.dateApplied }

  fun getCurrentPremisesByCrn(crn: String): Cas1ExternalPremisesDto? = approvedPremisesApplicationRepository
    .findByCrn(crn)
    .sortedWith(applicationSuitabilityComparator)
    .firstNotNullOfOrNull { application ->
      cas1PlacementPairService.getPlacementPairs(application.id)
        .sortedByDescending { it.dateApplied }
        .firstOrNull { it.placement?.status == Cas1SpaceBookingStatus.ARRIVED }?.placement?.premises
    }
}
