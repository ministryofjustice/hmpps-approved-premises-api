package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService.PlacementRequestAndCancellations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingNotMadeTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NewPlacementRequestBookingConfirmationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestDetailTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
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
  private val cas1WithdrawableService: Cas1WithdrawableService,
) : PlacementRequestsApiDelegate {

  override fun placementRequestsGet(): ResponseEntity<List<PlacementRequest>> {
    val user = userService.getUserForRequest()

    val requests = placementRequestService.getVisiblePlacementRequestsForUser(user, null, null)

    return ResponseEntity.ok(
      requests.first.map {
        val personInfo = offenderService.getPersonInfoResult(it.application.crn, user.deliusUsername, false)

        placementRequestTransformer.transformJpaToApi(it, personInfo)
      },
    )
  }

  override fun placementRequestsDashboardGet(
    status: PlacementRequestStatus?,
    crn: String?,
    crnOrName: String?,
    tier: RiskTierLevel?,
    arrivalDateStart: LocalDate?,
    arrivalDateEnd: LocalDate?,
    requestType: PlacementRequestRequestType?,
    apAreaId: UUID?,
    cruManagementAreaId: UUID?,
    page: Int?,
    sortBy: PlacementRequestSortField?,
    sortDirection: SortDirection?,
  ): ResponseEntity<List<PlacementRequest>> {
    val user = userService.getUserForRequest()

    if (!user.hasPermission(UserPermission.CAS1_VIEW_CRU_DASHBOARD)) {
      throw ForbiddenProblem()
    }

    val (requests, metadata) = placementRequestService.getAllActive(
      PlacementRequestService.AllActiveSearchCriteria(
        status = status,
        crn = crn,
        crnOrName = crnOrName,
        tier = tier?.value,
        arrivalDateStart = arrivalDateStart,
        arrivalDateEnd = arrivalDateEnd,
        requestType = requestType,
        apAreaId = apAreaId,
        cruManagementAreaId = cruManagementAreaId,
      ),
      PageCriteria(
        sortBy = sortBy ?: PlacementRequestSortField.CREATED_AT,
        sortDirection = sortDirection ?: SortDirection.ASC,
        page = page,
      ),
    )

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      mapPersonDetailOntoPlacementRequests(requests, user),
    )
  }

  override fun placementRequestsIdGet(id: UUID): ResponseEntity<PlacementRequestDetail> {
    val user = userService.getUserForRequest()

    val placementRequest = extractEntityFromCasResult(placementRequestService.getPlacementRequestForUser(user, id))

    return ResponseEntity.ok(toPlacementRequestDetail(user, placementRequest))
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

  override fun placementRequestsIdWithdrawalPost(id: UUID, body: WithdrawPlacementRequest?): ResponseEntity<PlacementRequestDetail> {
    val user = userService.getUserForRequest()

    val reason = when (body?.reason) {
      WithdrawPlacementRequestReason.DUPLICATE_PLACEMENT_REQUEST -> PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST
      WithdrawPlacementRequestReason.ALTERNATIVE_PROVISION_IDENTIFIED -> PlacementRequestWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED
      WithdrawPlacementRequestReason.CHANGE_IN_CIRCUMSTANCES -> PlacementRequestWithdrawalReason.CHANGE_IN_CIRCUMSTANCES
      WithdrawPlacementRequestReason.CHANGE_IN_RELEASE_DECISION -> PlacementRequestWithdrawalReason.CHANGE_IN_RELEASE_DECISION
      WithdrawPlacementRequestReason.NO_CAPACITY_DUE_TO_LOST_BED -> PlacementRequestWithdrawalReason.NO_CAPACITY_DUE_TO_LOST_BED
      WithdrawPlacementRequestReason.NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION -> PlacementRequestWithdrawalReason.NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION
      WithdrawPlacementRequestReason.NO_CAPACITY -> PlacementRequestWithdrawalReason.NO_CAPACITY
      WithdrawPlacementRequestReason.ERROR_IN_PLACEMENT_REQUEST -> PlacementRequestWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST
      WithdrawPlacementRequestReason.WITHDRAWN_BY_PP -> throw NotAllowedProblem("Withdrawal reason is reserved for internal use")
      WithdrawPlacementRequestReason.RELATED_APPLICATION_WITHDRAWN -> throw NotAllowedProblem("Withdrawal reason is reserved for internal use")
      WithdrawPlacementRequestReason.RELATED_PLACEMENT_REQUEST_WITHDRAWN -> throw NotAllowedProblem("Withdrawal reason is reserved for internal use")
      WithdrawPlacementRequestReason.RELATED_PLACEMENT_APPLICATION_WITHDRAWN -> throw NotAllowedProblem("Withdrawal reason is reserved for internal use")
      null -> null
    }

    val placementRequestAndCancellations = extractEntityFromCasResult(
      cas1WithdrawableService.withdrawPlacementRequest(
        id,
        user,
        reason,
      ),
    )

    return ResponseEntity.ok(toPlacementRequestDetail(user, placementRequestAndCancellations))
  }

  private fun toPlacementRequestDetail(
    forUser: UserEntity,
    placementRequestAndCancellations: PlacementRequestAndCancellations,
  ): PlacementRequestDetail {
    val personInfo = offenderService.getPersonInfoResult(
      placementRequestAndCancellations.placementRequest.application.crn,
      forUser.deliusUsername,
      ignoreLaoRestrictions = false,
    )

    return placementRequestDetailTransformer.transformJpaToApi(
      placementRequestAndCancellations.placementRequest,
      personInfo,
      placementRequestAndCancellations.cancellations,
    )
  }

  private fun mapPersonDetailOntoPlacementRequests(placementRequests: List<PlacementRequestEntity>, user: UserEntity): List<PlacementRequest> {
    return placementRequests.map {
      val personInfo = offenderService.getPersonInfoResult(it.application.crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

      placementRequestTransformer.transformJpaToApi(it, personInfo)
    }
  }
}
