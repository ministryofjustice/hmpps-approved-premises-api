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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingNotMadeTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.LocalDate
import java.util.UUID

@Service
class PlacementRequestsController(
  private val userService: UserService,
  private val placementRequestService: Cas1PlacementRequestService,
  private val placementRequestTransformer: PlacementRequestTransformer,
  private val bookingNotMadeTransformer: BookingNotMadeTransformer,
  private val userAccessService: Cas1UserAccessService,
  private val offenderDetailService: OffenderDetailService,
) : PlacementRequestsApiDelegate {

  @Deprecated("This will be removed once UI has removed usage in code")
  override fun placementRequestsIdBookingPost(id: UUID, newPlacementRequestBooking: NewPlacementRequestBooking): ResponseEntity<NewPlacementRequestBookingConfirmation> = ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

  @Deprecated("Use Cas1PlacementRequestsController.search instead")
  override fun placementRequestsDashboardGet(
    status: PlacementRequestStatus?,
    crnOrName: String?,
    tier: RiskTierLevel?,
    arrivalDateStart: LocalDate?,
    arrivalDateEnd: LocalDate?,
    requestType: PlacementRequestRequestType?,
    cruManagementAreaId: UUID?,
    page: Int?,
    sortBy: PlacementRequestSortField?,
    sortDirection: SortDirection?,
  ): ResponseEntity<List<PlacementRequest>> {
    val user = userService.getUserForRequest()

    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_VIEW_CRU_DASHBOARD)

    val (requests, metadata) = placementRequestService.getAllActive(
      Cas1PlacementRequestService.AllActiveSearchCriteria(
        status = status,
        crnOrName = crnOrName,
        tier = tier?.value,
        arrivalDateStart = arrivalDateStart,
        arrivalDateEnd = arrivalDateEnd,
        requestType = requestType,
        cruManagementAreaId = cruManagementAreaId,
      ),
      PageCriteria(
        sortBy = sortBy ?: PlacementRequestSortField.createdAt,
        sortDirection = sortDirection ?: SortDirection.asc,
        page = page,
      ),
    )

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      mapPersonDetailOntoPlacementRequests(requests, user),
    )
  }

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

  private fun mapPersonDetailOntoPlacementRequests(placementRequests: List<PlacementRequestEntity>, user: UserEntity): List<PlacementRequest> = placementRequests.map {
    val personInfo = offenderDetailService.getPersonInfoResult(it.application.crn, user.cas1LaoStrategy())

    placementRequestTransformer.transformJpaToApi(it, personInfo)
  }
}
