package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PlacementRequestsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBookingConfirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NewPlacementRequestBookingConfirmationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestDetailTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromAuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getInfoForPersonOrThrow
import java.time.LocalDate
import java.util.UUID

@Service
class PlacementRequestsController(
  private val userService: UserService,
  private val placementRequestService: PlacementRequestService,
  private val placementRequestTransformer: PlacementRequestTransformer,
  private val placementRequestDetailTransformer: PlacementRequestDetailTransformer,
  private val offenderService: OffenderService,
  private val bookingService: BookingService,
  private val bookingConfirmationTransformer: NewPlacementRequestBookingConfirmationTransformer,
  private val bookingNotMadeTransformer: BookingNotMadeTransformer,
) : PlacementRequestsApiDelegate {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun placementRequestsGet(): ResponseEntity<List<PlacementRequest>> {
    val user = userService.getUserForRequest()

    val requests = placementRequestService.getVisiblePlacementRequestsForUser(user)

    return ResponseEntity.ok(
      requests.map {
        val personInfo = offenderService.getInfoForPersonOrThrow(it.application.crn, user)

        placementRequestTransformer.transformJpaToApi(it, personInfo)
      },
    )
  }

  override fun placementRequestsDashboardGet(status: PlacementRequestStatus?, crn: String?, crnOrName: String?, tier: RiskTierLevel?, arrivalDateStart: LocalDate?, arrivalDateEnd: LocalDate?, page: Int?, sortBy: PlacementRequestSortField?, sortDirection: SortDirection?): ResponseEntity<List<PlacementRequest>> {
    val user = userService.getUserForRequest()

    if (!user.hasRole(UserRole.CAS1_WORKFLOW_MANAGER)) {
      throw ForbiddenProblem()
    }

    val (requests, metadata) = placementRequestService.getAllActive(status, crn, tier?.value, arrivalDateStart, arrivalDateEnd, page, sortBy ?: PlacementRequestSortField.createdAt, sortDirection)

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      mapPersonDetailOntoPlacementRequests(requests, user),
    )
  }

  override fun placementRequestsIdGet(id: UUID): ResponseEntity<PlacementRequestDetail> {
    val user = userService.getUserForRequest()

    val authorisationResult = placementRequestService.getPlacementRequestForUser(user, id)

    val (placementRequest, cancellations) = when (authorisationResult) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(id, "PlacementRequest")
      is AuthorisableActionResult.Success -> authorisationResult.entity
    }

    val personInfo = offenderService.getInfoForPersonOrThrow(placementRequest.application.crn, user)

    return ResponseEntity.ok(
      placementRequestDetailTransformer.transformJpaToApi(placementRequest, personInfo, cancellations),
    )
  }

  override fun placementRequestsIdBookingPost(id: UUID, newPlacementRequestBooking: NewPlacementRequestBooking): ResponseEntity<NewPlacementRequestBookingConfirmation> {
    val user = userService.getUserForRequest()

    val authorisableResult = bookingService.createApprovedPremisesBookingFromPlacementRequest(
      user = user,
      placementRequestId = id,
      bedId = newPlacementRequestBooking.bedId,
      premisesId = newPlacementRequestBooking.premisesId,
      arrivalDate = newPlacementRequestBooking.arrivalDate,
      departureDate = newPlacementRequestBooking.departureDate,
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

    return ResponseEntity.ok(bookingConfirmationTransformer.transformJpaToApi(createdBooking))
  }

  override fun placementRequestsIdBookingNotMadePost(id: UUID, newBookingNotMade: NewBookingNotMade): ResponseEntity<BookingNotMade> {
    val user = userService.getUserForRequest()

    val authorisableResult = placementRequestService.createBookingNotMade(
      user = user,
      placementRequestId = id,
      notes = newBookingNotMade.notes,
    )

    val bookingNotMade = when (authorisableResult) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(authorisableResult.id!!, authorisableResult.entityType!!)
      is AuthorisableActionResult.Success -> authorisableResult.entity
    }

    return ResponseEntity(bookingNotMadeTransformer.transformJpaToApi(bookingNotMade), HttpStatus.OK)
  }

  override fun placementRequestsIdWithdrawalPost(id: UUID): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val result = extractEntityFromAuthorisableActionResult(
      placementRequestService.withdrawPlacementRequest(id, user),
    )

    return ResponseEntity.ok(result)
  }

  private fun mapPersonDetailOntoPlacementRequests(placementRequests: List<PlacementRequestEntity>, user: UserEntity): List<PlacementRequest> {
    return placementRequests.mapNotNull {
      val personInfo = offenderService.getInfoForPerson(it.application.crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

      if (personInfo !is PersonInfoResult.Success) throw InternalServerErrorProblem("Unable to get Person Info for CRN: ${it.application.crn}")

      placementRequestTransformer.transformJpaToApi(it, personInfo)
    }
  }
}
