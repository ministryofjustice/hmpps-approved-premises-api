package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalApplicationV2Dto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPlacementV2Dto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementOutcomeDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1RequestForPlacementOutcomeV2Service.RequestForPlacementOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external.Cas1ExternalApplicationV2Transformer
import java.time.Clock
import java.time.LocalDate

@Service
class Cas1ExternalApplicationV2Service(
  private val approvedPremisesApplicationRepository: ApprovedPremisesApplicationRepository,
  private val requestForPlacementOutcomeV2Service: Cas1RequestForPlacementOutcomeV2Service,
  private val transformer: Cas1ExternalApplicationV2Transformer,
  private val clock: Clock,
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

  fun getSuitableApplicationByCrn(crn: String): Cas1ExternalApplicationV2Dto? = approvedPremisesApplicationRepository
    .findByCrn(crn)
    .maxWithOrNull(applicationSuitabilityComparator)
    ?.let { toExternalApplicationV2(it) }

  private fun toExternalApplicationV2(application: ApprovedPremisesApplicationEntity): Cas1ExternalApplicationV2Dto {
    val outcomes = requestForPlacementOutcomeV2Service.getOutcomes(application.id)

    val today = LocalDate.now(clock)

    val pendingArrivals = outcomes.filter { outcome -> outcome.pendingArrivalDate()?.let { it >= today } == true }

    val upcoming = pendingArrivals.minByOrNull { it.pendingArrivalDate()!! }

    val history = (outcomes - pendingArrivals.toSet())
      .sortedByDescending { it.statusLastUpdated }
      .map { it.toDto() }

    return transformer.transformToApplicationV2(
      application = application,
      upcomingRequestForPlacement = upcoming?.toDto(),
      requestForPlacementHistory = history,
    )
  }

  private fun RequestForPlacementOutcome.toDto() = Cas1ExternalRequestForPlacementOutcomeDto(requestForPlacement, placement)

  private fun RequestForPlacementOutcome.pendingArrivalDate() = placement?.pendingArrivalDate()

  private fun Cas1ExternalPlacementV2Dto.pendingArrivalDate() = if (status == Cas1SpaceBookingStatus.UPCOMING) expectedArrivalDate else null
}
