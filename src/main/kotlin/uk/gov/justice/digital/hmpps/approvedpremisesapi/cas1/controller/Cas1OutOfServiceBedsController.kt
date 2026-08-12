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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Temporality
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas1OutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1NewOutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1NewOutOfServiceBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1OutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1OutOfServiceBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1OutOfServiceBedSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedCancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.LocalDate
import java.util.UUID

@Cas1Controller
@Tag(name = "out-of-service beds")
class Cas1OutOfServiceBedsController(
  private val userAccessService: Cas1UserAccessService,
  private val premisesService: Cas1PremisesService,
  private val outOfServiceBedService: Cas1OutOfServiceBedService,
  private val outOfServiceBedTransformer: Cas1OutOfServiceBedTransformer,
  private val outOfServiceBedCancellationTransformer: Cas1OutOfServiceBedCancellationTransformer,
) {

  @Operation(
    summary = "Lists all Out-Of-Service Beds entries",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = Cas1OutOfServiceBed::class)))]),
    ],
  )
  @GetMapping(
    value = ["/out-of-service-beds"],
    produces = ["application/json"],
  )
  fun getOutOfServiceBeds(
    @RequestParam temporality: List<Temporality>?,
    @RequestParam premisesId: UUID?,
    @RequestParam apAreaId: UUID?,
    @RequestParam sortDirection: SortDirection?,
    @RequestParam sortBy: Cas1OutOfServiceBedSortField?,
    @RequestParam page: Int?,
    @RequestParam perPage: Int?,
  ): ResponseEntity<List<Cas1OutOfServiceBed>> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_VIEW_OUT_OF_SERVICE_BEDS)

    val (outOfServiceBeds, pageMetadata) = outOfServiceBedService.getOutOfServiceBeds(
      temporality?.toSet() ?: setOf(Temporality.current, Temporality.future),
      premisesId,
      apAreaId,
      PageCriteria(
        sortBy ?: Cas1OutOfServiceBedSortField.startDate,
        sortDirection ?: SortDirection.asc,
        page,
        perPage,
      ),
    )

    return ResponseEntity
      .ok()
      .headers(pageMetadata?.toHeaders())
      .body(outOfServiceBeds.map(outOfServiceBedTransformer::transformJpaToApi))
  }

  @Operation(
    summary = "Lists all Out-Of-Service Beds entries for the Premises",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = Cas1OutOfServiceBed::class)))]),
    ],
  )
  @GetMapping(
    value = ["/premises/{premisesId}/out-of-service-beds"],
    produces = ["application/json"],
  )
  fun getOutOfServiceBedsForPremises(
    @Parameter(description = "ID of the premises to show out-of-service beds for")
    @PathVariable premisesId: UUID,
  ): ResponseEntity<List<Cas1OutOfServiceBed>> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_VIEW_OUT_OF_SERVICE_BEDS)

    tryGetApprovedPremises(premisesId)

    val outOfServiceBeds = outOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premisesId)

    return ResponseEntity.ok(outOfServiceBeds.map(outOfServiceBedTransformer::transformJpaToApi))
  }

  @Operation(
    summary = "Posts a cancellation to a specified out-of-service bed",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas1OutOfServiceBedCancellation::class))]),
      ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
      ApiResponse(responseCode = "404", description = "invalid premises ID or out-of-service bed ID", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @PostMapping(
    value = ["/premises/{premisesId}/out-of-service-beds/{outOfServiceBedId}/cancellations"],
    produces = ["application/json", "application/problem+json"],
    consumes = ["application/json"],
  )
  fun cancelOutOfServiceBed(
    @Parameter(description = "ID of the premises the cancellation is related to")
    @PathVariable premisesId: UUID,
    @Parameter(description = "ID of the out-of-service bed")
    @PathVariable outOfServiceBedId: UUID,
    @Parameter(description = "details of the cancellation")
    @RequestBody body: Cas1NewOutOfServiceBedCancellation,
  ): ResponseEntity<Cas1OutOfServiceBedCancellation> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_OUT_OF_SERVICE_BED_CANCEL)

    val premises = tryGetApprovedPremises(premisesId)

    val outOfServiceBed = premises
      .outOfServiceBeds
      .firstOrNull { it.id == outOfServiceBedId }
      ?: throw NotFoundProblem(outOfServiceBedId, "OutOfServiceBed")

    val cancelOutOfServiceBedResult = outOfServiceBedService.cancelOutOfServiceBed(
      outOfServiceBed = outOfServiceBed,
      notes = body.notes,
    )

    return ResponseEntity.ok(
      outOfServiceBedCancellationTransformer.transformJpaToApi(
        extractEntityFromCasResult(cancelOutOfServiceBedResult),
      ),
    )
  }

  @Operation(
    summary = "Returns a specific out-of-service bed for a premises",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas1OutOfServiceBed::class))]),
      ApiResponse(responseCode = "404", description = "invalid premises or out-of-service bed ID", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @GetMapping(
    value = ["/premises/{premisesId}/out-of-service-beds/{outOfServiceBedId}"],
    produces = ["application/json"],
  )
  fun getOutOfServiceBed(
    @Parameter(description = "ID of the premises the out-of-service bed is related to")
    @PathVariable premisesId: UUID,
    @Parameter(description = "ID of the out-of-service bed")
    @PathVariable outOfServiceBedId: UUID,
  ): ResponseEntity<Cas1OutOfServiceBed> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_VIEW_OUT_OF_SERVICE_BEDS)

    val premises = tryGetApprovedPremises(premisesId)

    val outOfServiceBed = premises.outOfServiceBeds.firstOrNull { it.id == outOfServiceBedId }
      ?: throw NotFoundProblem(outOfServiceBedId, "OutOfServiceBed")

    return ResponseEntity.ok(outOfServiceBedTransformer.transformJpaToApi(outOfServiceBed))
  }

  @Operation(
    summary = "Updates an out-of-service bed for a premises",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas1OutOfServiceBed::class))]),
      ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
      ApiResponse(responseCode = "404", description = "invalid premises ID or booking ID", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @PutMapping(
    value = ["/premises/{premisesId}/out-of-service-beds/{outOfServiceBedId}"],
    produces = ["application/json", "application/problem+json"],
    consumes = ["application/json"],
  )
  fun updateOutOfServiceBed(
    @Parameter(description = "ID of the premises the out-of-service bed is related to")
    @PathVariable premisesId: UUID,
    @Parameter(description = "ID of the out-of-service bed")
    @PathVariable outOfServiceBedId: UUID,
    @Parameter(description = "details of the out-of-service bed")
    @RequestBody body: UpdateCas1OutOfServiceBed,
  ): ResponseEntity<Cas1OutOfServiceBed> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_OUT_OF_SERVICE_BED_CREATE)

    val premises = tryGetApprovedPremises(premisesId)
    val outOfServiceBed = premises.outOfServiceBeds.firstOrNull { it.id == outOfServiceBedId } ?: throw NotFoundProblem(outOfServiceBedId, "OutOfServiceBed")

    throwIfOutOfServiceBedDatesConflict(body.startDate, body.endDate, outOfServiceBedId, outOfServiceBed.bed.id)

    val updateOutOfServiceBedResult = outOfServiceBedService.updateOutOfServiceBed(
      outOfServiceBedId,
      body.startDate,
      body.endDate,
      body.reason,
      body.referenceNumber,
      body.notes,
    )

    return ResponseEntity.ok(
      outOfServiceBedTransformer.transformJpaToApi(
        extractEntityFromCasResult(updateOutOfServiceBedResult),
      ),
    )
  }

  @Operation(
    summary = "Posts an out-of-service bed to a specified approved premises",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas1OutOfServiceBed::class))]),
      ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
      ApiResponse(responseCode = "404", description = "invalid premises ID or booking ID", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @PostMapping(
    value = ["/premises/{premisesId}/out-of-service-beds"],
    produces = ["application/json", "application/problem+json"],
    consumes = ["application/json"],
  )
  fun createOutOfServiceBed(
    @Parameter(description = "ID of the premises the out-of-service bed is related to")
    @PathVariable premisesId: UUID,
    @Parameter(description = "details of the out-of-service bed")
    @RequestBody body: Cas1NewOutOfServiceBed,
  ): ResponseEntity<Cas1OutOfServiceBed> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_OUT_OF_SERVICE_BED_CREATE)

    val premises = tryGetApprovedPremises(premisesId)

    throwIfOutOfServiceBedDatesConflict(body.startDate, body.endDate, null, body.bedId)

    val result = outOfServiceBedService.createOutOfServiceBed(
      premises = premises,
      startDate = body.startDate,
      endDate = body.endDate,
      reasonId = body.reason,
      referenceNumber = body.referenceNumber,
      notes = body.notes,
      bedId = body.bedId,
    )

    val outOfServiceBed = extractEntityFromCasResult(result)

    return ResponseEntity.ok(outOfServiceBedTransformer.transformJpaToApi(outOfServiceBed))
  }

  private fun tryGetApprovedPremises(premisesId: UUID): ApprovedPremisesEntity = premisesService.getPremises(premisesId) as? ApprovedPremisesEntity ?: throw NotFoundProblem(premisesId, "Premises")

  private val ApprovedPremisesEntity.outOfServiceBeds: List<Cas1OutOfServiceBedEntity>
    get() = outOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(this.id)

  private fun throwIfOutOfServiceBedDatesConflict(
    startDate: LocalDate,
    endDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID,
  ) {
    outOfServiceBedService.getOutOfServiceBedWithConflictingDates(startDate, endDate, thisEntityId, bedId)?.let {
      throw ConflictProblem(it.id, "An out-of-service bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates")
    }
  }
}
