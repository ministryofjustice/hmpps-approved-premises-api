package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.SpaceBookingsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssignKeyWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingResidency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission.CAS1_SPACE_BOOKING_LIST
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.forCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService.SpaceBookingFilterCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LimitedAccessStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Service
class SpaceBookingController(
  private val userAccessService: UserAccessService,
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val spaceBookingService: Cas1SpaceBookingService,
  private val spaceBookingTransformer: Cas1SpaceBookingTransformer,
) : SpaceBookingsCas1Delegate {
  override fun placementRequestsPlacementRequestIdSpaceBookingsPost(
    placementRequestId: UUID,
    body: NewCas1SpaceBooking,
  ): ResponseEntity<Cas1SpaceBooking> {
    val user = userService.getUserForRequest()

    val booking = extractEntityFromCasResult(
      spaceBookingService.createNewBooking(
        body.premisesId,
        placementRequestId,
        body.arrivalDate,
        body.departureDate,
        user,
      ),
    )

    val person = offenderService.getPersonInfoResult(
      booking.placementRequest.application.crn,
      user.deliusUsername,
      user.hasQualification(UserQualification.LAO),
    )

    return ResponseEntity.ok(spaceBookingTransformer.transformJpaToApi(person, booking))
  }

  override fun premisesPremisesIdSpaceBookingsGet(
    premisesId: UUID,
    residency: Cas1SpaceBookingResidency?,
    crnOrName: String?,
    sortDirection: SortDirection?,
    sortBy: Cas1SpaceBookingSummarySortField?,
    page: Int?,
    perPage: Int?,
  ): ResponseEntity<List<Cas1SpaceBookingSummary>> {
    userAccessService.ensureCurrentUserHasPermission(CAS1_SPACE_BOOKING_LIST)

    val result = spaceBookingService.search(
      premisesId,
      SpaceBookingFilterCriteria(
        residency = residency,
        crnOrName = crnOrName,
      ),
      PageCriteria(
        sortBy = sortBy ?: Cas1SpaceBookingSummarySortField.personName,
        sortDirection = sortDirection ?: SortDirection.desc,
        page = page,
        perPage = perPage,
      ),
    )

    val (searchResults, metadata) = extractEntityFromCasResult(result)

    val user = userService.getUserForRequest()
    val offenderSummaries = offenderService.getPersonSummaryInfoResults(
      crns = searchResults.map { it.crn }.toSet(),
      limitedAccessStrategy = user.cas1LimitedAccessStrategy(),
    )

    val summaries = searchResults.map {
      spaceBookingTransformer.transformSearchResultToSummary(
        it,
        offenderSummaries.forCrn(it.crn),
      )
    }

    return ResponseEntity.ok()
      .headers(metadata?.toHeaders())
      .body(summaries)
  }

  override fun premisesPremisesIdSpaceBookingsBookingIdGet(
    premisesId: UUID,
    bookingId: UUID,
  ): ResponseEntity<Cas1SpaceBooking> {
    return super.premisesPremisesIdSpaceBookingsBookingIdGet(premisesId, bookingId)
  }

  override fun premisesPremisesIdSpaceBookingsBookingIdArrivalPost(
    premisesId: UUID,
    bookingId: UUID,
    cas1NewArrival: Cas1NewArrival,
  ): ResponseEntity<Unit> {
    return super.premisesPremisesIdSpaceBookingsBookingIdArrivalPost(premisesId, bookingId, cas1NewArrival)
  }

  override fun premisesPremisesIdSpaceBookingsBookingIdDeparturePost(
    premisesId: UUID,
    bookingId: UUID,
    cas1NewDeparture: Cas1NewDeparture,
  ): ResponseEntity<Unit> {
    return super.premisesPremisesIdSpaceBookingsBookingIdDeparturePost(premisesId, bookingId, cas1NewDeparture)
  }

  override fun premisesPremisesIdSpaceBookingsBookingIdKeyworkerPost(
    premisesId: UUID,
    bookingId: UUID,
    cas1AssignKeyWorker: Cas1AssignKeyWorker,
  ): ResponseEntity<Unit> {
    return super.premisesPremisesIdSpaceBookingsBookingIdKeyworkerPost(premisesId, bookingId, cas1AssignKeyWorker)
  }
}
