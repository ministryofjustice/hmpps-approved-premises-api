package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SuitablePremisesDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import java.time.LocalDate

@SuppressWarnings("TooGenericExceptionThrown")
@Service
class Cas1ExternalApplicationService(
  private val approvedPremisesApplicationRepository: ApprovedPremisesApplicationRepository,
  private val cas1RequestForPlacementService: Cas1RequestForPlacementService,
  private val cas1PremisesService: Cas1PremisesService,

) {
  fun getSuitableApplicationByCrn(crn: String): Cas1SuitableApplication? = approvedPremisesApplicationRepository.findByCrn(crn)
    .maxWithOrNull(
      compareBy<ApprovedPremisesApplicationEntity> { suitableStatusesAsc[it.status] }
        .thenBy { it.submittedAt ?: it.createdAt },
    )
    ?.let { application ->

      val rfps = (cas1RequestForPlacementService.getRequestsForPlacementByApplication(application.id, requestingUser = null) as CasResult.Success).value

      val placementHistories = rfps.flatMap { rfp ->

        if (rfp.placements.isEmpty()) {
          listOf(
            Cas1PlacementHistory(
              dateApplied = rfp.statusSetDate,
              requestForPlacementStatus = rfp.status,
              placementStatus = null,
              premises = null,
              isSuitable = false,
            ),
          )
        } else {
          rfp.placements.map { placement ->
            println(placement)
            println("hello")
            println(placement.premises.id)
            println(placement.premises.name)

            val premises = cas1PremisesService.findPremisesById(placement.premises.id)
              ?.let {
                SuitablePremisesDto(
                  startDate = placement.expectedArrivalDate,
                  endDate = placement.expectedDepartureDate,
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
              isSuitable = false,
            )
          }
        }
      }.sortedByDescending { it.dateApplied }

      val today = LocalDate.now()

      val suitablePlacement =
        placementHistories.lastOrNull { it.dateApplied >= today }
          ?: placementHistories.firstOrNull { it.dateApplied < today }

      suitablePlacement?.isSuitable = true

      Cas1SuitableApplication(
        id = application.id,
        applicationStatus = application.status,
        placementHistories = placementHistories,
        requestForPlacementStatus = suitablePlacement?.requestForPlacementStatus,
        placementStatus = suitablePlacement?.placementStatus,
      )
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
