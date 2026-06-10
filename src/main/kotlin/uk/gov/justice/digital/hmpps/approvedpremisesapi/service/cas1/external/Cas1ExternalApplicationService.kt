package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ExternalPremisesDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import java.time.LocalDate
import java.util.UUID

@SuppressWarnings("TooGenericExceptionThrown")
@Service
class Cas1ExternalApplicationService(
  private val approvedPremisesApplicationRepository: ApprovedPremisesApplicationRepository,
  private val cas1RequestForPlacementService: Cas1RequestForPlacementService,
  private val cas1PremisesService: Cas1PremisesService,
) {

  private val mostSuitableApplication = compareBy<ApprovedPremisesApplicationEntity> { suitableStatusesAsc[it.status] }
    .thenBy { it.submittedAt ?: it.createdAt }

  fun getPlacementHistories(applicationId: UUID): List<Cas1PlacementHistory> {
    val rfps = (cas1RequestForPlacementService.getRequestsForPlacementByApplication(applicationId, requestingUser = null) as CasResult.Success).value

    return rfps.flatMap { rfp ->

      if (rfp.placements.isEmpty()) {
        listOf(
          Cas1PlacementHistory(
            dateApplied = rfp.statusSetDate,
            requestForPlacementStatus = rfp.status,
            placementStatus = null,
            premises = null,
          ),
        )
      } else {
        rfp.placements.map { placement ->
          val premises = cas1PremisesService.findPremisesById(placement.premises.id)
            ?.let {
              Cas1ExternalPremisesDto(
                startDate = placement.actualArrivalDate ?: placement.expectedArrivalDate,
                endDate = placement.actualDepartureDate ?: placement.expectedDepartureDate,
                postcode = it.postcode,
                addressLine1 = it.addressLine1,
                addressLine2 = it.addressLine2,
                town = it.town,
              )
            }

          Cas1PlacementHistory(
            dateApplied = requireNotNull(placement.statusSetDate),
            requestForPlacementStatus = rfp.status,
            placementStatus = placement.status,
            premises = premises,
          )
        }
      }
    }.sortedByDescending { it.dateApplied }
  }

  fun getSuitableApplicationByCrn(crn: String): Cas1SuitableApplication? = approvedPremisesApplicationRepository.findByCrn(crn)
    .maxWithOrNull(mostSuitableApplication)
    ?.let { application ->

      val placementHistories = getPlacementHistories(application.id)

      val today = LocalDate.now()

      val suitablePlacement =
        placementHistories.lastOrNull { it.dateApplied >= today }
          ?: placementHistories.firstOrNull { it.dateApplied < today }

      Cas1SuitableApplication(
        id = application.id,
        applicationStatus = application.status,
        requestForPlacementStatus = suitablePlacement?.requestForPlacementStatus,
        placementStatus = suitablePlacement?.placementStatus,
        premises = suitablePlacement?.premises,
      )
    }

  fun getCurrentPremisesByCrn(crn: String): Cas1ExternalPremisesDto? = approvedPremisesApplicationRepository.findByCrn(crn)
    .sortedWith(mostSuitableApplication).firstNotNullOfOrNull { application ->
      getPlacementHistories(application.id)
        .firstOrNull { it.placementStatus == Cas1SpaceBookingStatus.ARRIVED }?.premises
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

  data class Cas1PlacementHistory(
    val dateApplied: LocalDate,
    val requestForPlacementStatus: RequestForPlacementStatus,
    val placementStatus: Cas1SpaceBookingStatus?,
    val premises: Cas1ExternalPremisesDto?,
  )
}
