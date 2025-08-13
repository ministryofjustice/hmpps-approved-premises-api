package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApprovedPlacementAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssignKeyWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewEmergencyTransfer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewPlannedTransfer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewSpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewSpaceBookingCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NonArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ShortenSpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingResidency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1UpdateSpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission.CAS1_SPACE_BOOKING_LIST
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission.CAS1_SPACE_BOOKING_RECORD_NON_ARRIVAL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission.CAS1_TRANSFER_CREATE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.forCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ParamDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingManagementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingManagementService.DepartureInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService.SpaceBookingFilterCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService.UpdateType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingUpdateService.UpdateBookingDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Cas1Controller
@Tag(name = "CAS1 Space Booking")
class Cas1SpaceBookingController(
  private val userAccessService: Cas1UserAccessService,
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val spaceBookingService: Cas1SpaceBookingService,
  private val spaceBookingTransformer: Cas1SpaceBookingTransformer,
  private val cas1SpaceBookingService: Cas1SpaceBookingService,
  private val cas1WithdrawableService: Cas1WithdrawableService,
  private val characteristicService: CharacteristicService,
  private val cas1TimelineService: Cas1TimelineService,
  private val cas1ChangeRequestService: Cas1ChangeRequestService,
  private val cas1ChangeRequestRepository: Cas1ChangeRequestRepository,
  private val cas1BookingManagementService: Cas1BookingManagementService,
  private val offenderDetailService: OffenderDetailService,
) {

  @SuppressWarnings("UnusedParameter")
  @Operation(summary = "Returns timeline of a specific space booking with a given ID")
  @GetMapping("/premises/{premisesId}/space-bookings/{bookingId}/timeline")
  fun getSpaceBookingTimeline(
    @PathVariable premisesId: UUID,
    @PathVariable bookingId: UUID,
  ): ResponseEntity<List<Cas1TimelineEvent>> {
    val events = cas1TimelineService.getSpaceBookingTimeline(bookingId)
    return ResponseEntity.ok(events)
  }

  @Operation(
    summary = "Create a booking for a space in premises, associated with a given placement request",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas1SpaceBooking::class))]),
      ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
      ApiResponse(responseCode = "404", description = "invalid premises ID or booking ID", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @PostMapping("/placement-requests/{placementRequestId}/space-bookings")
  fun createSpaceBooking(
    @PathVariable placementRequestId: UUID,
    @RequestBody body: Cas1NewSpaceBooking,
  ): ResponseEntity<Cas1SpaceBooking> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_SPACE_BOOKING_CREATE)

    val user = userService.getUserForRequest()

    val requestedCharacteristics =
      (
        (body.requirements?.essentialCharacteristics ?: emptyList()) +
          ((body.characteristics) ?: emptyList())
        ).toSet()

    val characteristics = characteristicService.getCharacteristicsByPropertyNames(
      requestedCharacteristics.map { it.value },
      ServiceName.approvedPremises,
    )

    val booking = extractEntityFromCasResult(
      spaceBookingService.createNewBooking(
        body.premisesId,
        placementRequestId,
        body.arrivalDate,
        body.departureDate,
        user,
        characteristics,
      ),
    )

    return ResponseEntity.ok(toCas1SpaceBooking(booking))
  }

  @Operation(summary = "Lists space bookings for the premises, given optional filtering criteria")
  @GetMapping("/premises/{premisesId}/space-bookings")
  fun getSpaceBookings(
    @Parameter(description = "ID of the premises to show space bookings for", required = true) @PathVariable premisesId: UUID,
    @RequestParam residency: Cas1SpaceBookingResidency?,
    @RequestParam crnOrName: String?,
    @Schema(deprecated = true, description = "Use keyworker user id") @RequestParam keyWorkerStaffCode: String?,
    @RequestParam keyWorkerUserId: UUID?,
    @RequestParam sortDirection: SortDirection?,
    @RequestParam sortBy: Cas1SpaceBookingSummarySortField?,
    @RequestParam page: Int?,
    @RequestParam perPage: Int?,
  ): ResponseEntity<List<Cas1SpaceBookingSummary>> {
    userAccessService.ensureCurrentUserHasPermission(CAS1_SPACE_BOOKING_LIST)

    val result = spaceBookingService.search(
      premisesId,
      SpaceBookingFilterCriteria(
        residency = residency,
        crnOrName = crnOrName,
        keyWorkerStaffCode = keyWorkerStaffCode,
        keyWorkerUserId = keyWorkerUserId,
      ),
      PageCriteria(
        sortBy = sortBy ?: Cas1SpaceBookingSummarySortField.personName,
        sortDirection = sortDirection ?: SortDirection.desc,
        page = page,
        perPage = perPage,
      ),
    )

    val searchResultsContainer = extractEntityFromCasResult(result)

    val user = userService.getUserForRequest()
    val offenderSummaries = offenderService.getPersonSummaryInfoResults(
      crns = searchResultsContainer.results.map { it.crn }.toSet(),
      laoStrategy = user.cas1LaoStrategy(),
    )

    val summaries = searchResultsContainer.results.map {
      spaceBookingTransformer.transformSearchResultToSummary(
        searchResult = it,
        premises = searchResultsContainer.premises,
        personSummaryInfo = offenderSummaries.forCrn(it.crn),
      )
    }

    return ResponseEntity.ok()
      .headers(searchResultsContainer.paginationMetadata?.toHeaders())
      .body(summaries)
  }

  @Operation(summary = "Returns space booking information for a given id")
  @GetMapping("/space-bookings/{bookingId}")
  fun getSpaceBookingById(@Parameter(description = "ID of the space booking") @PathVariable bookingId: UUID): ResponseEntity<Cas1SpaceBooking> {
    val booking = extractEntityFromCasResult(spaceBookingService.getBooking(bookingId))

    return ResponseEntity
      .ok()
      .body(toCas1SpaceBooking(booking))
  }

  @Operation(summary = "Returns space booking information for a given id")
  @GetMapping("/premises/{premisesId}/space-bookings/{bookingId}")
  fun getSpaceBookingByPremiseAndId(
    @Parameter(description = "ID of the corresponding premises", required = true) @PathVariable premisesId: UUID,
    @Parameter(description = "ID of the space booking") @PathVariable bookingId: UUID,
  ): ResponseEntity<Cas1SpaceBooking> {
    val booking = extractEntityFromCasResult(spaceBookingService.getBookingForPremisesAndId(premisesId, bookingId))

    return ResponseEntity
      .ok()
      .body(toCas1SpaceBooking(booking))
  }

  @PatchMapping("/premises/{premisesId}/space-bookings/{bookingId}")
  fun updateSpaceBooking(
    @Parameter(description = "ID of the corresponding premises", required = true) @PathVariable premisesId: UUID,
    @Parameter(description = "ID of the space booking", required = true) @PathVariable bookingId: UUID,
    @RequestBody cas1UpdateSpaceBooking: Cas1UpdateSpaceBooking,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_SPACE_BOOKING_CREATE)

    val user = userService.getUserForRequest()

    val characteristics = (cas1UpdateSpaceBooking.characteristics ?: emptyList())
      .map { it.value }
      .let { values ->
        characteristicService.getCharacteristicsByPropertyNames(values, ServiceName.approvedPremises)
      }
      .filter { it.isModelScopeRoom() }

    ensureEntityFromCasResultIsSuccess(
      cas1SpaceBookingService.updateBooking(
        UpdateBookingDetails(
          bookingId = bookingId,
          premisesId = premisesId,
          arrivalDate = cas1UpdateSpaceBooking.arrivalDate,
          departureDate = cas1UpdateSpaceBooking.departureDate,
          characteristics = characteristics,
          updatedBy = user,
          updateType = UpdateType.AMENDMENT,
        ),
      ),
    )

    return ResponseEntity(HttpStatus.OK)
  }

  @PatchMapping("/premises/{premisesId}/space-bookings/{bookingId}/shorten")
  fun shortenSpaceBooking(
    @Parameter(description = "ID of the corresponding premises", required = true) @PathVariable premisesId: UUID,
    @Parameter(description = "ID of the space booking", required = true) @PathVariable bookingId: UUID,
    @RequestBody cas1ShortenSpaceBooking: Cas1ShortenSpaceBooking,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_SPACE_BOOKING_SHORTEN)

    ensureEntityFromCasResultIsSuccess(
      cas1SpaceBookingService.shortenBooking(
        UpdateBookingDetails(
          bookingId = bookingId,
          premisesId = premisesId,
          departureDate = cas1ShortenSpaceBooking.departureDate,
          updatedBy = userService.getUserForRequest(),
          updateType = UpdateType.SHORTENING,
        ),
      ),
    )

    return ResponseEntity(HttpStatus.OK)
  }

  @PostMapping("/premises/{premisesId}/space-bookings/{bookingId}/arrival")
  fun recordArrival(
    @Parameter(description = "ID of the corresponding premises", required = true) @PathVariable premisesId: UUID,
    @Parameter(description = "ID of the space booking", required = true) @PathVariable bookingId: UUID,
    @RequestBody cas1NewArrival: Cas1NewArrival,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_SPACE_BOOKING_RECORD_ARRIVAL)

    val arrivalDateTime = cas1NewArrival.arrivalDateTime
    val arrivalDateAndTime = if (arrivalDateTime != null) {
      Pair(
        arrivalDateTime.toLocalDate(),
        arrivalDateTime.toLocalDateTime().toLocalTime(),
      )
    } else {
      getArrivalDateAndTime(cas1NewArrival.arrivalDate, cas1NewArrival.arrivalTime)
    }

    ensureEntityFromCasResultIsSuccess(
      cas1BookingManagementService.recordArrivalForBooking(
        premisesId,
        bookingId,
        arrivalDate = arrivalDateAndTime.first,
        arrivalTime = arrivalDateAndTime.second,
      ),
    )
    return ResponseEntity(HttpStatus.OK)
  }

  @PostMapping("/premises/{premisesId}/space-bookings/{bookingId}/departure")
  fun recordDeparture(
    @Parameter(description = "ID of the corresponding premises", required = true) @PathVariable premisesId: UUID,
    @Parameter(description = "ID of the space booking", required = true) @PathVariable bookingId: UUID,
    @RequestBody cas1NewDeparture: Cas1NewDeparture,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_SPACE_BOOKING_RECORD_DEPARTURE)

    val departureDateTime = cas1NewDeparture.departureDateTime
    val departureDateAndTime = if (departureDateTime != null) {
      Pair(
        departureDateTime.toLocalDate(),
        departureDateTime.toLocalDateTime().toLocalTime(),
      )
    } else {
      val departureDate = cas1NewDeparture.departureDate
        ?: throw BadRequestProblem(invalidParams = mapOf("departureDate" to ParamDetails("is required")))
      val departureTime = cas1NewDeparture.departureTime
        ?: throw BadRequestProblem(invalidParams = mapOf("departureTime" to ParamDetails("is required")))
      Pair(
        departureDate,
        LocalTime.parse(departureTime),
      )
    }

    ensureEntityFromCasResultIsSuccess(
      cas1BookingManagementService.recordDepartureForBooking(
        premisesId,
        bookingId,
        DepartureInfo(
          departureDate = departureDateAndTime.first,
          departureTime = departureDateAndTime.second,
          reasonId = cas1NewDeparture.reasonId,
          moveOnCategoryId = cas1NewDeparture.moveOnCategoryId,
          notes = cas1NewDeparture.notes,
        ),
      ),
    )
    return ResponseEntity(HttpStatus.OK)
  }

  @PostMapping("/premises/{premisesId}/space-bookings/{bookingId}/keyworker")
  fun assignKeyworker(
    @Parameter(description = "ID of the corresponding premises", required = true) @PathVariable premisesId: UUID,
    @Parameter(description = "ID of the space booking", required = true) @PathVariable bookingId: UUID,
    @RequestBody cas1AssignKeyWorker: Cas1AssignKeyWorker,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_SPACE_BOOKING_RECORD_KEYWORKER)

    ensureEntityFromCasResultIsSuccess(
      cas1BookingManagementService.recordKeyWorkerAssignedForBooking(
        premisesId,
        bookingId,
        cas1AssignKeyWorker,
      ),
    )
    return ResponseEntity(HttpStatus.OK)
  }

  @PostMapping("/premises/{premisesId}/space-bookings/{bookingId}/cancellations")
  fun cancelSpaceBooking(
    @Parameter(description = "ID of the corresponding premises", required = true) @PathVariable premisesId: UUID,
    @Parameter(description = "ID of the space booking", required = true) @PathVariable bookingId: UUID,
    @RequestBody body: Cas1NewSpaceBookingCancellation,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_SPACE_BOOKING_WITHDRAW)

    val spaceBooking = extractEntityFromCasResult(
      cas1SpaceBookingService.getBookingForPremisesAndId(premisesId, bookingId),
    )

    return ResponseEntity
      .ok()
      .body(
        extractEntityFromCasResult(
          cas1WithdrawableService.withdrawSpaceBooking(
            spaceBooking = spaceBooking,
            user = userService.getUserForRequest(),
            cancelledAt = body.occurredAt,
            body.reasonId,
            body.reasonNotes,
            appealChangeRequestId = null,
          ),
        ),
      )
  }

  @Transactional
  @PostMapping("/premises/{premisesId}/space-bookings/{bookingId}/appeal")
  fun appeal(
    @Parameter(description = "ID of the corresponding premises", required = true) @PathVariable premisesId: UUID,
    @Parameter(description = "ID of the space booking", required = true) @PathVariable bookingId: UUID,
    @RequestBody body: Cas1ApprovedPlacementAppeal,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PLACEMENT_APPEAL_ASSESS)
    val spaceBooking = extractEntityFromCasResult(
      cas1SpaceBookingService.getBookingForPremisesAndId(premisesId, bookingId),
    )

    ensureEntityFromCasResultIsSuccess(
      cas1ChangeRequestService.approvePlacementAppeal(
        changeRequestId = body.placementAppealChangeRequestId,
        user = userService.getUserForRequest(),
      ),
    )

    return ResponseEntity
      .ok()
      .body(
        extractEntityFromCasResult(
          cas1WithdrawableService.withdrawSpaceBooking(
            spaceBooking = spaceBooking,
            user = userService.getUserForRequest(),
            cancelledAt = body.occurredAt,
            userProvidedReason = CancellationReasonRepository.CAS1_BOOKING_SUCCESSFULLY_APPEALED_ID,
            otherReason = body.reasonNotes,
            appealChangeRequestId = body.placementAppealChangeRequestId,
          ),
        ),
      )
  }

  @PostMapping("/premises/{premisesId}/space-bookings/{bookingId}/non-arrival")
  fun recordNonArrival(
    @Parameter(description = "ID of the corresponding premises", required = true) @PathVariable premisesId: UUID,
    @Parameter(description = "ID of the space booking", required = true) @PathVariable bookingId: UUID,
    @RequestBody cas1NonArrival: Cas1NonArrival,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(CAS1_SPACE_BOOKING_RECORD_NON_ARRIVAL)

    val user = userService.getUserForRequest()

    ensureEntityFromCasResultIsSuccess(
      cas1BookingManagementService.recordNonArrivalForBooking(
        premisesId,
        bookingId,
        cas1NonArrival,
        user,
      ),
    )
    return ResponseEntity(HttpStatus.OK)
  }

  @Operation(summary = "Creates a space booking without a change request and truncates the departure date of the existing space booking")
  @PostMapping("/premises/{premisesId}/space-bookings/{bookingId}/emergency-transfer")
  fun emergencyTransfer(
    @Parameter(description = "ID of the corresponding premises", required = true) @PathVariable premisesId: UUID,
    @Parameter(description = "ID of the space booking", required = true) @PathVariable bookingId: UUID,
    @RequestBody cas1NewEmergencyTransfer: Cas1NewEmergencyTransfer,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(CAS1_TRANSFER_CREATE)

    val user = userService.getUserForRequest()

    ensureEntityFromCasResultIsSuccess(
      cas1SpaceBookingService.createEmergencyTransfer(premisesId, bookingId, user, cas1NewEmergencyTransfer),
    )

    return ResponseEntity(HttpStatus.OK)
  }

  @SuppressWarnings("UnusedParameter")
  @Operation(summary = "Creates a space booking for a planned transfer change request and truncates the departure date of the existing space booking. Will close the planned transfer change request.")
  @PostMapping("/premises/{premisesId}/space-bookings/{bookingId}/planned-transfer")
  fun plannedTransfer(
    @Parameter(description = "ID of the corresponding premises", required = true) @PathVariable premisesId: UUID,
    @Parameter(description = "ID of the space booking", required = true) @PathVariable bookingId: UUID,
    @RequestBody cas1NewPlannedTransfer: Cas1NewPlannedTransfer,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_TRANSFER_ASSESS)

    val user = userService.getUserForRequest()

    ensureEntityFromCasResultIsSuccess(
      cas1SpaceBookingService.createPlannedTransfer(bookingId, user, cas1NewPlannedTransfer),
    )

    return ResponseEntity(HttpStatus.OK)
  }

  private fun toCas1SpaceBooking(booking: Cas1SpaceBookingEntity): Cas1SpaceBooking {
    val user = userService.getUserForRequest()

    val person = offenderDetailService.getPersonInfoResult(
      booking.crn,
      user.cas1LaoStrategy(),
    )

    val otherBookingsInPremiseForCrn = spaceBookingService.getBookingsForPremisesAndCrn(
      premisesId = booking.premises.id,
      crn = booking.crn,
    ).filter { it.id != booking.id }
      .sortedBy { it.canonicalArrivalDate }

    val changeRequests = cas1ChangeRequestRepository.findAllBySpaceBookingAndResolvedIsFalse(booking)

    return spaceBookingTransformer.transformJpaToApi(person, booking, otherBookingsInPremiseForCrn, changeRequests)
  }

  @SuppressWarnings("ThrowsCount")
  private fun getArrivalDateAndTime(arrivalDate: LocalDate?, arrivalTime: String?): Pair<LocalDate, LocalTime> {
    if (arrivalDate == null) throw BadRequestProblem(invalidParams = mapOf("arrivalDate" to ParamDetails("is required")))
    if (arrivalTime == null) throw BadRequestProblem(invalidParams = mapOf("arrivalTime" to ParamDetails("is required")))

    if (arrivalDate > LocalDate.now()) throw BadRequestProblem(invalidParams = mapOf("arrivalDate" to ParamDetails("must be in the past")))
    if (LocalDateTime.of(arrivalDate, LocalTime.parse(arrivalTime)) > LocalDateTime.now()) {
      throw BadRequestProblem(invalidParams = mapOf("arrivalTime" to ParamDetails("must be in the past")))
    }
    return Pair(arrivalDate, LocalTime.parse(arrivalTime))
  }
}
