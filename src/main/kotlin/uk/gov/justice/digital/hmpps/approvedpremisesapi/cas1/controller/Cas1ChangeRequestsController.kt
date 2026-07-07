package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.ChangeRequestsCas1Delegate
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
@Service
class Cas1ChangeRequestsController(
  private val cas1ChangeRequestService: Cas1ChangeRequestService,
  private val cas1ChangeRequestTransformer: Cas1ChangeRequestTransformer,
  private val userAccessService: Cas1UserAccessService,
) : ChangeRequestsCas1Delegate {

  override fun createPlacementAppeal(placementRequestId: UUID, cas1NewChangeRequest: Cas1NewChangeRequest): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PLACEMENT_APPEAL_CREATE)

    val result = cas1ChangeRequestService.createChangeRequest(placementRequestId, cas1NewChangeRequest)

    ensureEntityFromCasResultIsSuccess(result)

    return ResponseEntity(HttpStatus.OK)
  }

  override fun createPlannedTransfer(placementRequestId: UUID, cas1NewChangeRequest: Cas1NewChangeRequest): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_TRANSFER_CREATE)

    val result = cas1ChangeRequestService.createChangeRequest(placementRequestId, cas1NewChangeRequest)

    ensureEntityFromCasResultIsSuccess(result)

    return ResponseEntity(HttpStatus.OK)
  }

  override fun createPlacementExtension(placementRequestId: UUID, cas1NewChangeRequest: Cas1NewChangeRequest): ResponseEntity<Unit> = throw BadRequestProblem(
    errorDetail = "Change request type is not ${Cas1ChangeRequestType.PLANNED_TRANSFER} or ${Cas1ChangeRequestType.PLACEMENT_APPEAL}",
  )

  override fun get(
    placementRequestId: UUID,
    changeRequestId: UUID,
  ): ResponseEntity<Cas1ChangeRequest> {
    userAccessService.ensureCurrentUserHasPermission(CAS1_CHANGE_REQUEST_VIEW)

    val result = extractEntityFromCasResult(cas1ChangeRequestService.getChangeRequestForPlacementId(placementRequestId, changeRequestId))

    return ResponseEntity.ok(
      cas1ChangeRequestTransformer.transformEntityToCas1ChangeRequest(
        result,
      ),
    )
  }

  override fun reject(
    placementRequestId: UUID,
    changeRequestId: UUID,
    cas1RejectChangeRequest: Cas1RejectChangeRequest,
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
