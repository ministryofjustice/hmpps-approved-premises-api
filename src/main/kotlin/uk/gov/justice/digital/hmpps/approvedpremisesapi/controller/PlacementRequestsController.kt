package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PlacementRequestsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingNotMadeTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPersonDetailsForCrn
import java.util.UUID

@Service
class PlacementRequestsController(
  private val userService: UserService,
  private val placementRequestService: PlacementRequestService,
  private val placementRequestTransformer: PlacementRequestTransformer,
  private val offenderService: OffenderService,
  private val bookingService: BookingService,
  private val bookingTransformer: BookingTransformer,
  private val bookingNotMadeTransformer: BookingNotMadeTransformer
) : PlacementRequestsApiDelegate {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun placementRequestsGet(): ResponseEntity<List<PlacementRequest>> {
    val user = userService.getUserForRequest()

    val requests = placementRequestService.getVisiblePlacementRequestsForUser(user)

    return ResponseEntity.ok(
      requests.mapNotNull {
        val personDetail = getPersonDetailsForCrn(log, it.application.crn, user.deliusUsername, offenderService)

        if (personDetail === null) {
          return@mapNotNull null
        }

        placementRequestTransformer.transformJpaToApi(it, personDetail.first, personDetail.second)
      }
    )
  }

  override fun placementRequestsIdGet(id: UUID): ResponseEntity<PlacementRequest> {
    val user = userService.getUserForRequest()

    val authorisationResult = placementRequestService.getPlacementRequestForUser(user, id)

    val placementRequest = when (authorisationResult) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(id, "PlacementRequest")
      is AuthorisableActionResult.Success -> authorisationResult.entity
    }

    val personDetail = getPersonDetailsForCrn(log, placementRequest.application.crn, user.deliusUsername, offenderService)
      ?: throw NotFoundProblem(placementRequest.application.crn, "Offender")

    return ResponseEntity.ok(
      placementRequestTransformer.transformJpaToApi(placementRequest, personDetail.first, personDetail.second)
    )
  }

  override fun placementRequestsIdBookingPost(id: UUID, newPlacementRequestBooking: NewPlacementRequestBooking): ResponseEntity<Booking> {
    val user = userService.getUserForRequest()

    val authorisableResult = bookingService.createApprovedPremisesBookingFromPlacementRequest(
      user = user,
      placementRequestId = id,
      bedId = newPlacementRequestBooking.bedId,
      arrivalDate = newPlacementRequestBooking.arrivalDate,
      departureDate = newPlacementRequestBooking.departureDate
    )

    val validatableResult = when (authorisableResult) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(authorisableResult.id!!, authorisableResult.entityType!!)
      is AuthorisableActionResult.Success -> authorisableResult.entity
    }

    val createdBooking = when (validatableResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validatableResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validatableResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = validatableResult.conflictingEntityId, conflictReason = validatableResult.message)
      is ValidatableActionResult.Success -> validatableResult.entity
    }

    val offenderResult = offenderService.getOffenderByCrn(createdBooking.crn, user.deliusUsername)
    val offender = when (offenderResult) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw BadRequestProblem(mapOf("$.crn" to "doesNotExist"))
      is AuthorisableActionResult.Success -> offenderResult.entity
    }

    if (offender.otherIds.nomsNumber == null) {
      throw InternalServerErrorProblem("No nomsNumber present for CRN")
    }

    val inmateDetailResult = offenderService.getInmateDetailByNomsNumber(offender.otherIds.nomsNumber)
    val inmate = when (inmateDetailResult) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw BadRequestProblem(mapOf("$.crn" to "doesNotExist"))
      is AuthorisableActionResult.Success -> inmateDetailResult.entity
    }

    return ResponseEntity.ok(bookingTransformer.transformJpaToApi(createdBooking, offender, inmate, null))
  }

  override fun placementRequestsIdBookingNotMadePost(id: UUID, newBookingNotMade: NewBookingNotMade): ResponseEntity<BookingNotMade> {
    val user = userService.getUserForRequest()

    val authorisableResult = placementRequestService.createBookingNotMade(
      user = user,
      placementRequestId = id,
      notes = newBookingNotMade.notes
    )

    val bookingNotMade = when (authorisableResult) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(authorisableResult.id!!, authorisableResult.entityType!!)
      is AuthorisableActionResult.Success -> authorisableResult.entity
    }

    return ResponseEntity(bookingNotMadeTransformer.transformJpaToApi(bookingNotMade), HttpStatus.OK)
  }
}
