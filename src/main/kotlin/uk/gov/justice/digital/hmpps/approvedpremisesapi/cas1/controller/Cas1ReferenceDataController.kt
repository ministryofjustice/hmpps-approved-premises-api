package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DepartureReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NonArrivalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1OutOfServiceBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository.Companion.BED_ON_HOLD_REASON_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRejectionReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.MoveOnCategoryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NonArrivalReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1CruManagementAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedReasonTransformer

@Cas1Controller
@Tag(name = "Reference Data")
class Cas1ReferenceDataController(
  private val cas1OutOfServiceBedReasonTransformer: Cas1OutOfServiceBedReasonTransformer,
  private val cas1OutOfServiceBedReasonRepository: Cas1OutOfServiceBedReasonRepository,
  private val cas1CruManagementAreaTransformer: Cas1CruManagementAreaTransformer,
  private val cas1CruManagementAreaRepository: Cas1CruManagementAreaRepository,
  private val departureReasonRepository: DepartureReasonRepository,
  private val departureReasonTransformer: DepartureReasonTransformer,
  private val nonArrivalReasonRepository: NonArrivalReasonRepository,
  private val nonArrivalReasonTransformer: NonArrivalReasonTransformer,
  private val moveOnCategoryRepository: MoveOnCategoryRepository,
  private val moveOnCategoryTransformer: MoveOnCategoryTransformer,
  private val changeRequestReasonRepository: Cas1ChangeRequestReasonRepository,
  private val changeRequestRejectionReasonRepository: Cas1ChangeRequestRejectionReasonRepository,
  private val userService: UserService,
) {

  @Operation(
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = NamedId::class)))]),
    ],
  )
  @GetMapping(
    value = ["/reference-data/change-request-reasons/{changeRequestType}"],
    produces = ["application/json"],
  )
  fun getChangeRequestReasons(
    @Parameter(schema = Schema(allowableValues = ["placementAppeal", "placementExtension", "plannedTransfer"]))
    @PathVariable changeRequestType: Cas1ChangeRequestType,
  ): ResponseEntity<List<NamedId>> = ResponseEntity.ok(
    changeRequestReasonRepository.findByChangeRequestTypeAndArchivedIsFalse(
      when (changeRequestType) {
        Cas1ChangeRequestType.PLACEMENT_APPEAL -> ChangeRequestType.PLACEMENT_APPEAL
        Cas1ChangeRequestType.PLACEMENT_EXTENSION -> ChangeRequestType.PLACEMENT_EXTENSION
        Cas1ChangeRequestType.PLANNED_TRANSFER -> ChangeRequestType.PLANNED_TRANSFER
      },
    ).map { NamedId(it.id, it.code) },
  )

  @Operation(
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = NamedId::class)))]),
    ],
  )
  @GetMapping(
    value = ["/reference-data/change-request-rejection-reasons/{changeRequestType}"],
    produces = ["application/json"],
  )
  fun getChangeRequestRejectionReasons(
    @Parameter(schema = Schema(allowableValues = ["placementAppeal", "placementExtension", "plannedTransfer"]))
    @PathVariable changeRequestType: Cas1ChangeRequestType,
  ): ResponseEntity<List<NamedId>> = ResponseEntity.ok(
    changeRequestRejectionReasonRepository.findByChangeRequestTypeAndArchivedIsFalse(
      when (changeRequestType) {
        Cas1ChangeRequestType.PLACEMENT_APPEAL -> ChangeRequestType.PLACEMENT_APPEAL
        Cas1ChangeRequestType.PLACEMENT_EXTENSION -> ChangeRequestType.PLACEMENT_EXTENSION
        Cas1ChangeRequestType.PLANNED_TRANSFER -> ChangeRequestType.PLANNED_TRANSFER
      },
    ).map { NamedId(it.id, it.code) },
  )

  @Operation(
    summary = "Lists all reasons for beds going out of service",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = Cas1OutOfServiceBedReason::class)))]),
    ],
  )
  @GetMapping(
    value = ["/reference-data/out-of-service-bed-reasons"],
    produces = ["application/json"],
  )
  fun getOutOfServiceBedReasons(): ResponseEntity<List<Cas1OutOfServiceBedReason>> {
    val user = userService.getUserForRequest()

    return ResponseEntity.ok(
      cas1OutOfServiceBedReasonRepository.findActive()
        .filter { it.id != BED_ON_HOLD_REASON_ID || user.hasPermission(UserPermission.CAS1_OUT_OF_SERVICE_BED_CREATE_BED_ON_HOLD) }
        .map { reason ->
          cas1OutOfServiceBedReasonTransformer.transformJpaToApi(reason)
        },
    )
  }

  @Operation(
    summary = "Lists all CRU Management Areas",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = Cas1CruManagementArea::class)))]),
    ],
  )
  @GetMapping(
    value = ["/reference-data/cru-management-areas"],
    produces = ["application/json"],
  )
  fun getCruManagementAreas(): ResponseEntity<List<Cas1CruManagementArea>> = ResponseEntity.ok(
    cas1CruManagementAreaRepository.findAll()
      .map { cas1CruManagementAreaTransformer.transformJpaToApi(it) },
  )

  @Operation(
    summary = "Lists all active departure reasons",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = DepartureReason::class)))]),
    ],
  )
  @GetMapping(
    value = ["/reference-data/departure-reasons"],
    produces = ["application/json"],
  )
  fun getDepartureReasons(): ResponseEntity<List<DepartureReason>> = ResponseEntity.ok(
    departureReasonRepository.findActiveForCas1()
      .map { departureReasonTransformer.transformJpaToApi(it) },
  )

  @Operation(
    summary = "Lists all active move-on categories",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = MoveOnCategory::class)))]),
    ],
  )
  @GetMapping(
    value = ["/reference-data/move-on-categories"],
    produces = ["application/json"],
  )
  fun getMoveOnCategories(): ResponseEntity<List<MoveOnCategory>> = ResponseEntity.ok(
    moveOnCategoryRepository.findActiveForCas1()
      .map(moveOnCategoryTransformer::transformJpaToApi),
  )

  @Operation(
    summary = "Lists all active non-arrivals reasons",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = NonArrivalReason::class)))]),
    ],
  )
  @GetMapping(
    value = ["/reference-data/non-arrival-reasons"],
    produces = ["application/json"],
  )
  fun getNonArrivalReasons(): ResponseEntity<List<NonArrivalReason>> = ResponseEntity.ok(
    nonArrivalReasonRepository.findAllActiveReasons()
      .map(nonArrivalReasonTransformer::transformJpaToApi),
  )
}
