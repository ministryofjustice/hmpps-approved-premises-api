package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import kotlinx.datetime.toKotlinDatePeriod
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingResidency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1SpaceSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1SpaceBookingService(
  private val premisesService: PremisesService,
  private val placementRequestService: PlacementRequestService,
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  private val cas1SpaceSearchRepository: Cas1SpaceSearchRepository,
  private val cas1BookingDomainEventService: Cas1BookingDomainEventService,
) {
  fun createNewBooking(
    premisesId: UUID,
    placementRequestId: UUID,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    createdBy: UserEntity,
  ): CasResult<Cas1SpaceBookingEntity> = validatedCasResult {
    val premises = premisesService.getPremises(premisesId) as? ApprovedPremisesEntity
    if (premises == null) {
      "$.premisesId" hasValidationError "doesNotExist"
    }

    val placementRequest = placementRequestService.getPlacementRequestOrNull(placementRequestId)
    if (placementRequest == null) {
      "$.placementRequestId" hasValidationError "doesNotExist"
    }

    if (arrivalDate >= departureDate) {
      "$.departureDate" hasValidationError "shouldBeAfterArrivalDate"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    premises!!
    placementRequest!!

    when (val existingBooking = cas1SpaceBookingRepository.findByPremisesIdAndPlacementRequestId(premisesId, placementRequestId)) {
      null -> {}
      else -> return existingBooking.id hasConflictError "A Space Booking already exists for this premises and placement request"
    }

    val durationInDays = arrivalDate.until(departureDate).toKotlinDatePeriod().days
    cas1SpaceSearchRepository.getSpaceAvailabilityForCandidatePremises(listOf(premisesId), arrivalDate, durationInDays)

    val application = placementRequest.application

    val spaceBooking = cas1SpaceBookingRepository.save(
      Cas1SpaceBookingEntity(
        id = UUID.randomUUID(),
        premises = premises,
        application = application,
        placementRequest = placementRequest,
        createdBy = createdBy,
        createdAt = OffsetDateTime.now(),
        expectedArrivalDate = arrivalDate,
        expectedDepartureDate = departureDate,
        actualArrivalDateTime = null,
        actualDepartureDateTime = null,
        canonicalArrivalDate = arrivalDate,
        canonicalDepartureDate = departureDate,
        crn = placementRequest.application.crn,
        keyWorkerStaffCode = null,
        keyWorkerName = null,
        keyWorkerAssignedAt = null,
      ),
    )

    cas1BookingDomainEventService.spaceBookingMade(
      application = application,
      booking = spaceBooking,
      user = createdBy,
      placementRequest = placementRequest,
    )

    success(spaceBooking)
  }

  fun search(
    premisesId: UUID,
    filterCriteria: SpaceBookingFilterCriteria,
    pageCriteria: PageCriteria<Cas1SpaceBookingSummarySortField>,
  ): CasResult<Pair<List<Cas1SpaceBookingSearchResult>, PaginationMetadata?>> {
    if (premisesService.getPremises(premisesId) !is ApprovedPremisesEntity) return CasResult.NotFound("premises", premisesId.toString())

    val page = cas1SpaceBookingRepository.search(
      filterCriteria.residency?.name,
      filterCriteria.crnOrName,
      premisesId,
      pageCriteria.toPageableOrAllPages(
        sortByConverter = when (pageCriteria.sortBy) {
          Cas1SpaceBookingSummarySortField.personName -> "personName"
          Cas1SpaceBookingSummarySortField.canonicalArrivalDate -> "canonicalArrivalDate"
          Cas1SpaceBookingSummarySortField.canonicalDepartureDate -> "canonicalDepartureDate"
          Cas1SpaceBookingSummarySortField.keyWorkerName -> "keyWorkerName"
          Cas1SpaceBookingSummarySortField.tier -> "tier"
        },
      ),
    )

    return CasResult.Success(
      Pair(
        page.toList(),
        getMetadata(page, pageCriteria),
      ),
    )
  }

  data class SpaceBookingFilterCriteria(
    val residency: Cas1SpaceBookingResidency?,
    val crnOrName: String?,
  )
}
