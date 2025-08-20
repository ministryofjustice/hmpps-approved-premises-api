package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PlacementRequestsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBookingConfirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingNotMadeTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Service
class PlacementRequestsController(
  private val userService: UserService,
  private val placementRequestService: Cas1PlacementRequestService,
  private val bookingNotMadeTransformer: BookingNotMadeTransformer,
  private val userAccessService: Cas1UserAccessService,
) : PlacementRequestsApiDelegate {

  @Deprecated("This will be removed once UI has removed usage in code")
  override fun placementRequestsIdBookingPost(id: UUID, newPlacementRequestBooking: NewPlacementRequestBooking): ResponseEntity<NewPlacementRequestBookingConfirmation> = ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

  override fun placementRequestsIdBookingNotMadePost(id: UUID, newBookingNotMade: NewBookingNotMade): ResponseEntity<BookingNotMade> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PLACEMENT_REQUEST_RECORD_UNABLE_TO_MATCH)

    val user = userService.getUserForRequest()

    val result = placementRequestService.createBookingNotMade(
      user = user,
      placementRequestId = id,
      notes = newBookingNotMade.notes,
    )

    val bookingNotMade = extractEntityFromCasResult(result)

    return ResponseEntity(bookingNotMadeTransformer.transformJpaToApi(bookingNotMade), HttpStatus.OK)
  }
}
