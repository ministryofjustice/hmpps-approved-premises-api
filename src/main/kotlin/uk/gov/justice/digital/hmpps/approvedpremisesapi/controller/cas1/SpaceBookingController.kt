package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.SpaceBookingsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingResidency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Service
class SpaceBookingController(
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
    return super.premisesPremisesIdSpaceBookingsGet(
      premisesId,
      residency,
      crnOrName,
      sortDirection,
      sortBy,
      page,
      perPage,
    )
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
}
