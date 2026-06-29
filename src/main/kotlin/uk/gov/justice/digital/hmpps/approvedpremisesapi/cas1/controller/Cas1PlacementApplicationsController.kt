package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas1Controller
@Tag(name = "CAS1 Placement Applications")
class Cas1PlacementApplicationsController(
  private val userService: UserService,
  private val cas1ApplicationService: Cas1ApplicationService,
  private val offenderService: OffenderService,
  private val cas1PlacementApplicationService: Cas1PlacementApplicationService,
  private val placementApplicationTransformer: PlacementApplicationTransformer,
  private val jsonMapper: JsonMapper,
  private val withdrawalService: Cas1WithdrawableService,
) {

  @Operation(
    summary = "Creates an application for a placement",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully recorded that a placement application has been made", content = [Content(schema = Schema(implementation = PlacementApplication::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/placement-applications"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  @SuppressWarnings("TooGenericExceptionThrown")
  fun create(
    @RequestBody newPlacementApplication: NewPlacementApplication,
  ): ResponseEntity<PlacementApplication> {
    val user = userService.getUserForRequest()

    val application = extractEntityFromCasResult(
      cas1ApplicationService.getApplicationForUsername(newPlacementApplication.applicationId, user.deliusUsername),
    )

    val placementApplication = extractEntityFromCasResult(
      cas1PlacementApplicationService.createPlacementApplication(application, user),
    )

    return ResponseEntity.ok(placementApplicationTransformer.transformJpaToApi(placementApplication))
  }

  @Operation(
    summary = "Retrieves an application for a placement request",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully retrieved placement request application", content = [Content(schema = Schema(implementation = PlacementApplication::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/placement-applications/{id}"],
    produces = ["application/json"],
  )
  fun get(
    @PathVariable id: UUID,
  ): ResponseEntity<PlacementApplication> {
    val user = userService.getUserForRequest()

    val result = cas1PlacementApplicationService.getApplication(id)
    val placementApplication = extractEntityFromCasResult(result)

    if (!offenderService.canAccessOffender(placementApplication.application.crn, user.cas1LaoStrategy())) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(placementApplicationTransformer.transformJpaToApi(placementApplication))
  }

  @Operation(
    summary = "Updates an application for a placement request",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully retrieved placement request application", content = [Content(schema = Schema(implementation = PlacementApplication::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.PUT],
    value = ["/placement-applications/{id}"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  fun update(
    @PathVariable id: UUID,
    @RequestBody updatePlacementApplication: UpdatePlacementApplication,
  ): ResponseEntity<PlacementApplication> {
    val serializedData = jsonMapper.writeValueAsString(updatePlacementApplication.data)

    val result = cas1PlacementApplicationService.updateApplication(id, serializedData)

    val placementApplication = extractEntityFromCasResult(result)

    return ResponseEntity.ok(placementApplicationTransformer.transformJpaToApi(placementApplication))
  }

  @Operation(
    summary = "Submits an application for a placement request",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully submitted the placement application", content = [Content(array = ArraySchema(schema = Schema(implementation = PlacementApplication::class)))]),
      ApiResponse(responseCode = "400", description = "placement application has already been submitted", content = [Content(schema = Schema(implementation = ValidationError::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/placement-applications/{id}/submission"],
    produces = ["application/json", "application/problem+json"],
    consumes = ["application/json"],
  )
  fun submit(
    @PathVariable id: UUID,
    @RequestBody submitPlacementApplication: SubmitPlacementApplication,
  ): ResponseEntity<List<PlacementApplication>> {
    val result = cas1PlacementApplicationService.submitApplication(id, submitPlacementApplication)

    val placementApplications = extractEntityFromCasResult(result)

    return ResponseEntity.ok(placementApplications.map { placementApplicationTransformer.transformJpaToApi(it) })
  }

  @Operation(
    summary = "Submits a decision for a placement application",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully made a decision on the placement application", content = [Content(schema = Schema(implementation = PlacementApplication::class))]),
      ApiResponse(responseCode = "400", description = "placement application already has a decision made", content = [Content(schema = Schema(implementation = ValidationError::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/placement-applications/{id}/decision"],
    produces = ["application/json", "application/problem+json"],
    consumes = ["application/json"],
  )
  fun submitDecision(
    @PathVariable id: UUID,
    @RequestBody placementApplicationDecisionEnvelope: PlacementApplicationDecisionEnvelope,
  ): ResponseEntity<PlacementApplication> {
    val result = cas1PlacementApplicationService.recordDecision(id, placementApplicationDecisionEnvelope)

    val placementApplication = extractEntityFromCasResult(result)

    return ResponseEntity.ok(placementApplicationTransformer.transformJpaToApi(placementApplication))
  }

  @Operation(
    summary = "Withdraw a placement application",
    responses = [
      ApiResponse(responseCode = "200", description = "Placement application withdrawn", content = [Content(schema = Schema(implementation = PlacementApplication::class))]),
      ApiResponse(responseCode = "400", description = "placement application already has a decision made", content = [Content(schema = Schema(implementation = ValidationError::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/placement-applications/{id}/withdraw"],
    produces = ["application/json", "application/problem+json"],
    consumes = ["application/json"],
  )
  @SuppressWarnings("ThrowsCount")
  fun withdraw(
    @PathVariable id: UUID,
    @RequestBody withdrawPlacementApplication: WithdrawPlacementApplication?,
  ): ResponseEntity<PlacementApplication> {
    val withdrawalReason = when (withdrawPlacementApplication?.reason) {
      WithdrawPlacementRequestReason.duplicatePlacementRequest -> PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST
      WithdrawPlacementRequestReason.alternativeProvisionIdentified -> PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED
      WithdrawPlacementRequestReason.changeInCircumstances -> PlacementApplicationWithdrawalReason.CHANGE_IN_CIRCUMSTANCES
      WithdrawPlacementRequestReason.changeInReleaseDecision -> PlacementApplicationWithdrawalReason.CHANGE_IN_RELEASE_DECISION
      WithdrawPlacementRequestReason.noCapacityDueToLostBed -> PlacementApplicationWithdrawalReason.NO_CAPACITY_DUE_TO_LOST_BED
      WithdrawPlacementRequestReason.noCapacityDueToPlacementPrioritisation -> PlacementApplicationWithdrawalReason.NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION
      WithdrawPlacementRequestReason.noCapacity -> PlacementApplicationWithdrawalReason.NO_CAPACITY
      WithdrawPlacementRequestReason.errorInPlacementRequest -> PlacementApplicationWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST
      WithdrawPlacementRequestReason.withdrawnByPP -> throw NotAllowedProblem("Withdrawal reason is reserved for internal use")
      WithdrawPlacementRequestReason.relatedApplicationWithdrawn -> throw NotAllowedProblem("Withdrawal reason is reserved for internal use")
      WithdrawPlacementRequestReason.relatedPlacementRequestWithdrawn -> throw NotAllowedProblem("Withdrawal reason is reserved for internal use")
      WithdrawPlacementRequestReason.relatedPlacementApplicationWithdrawn -> throw NotAllowedProblem("Withdrawal reason is reserved for internal use")
      null -> null
    }

    val result = withdrawalService.withdrawPlacementApplication(
      id,
      userService.getUserForRequest(),
      withdrawalReason,
    )

    return ResponseEntity.ok(
      placementApplicationTransformer.transformJpaToApi(extractEntityFromCasResult(result)),
    )
  }
}
