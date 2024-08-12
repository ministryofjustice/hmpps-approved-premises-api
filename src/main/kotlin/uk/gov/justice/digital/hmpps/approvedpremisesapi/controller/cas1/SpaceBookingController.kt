package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.SpaceBookingsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromValidatableActionResult
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

    val booking = extractEntityFromValidatableActionResult(
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
}
