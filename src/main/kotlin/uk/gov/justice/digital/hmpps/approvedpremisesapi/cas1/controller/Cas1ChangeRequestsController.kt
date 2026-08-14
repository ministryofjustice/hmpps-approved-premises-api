package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1NewChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RejectChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission.CAS1_CHANGE_REQUEST_VIEW
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ChangeRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Deprecated("Change requests was developed but never used")
@Cas1Controller
@Tag(name = "change requests")
class Cas1ChangeRequestsController(
  private val cas1ChangeRequestService: Cas1ChangeRequestService,
  private val cas1ChangeRequestTransformer: Cas1ChangeRequestTransformer,
  private val userAccessService: Cas1UserAccessService,
) {

  @Operation(
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation"),
      ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
    ],
  )
  @PostMapping(
    value = ["/placement-requests/{placementRequestId}/appeal"],
    produces = ["application/problem+json"],
    consumes = ["application/json"],
  )
  fun createPlacementAppeal(
    @PathVariable placementRequestId: UUID,
    @RequestBody cas1NewChangeRequest: Cas1NewChangeRequest,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PLACEMENT_APPEAL_CREATE)

    val result = cas1ChangeRequestService.createChangeRequest(placementRequestId, cas1NewChangeRequest)

    ensureEntityFromCasResultIsSuccess(result)

    return ResponseEntity(HttpStatus.OK)
  }

  @Operation(
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation"),
      ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
    ],
  )
  @PostMapping(
    value = ["/placement-requests/{placementRequestId}/planned-transfer"],
    produces = ["application/problem+json"],
    consumes = ["application/json"],
  )
  fun createPlannedTransfer(
    @PathVariable placementRequestId: UUID,
    @RequestBody cas1NewChangeRequest: Cas1NewChangeRequest,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_TRANSFER_CREATE)

    val result = cas1ChangeRequestService.createChangeRequest(placementRequestId, cas1NewChangeRequest)

    ensureEntityFromCasResultIsSuccess(result)

    return ResponseEntity(HttpStatus.OK)
  }

  @Operation(
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation"),
      ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
    ],
  )
  @PostMapping(
    value = ["/placement-requests/{placementRequestId}/extension"],
    produces = ["application/problem+json"],
    consumes = ["application/json"],
  )
  @SuppressWarnings("UnusedParameter")
  fun createPlacementExtension(
    @PathVariable placementRequestId: UUID,
    @RequestBody cas1NewChangeRequest: Cas1NewChangeRequest,
  ): ResponseEntity<Unit> = throw BadRequestProblem(
    errorDetail = "Change request type is not ${Cas1ChangeRequestType.PLANNED_TRANSFER} or ${Cas1ChangeRequestType.PLACEMENT_APPEAL}",
  )

  @Operation(
    responses = [
      ApiResponse(responseCode = "200", description = "successfully retrieved change request", content = [Content(schema = Schema(implementation = Cas1ChangeRequest::class))]),
    ],
  )
  @GetMapping(
    value = ["/placement-requests/{placementRequestId}/change-requests/{changeRequestId}"],
    produces = ["application/json"],
  )
  fun get(
    @PathVariable placementRequestId: UUID,
    @PathVariable changeRequestId: UUID,
  ): ResponseEntity<Cas1ChangeRequest> {
    userAccessService.ensureCurrentUserHasPermission(CAS1_CHANGE_REQUEST_VIEW)

    val result = extractEntityFromCasResult(cas1ChangeRequestService.getChangeRequestForPlacementId(placementRequestId, changeRequestId))

    return ResponseEntity.ok(
      cas1ChangeRequestTransformer.transformEntityToCas1ChangeRequest(
        result,
      ),
    )
  }

  @Operation(
    responses = [
      ApiResponse(responseCode = "200", description = "successfully rejected a change request"),
    ],
  )
  @PatchMapping(
    value = ["/placement-requests/{placementRequestId}/change-requests/{changeRequestId}"],
    consumes = ["application/json"],
  )
  fun reject(
    @PathVariable placementRequestId: UUID,
    @PathVariable changeRequestId: UUID,
    @RequestBody cas1RejectChangeRequest: Cas1RejectChangeRequest,
  ): ResponseEntity<Unit> {
    val changeRequest = cas1ChangeRequestService.findChangeRequest(changeRequestId)
      ?: throw NotFoundProblem(changeRequestId, "ChangeRequest")

    when (changeRequest.type) {
      ChangeRequestType.PLACEMENT_APPEAL -> userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PLACEMENT_APPEAL_ASSESS)
      ChangeRequestType.PLANNED_TRANSFER -> userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_TRANSFER_ASSESS)
      ChangeRequestType.PLACEMENT_EXTENSION -> throw BadRequestProblem(errorDetail = "Change request type is not ${Cas1ChangeRequestType.PLANNED_TRANSFER} or ${Cas1ChangeRequestType.PLACEMENT_APPEAL}")
    }

    val result = cas1ChangeRequestService.rejectChangeRequest(placementRequestId, changeRequestId, cas1RejectChangeRequest)

    ensureEntityFromCasResultIsSuccess(result)

    return ResponseEntity(HttpStatus.OK)
  }
}
