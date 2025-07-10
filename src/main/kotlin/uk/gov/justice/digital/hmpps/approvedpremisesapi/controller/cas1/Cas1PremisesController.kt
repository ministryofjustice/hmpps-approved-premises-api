package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1BedDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesBasicSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesDaySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDaySummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.ContentType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1ReportsController.Companion.TIMESTAMP_FORMAT
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generateStreamingResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedSummaryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingDaySummaryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.StaffMemberTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1BedDetailTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1BedSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremiseCapacitySummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremisesDayTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Cas1Controller
@Tag(name = "CAS1 Premises")
class Cas1PremisesController(
  private val userAccessService: UserAccessService,
  private val userService: UserService,
  private val cas1PremisesService: Cas1PremisesService,
  private val cas1PremisesTransformer: Cas1PremisesTransformer,
  private val cas1PremiseCapacityTransformer: Cas1PremiseCapacitySummaryTransformer,
  private val cas1BedService: Cas1BedService,
  private val cas1BedSummaryTransformer: Cas1BedSummaryTransformer,
  private val cas1BedDetailTransformer: Cas1BedDetailTransformer,
  private val cas1PremisesDayTransformer: Cas1PremisesDayTransformer,
  private val cas1SpaceBookingDaySummaryService: Cas1SpaceBookingDaySummaryService,
  private val cas1OutOfServiceBedSummaryService: Cas1OutOfServiceBedSummaryService,
  private val cas1OutOfServiceBedSummaryTransformer: Cas1OutOfServiceBedSummaryTransformer,
  private val staffMemberService: StaffMemberService,
  private val staffMemberTransformer: StaffMemberTransformer,
  private val clock: Clock,
  private val spaceBookingService: Cas1SpaceBookingService,
) {

  @Operation(summary = "Returns a CSV showing premises occupancy for the next 30 days. This does not consider characteristics.")
  @GetMapping(
    value = ["/premises/occupancy-report"],
    produces = ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"],
  )
  fun getOccupancyReport(): ResponseEntity<StreamingResponseBody> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PREMISES_CAPACITY_REPORT_VIEW)

    val timestamp = LocalDateTime.now(clock).format(TIMESTAMP_FORMAT)

    return generateStreamingResponse(
      contentType = ContentType.CSV,
      fileName = "premises-occupancy-$timestamp.csv",
    ) { outputStream ->
      cas1PremisesService.createOccupancyReport(outputStream)
    }
  }

  @Operation(summary = "Lists all beds for the given premises")
  @GetMapping("/premises/{premisesId}/beds")
  fun getBeds(
    @PathVariable premisesId: UUID,
  ): ResponseEntity<List<Cas1PremisesBedSummary>> {
    val premises = cas1PremisesService.findPremiseById(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(cas1PremisesService.getBeds(premisesId).map(cas1BedSummaryTransformer::transformJpaToApi))
  }

  @Operation(summary = "Gets a given bed for a given premises")
  @GetMapping("/premises/{premisesId}/beds/{bedId}")
  fun getBed(
    @PathVariable premisesId: UUID,
    @PathVariable bedId: UUID,
  ): ResponseEntity<Cas1BedDetail> {
    val premises = cas1PremisesService.findPremiseById(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(cas1BedDetailTransformer.transformToApi(extractEntityFromCasResult(cas1BedService.getBedAndRoomCharacteristics(bedId))))
  }

  @Operation(summary = "Returns premises information")
  @GetMapping("/premises/{premisesId}")
  fun getPremisesById(
    @PathVariable premisesId: UUID,
  ): ResponseEntity<Cas1Premises> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PREMISES_VIEW)

    return ResponseEntity
      .ok()
      .body(
        cas1PremisesTransformer.toPremises(
          extractEntityFromCasResult(cas1PremisesService.getPremisesInfo(premisesId)),
        ),
      )
  }

  @Operation(summary = "Provide a summary of all premises, with optional filtering")
  @GetMapping("/premises/summary")
  fun getPremisesSummaries(
    @RequestParam gender: Cas1ApprovedPremisesGender?,
    @RequestParam apAreaId: UUID?,
    @RequestParam cruManagementAreaId: UUID?,
  ): ResponseEntity<List<Cas1PremisesBasicSummary>> = ResponseEntity
    .ok()
    .body(
      cas1PremisesService.getPremises(
        gender = when (gender) {
          Cas1ApprovedPremisesGender.man -> ApprovedPremisesGender.MAN
          Cas1ApprovedPremisesGender.woman -> ApprovedPremisesGender.WOMAN
          null -> null
        },
        apAreaId,
        cruManagementAreaId,
      ).map {
        cas1PremisesTransformer.toPremiseBasicSummary(it)
      },
    )

  /*
  This is a spike endpoint to test performance
  against a prod-like dataset. It will need refining
  once requirements are fully understood
   */
  @Operation(summary = "Provides capacity information for a given date range for all premises")
  @GetMapping("/premises/capacity")
  fun getCapacities(
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
  ): ResponseEntity<List<Cas1PremiseCapacity>> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PREMISES_VIEW)

    val capacities = extractEntityFromCasResult(
      cas1PremisesService.getPremisesCapacities(
        premisesIds = cas1PremisesService.getAllPremisesIds(),
        startDate = startDate,
        endDate = endDate,
        excludeSpaceBookingId = null,
      ),
    )

    return ResponseEntity.ok().body(
      capacities.map { cas1PremiseCapacityTransformer.toCas1PremiseCapacitySummary(it) },
    )
  }

  @Operation(summary = "Provides capacity information for a given date range")
  @GetMapping("/premises/{premisesId}/capacity")
  fun getCapacity(
    @PathVariable premisesId: UUID,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    @RequestParam excludeSpaceBookingId: UUID?,
  ): ResponseEntity<Cas1PremiseCapacity> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PREMISES_VIEW)

    cas1PremisesService.findPremiseById(premisesId)
      ?: throw NotFoundProblem(premisesId.toString(), "premises")

    val premiseCapacity = cas1PremisesService.getPremisesCapacities(
      premisesIds = listOf(premisesId),
      startDate = startDate,
      endDate = endDate,
      excludeSpaceBookingId = excludeSpaceBookingId,
    )

    return ResponseEntity.ok().body(
      cas1PremiseCapacityTransformer.toCas1PremiseCapacitySummary(
        premiseCapacity = extractEntityFromCasResult(premiseCapacity)[0],
      ),
    )
  }

  @Operation(summary = "Provides a summary of capacity, space bookings and out of service beds for a premise on a given day")
  @GetMapping("/premises/{premisesId}/day-summary/{date}")
  fun getDaySummary(
    @PathVariable premisesId: UUID,
    @PathVariable date: LocalDate,
    @RequestParam bookingsCriteriaFilter: List<Cas1SpaceBookingCharacteristic>?,
    @RequestParam bookingsSortDirection: SortDirection?,
    @RequestParam bookingsSortBy: Cas1SpaceBookingDaySummarySortField?,
    @RequestParam excludeSpaceBookingId: UUID?,
  ): ResponseEntity<Cas1PremisesDaySummary> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PREMISES_VIEW)
    val premises = cas1PremisesService.findPremiseById(premisesId)
      ?: throw NotFoundProblem(premisesId.toString(), "premises")

    return ResponseEntity.ok().body(
      cas1PremisesDayTransformer.toCas1PremisesDaySummary(
        date = date,
        premisesCapacity = cas1PremiseCapacityTransformer.toCas1PremiseCapacitySummary(
          premiseCapacity = extractEntityFromCasResult(
            cas1PremisesService.getPremisesCapacities(
              premisesIds = listOf(premisesId),
              startDate = date,
              endDate = date,
              excludeSpaceBookingId = excludeSpaceBookingId,
            ),
          )[0],
        ).capacity.first(),
        spaceBookings = extractEntityFromCasResult(
          cas1SpaceBookingDaySummaryService.getBookingDaySummaries(
            premisesId = premisesId,
            date = date,
            bookingsCriteriaFilter = bookingsCriteriaFilter,
            bookingsSortBy = bookingsSortBy ?: Cas1SpaceBookingDaySummarySortField.PERSON_NAME,
            bookingsSortDirection = bookingsSortDirection ?: SortDirection.desc,
            excludeSpaceBookingId = excludeSpaceBookingId,
          ),
        ),
        outOfServiceBeds = extractEntityFromCasResult(
          cas1OutOfServiceBedSummaryService.getOutOfServiceBedSummaries(
            premisesId = premisesId,
            apAreaId = premises.probationRegion.apArea!!.id,
            date = date,
          ),
        ).map(cas1OutOfServiceBedSummaryTransformer::toCas1OutOfServiceBedSummary),
        spaceBookingSummaries =
        spaceBookingService.getSpaceBookingsByPremisesIdAndCriteriaForDate(
          premises = premises,
          date = date,
          bookingsCriteriaFilter = bookingsCriteriaFilter,
          bookingsSortBy = bookingsSortBy ?: Cas1SpaceBookingDaySummarySortField.PERSON_NAME,
          bookingsSortDirection = bookingsSortDirection ?: SortDirection.desc,
          excludeSpaceBookingId = excludeSpaceBookingId,
          user = userService.getUserForRequest(),
        ),
      ),
    )
  }

  @Operation(summary = "Returns the staff that work at an approved premises")
  @GetMapping("/premises/{premisesId}/staff")
  fun getStaff(
    @PathVariable premisesId: UUID,
  ): ResponseEntity<List<StaffMember>> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PREMISES_VIEW)

    val premises = cas1PremisesService.findPremiseById(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val staffMembersResult = staffMemberService.getStaffMembersForQCode(premises.qCode)

    if (staffMembersResult is CasResult.NotFound) {
      return ResponseEntity.ok(emptyList())
    }

    return ResponseEntity.ok(
      extractEntityFromCasResult(staffMembersResult)
        .content
        .map(staffMemberTransformer::transformDomainToApi),
    )
  }
}
