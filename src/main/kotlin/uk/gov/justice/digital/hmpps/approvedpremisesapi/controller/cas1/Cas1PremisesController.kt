package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.PremisesCas1Delegate
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedSummaryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingDaySummaryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1BedDetailTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1BedSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremiseCapacitySummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremisesDayTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.LocalDate
import java.util.UUID

@Service
class Cas1PremisesController(
  val userAccessService: UserAccessService,
  val cas1PremisesService: Cas1PremisesService,
  val cas1PremisesTransformer: Cas1PremisesTransformer,
  val cas1PremiseCapacityTransformer: Cas1PremiseCapacitySummaryTransformer,
  private val cas1BedService: Cas1BedService,
  private val cas1BedSummaryTransformer: Cas1BedSummaryTransformer,
  private val cas1BedDetailTransformer: Cas1BedDetailTransformer,
  private val cas1PremisesDayTransformer: Cas1PremisesDayTransformer,
  private val cas1SpaceBookingDaySummaryService: Cas1SpaceBookingDaySummaryService,
  private val cas1OutOfServiceBedSummaryService: Cas1OutOfServiceBedSummaryService,
  private val cas1OutOfServiceBedSummaryTransformer: Cas1OutOfServiceBedSummaryTransformer,
) : PremisesCas1Delegate {

  override fun getBeds(premisesId: UUID): ResponseEntity<List<Cas1PremisesBedSummary>> {
    val premises = cas1PremisesService.findPremiseById(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(cas1PremisesService.getBeds(premisesId).map(cas1BedSummaryTransformer::transformJpaToApi))
  }

  override fun getBed(premisesId: UUID, bedId: UUID): ResponseEntity<Cas1BedDetail> {
    val premises = cas1PremisesService.findPremiseById(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(cas1BedDetailTransformer.transformToApi(extractEntityFromCasResult(cas1BedService.getBedAndRoomCharacteristics(bedId))))
  }

  override fun getPremisesById(premisesId: UUID): ResponseEntity<Cas1Premises> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PREMISES_VIEW)

    return ResponseEntity
      .ok()
      .body(
        cas1PremisesTransformer.toPremises(
          extractEntityFromCasResult(cas1PremisesService.getPremisesInfo(premisesId)),
        ),
      )
  }

  override fun getPremisesSummaries(
    gender: Cas1ApprovedPremisesGender?,
    apAreaId: UUID?,
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
      ).map {
        cas1PremisesTransformer.toPremiseBasicSummary(it)
      },
    )

  override fun getCapacity(
    premisesId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    excludeSpaceBookingId: UUID?,
  ): ResponseEntity<Cas1PremiseCapacity> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PREMISES_VIEW)

    val premiseCapacity = cas1PremisesService.getPremiseCapacity(
      premisesId = premisesId,
      startDate = startDate,
      endDate = endDate,
      excludeSpaceBookingId = excludeSpaceBookingId,
    )

    return ResponseEntity.ok().body(
      cas1PremiseCapacityTransformer.toCas1PremiseCapacitySummary(
        premiseCapacity = extractEntityFromCasResult(premiseCapacity),
      ),
    )
  }

  override fun getDaySummary(
    premisesId: UUID,
    date: LocalDate,
    bookingsCriteriaFilter: List<Cas1SpaceBookingCharacteristic>?,
    bookingsSortDirection: SortDirection?,
    bookingsSortBy: Cas1SpaceBookingDaySummarySortField?,
    excludeSpaceBookingId: UUID?,
  ): ResponseEntity<Cas1PremisesDaySummary> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PREMISES_VIEW)
    val premises = cas1PremisesService.findPremiseById(premisesId)

    return ResponseEntity.ok().body(
      cas1PremisesDayTransformer.toCas1PremisesDaySummary(
        date = date,
        premisesCapacity = cas1PremiseCapacityTransformer.toCas1PremiseCapacitySummary(
          premiseCapacity = extractEntityFromCasResult(
            cas1PremisesService.getPremiseCapacity(
              premisesId = premisesId,
              startDate = date,
              endDate = date,
              excludeSpaceBookingId = excludeSpaceBookingId,
            ),
          ),
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
            apAreaId = premises?.probationRegion?.apArea!!.id,
            date = date,
          ),
        ).map(cas1OutOfServiceBedSummaryTransformer::toCas1OutOfServiceBedSummary),
      ),
    )
  }
}
