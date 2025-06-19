package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OverbookingRange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1OccupancyReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.repository.CsvJdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.io.OutputStream
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class Cas1PremisesService(
  val premisesRepository: ApprovedPremisesRepository,
  val bedRepository: BedRepository,
  val premisesService: PremisesService,
  val outOfServiceBedService: Cas1OutOfServiceBedService,
  val spacePlanningService: SpacePlanningService,
  val spaceBookingRepository: Cas1SpaceBookingRepository,
  val featureFlagService: FeatureFlagService,
  val cas1OccupancyReportRepository: Cas1OccupancyReportRepository,
  private val clock: Clock,
) {
  companion object {
    private const val OVERBOOKING_RANGE_DURATION_WEEKS = 12L
  }

  private val log = LoggerFactory.getLogger(this::class.java)

  @SuppressWarnings("MagicNumber")
  fun createOccupancyReport(outputStream: OutputStream) {
    val now = LocalDate.now(clock)

    CsvJdbcResultSetConsumer(
      outputStream = outputStream,
    ).use { consumer ->
      cas1OccupancyReportRepository.generate(
        fromInclusive = now,
        toInclusive = now.plusDays(29),
        jbdcResultSetConsumer = consumer,
      )
    }
  }

  fun getPremisesInfo(premisesId: UUID): CasResult<Cas1PremisesInfo> {
    val premise = premisesRepository.findByIdOrNull(premisesId)
      ?: return CasResult.NotFound("premises", premisesId.toString())

    val bedCount = premisesService.getBedCount(premise)
    val outOfServiceBedsCount = outOfServiceBedService.getCurrentOutOfServiceBedsCountForPremisesId(premisesId)
    val spaceBookingCount = spaceBookingRepository.countActiveSpaceBookings(premisesId).toInt()

    val overbookingSummary = if (!featureFlagService.getBooleanFlag("cas1-disable-overbooking-summary")) {
      premise.takeIf { it.supportsSpaceBookings }?.let { buildOverBookingSummary(it) } ?: emptyList()
    } else {
      emptyList()
    }

    return CasResult.Success(
      Cas1PremisesInfo(
        entity = premise,
        bedCount = bedCount,
        availableBeds = bedCount - outOfServiceBedsCount - spaceBookingCount,
        outOfServiceBeds = outOfServiceBedsCount,
        overbookingSummary = overbookingSummary,
      ),
    )
  }

  private fun buildOverBookingSummary(premises: ApprovedPremisesEntity): List<Cas1OverbookingRange> {
    val premisesCapacitySummary = spacePlanningService.capacity(
      premises,
      rangeInclusive = DateRange(LocalDate.now(), LocalDate.now().plusWeeks(OVERBOOKING_RANGE_DURATION_WEEKS).minusDays(1)),
      excludeSpaceBookingId = null,
    )

    val overbookedDays = premisesCapacitySummary
      .byDay
      .filter { it.isPremiseOverbooked() }

    return Cas1PremiseOverbookingCalculator().calculate(overbookedDays)
  }

  fun getPremises(gender: ApprovedPremisesGender?, apAreaId: UUID?, cruManagementAreaId: UUID?) = premisesRepository.findForSummaries(gender, apAreaId, cruManagementAreaId)

  fun findPremiseById(id: UUID) = premisesRepository.findByIdOrNull(id)

  fun premiseExistsById(id: UUID) = premisesRepository.existsById(id)

  fun getPremiseCapacity(
    premisesId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    excludeSpaceBookingId: UUID?,
  ): CasResult<SpacePlanningService.PremiseCapacitySummary> {
    val premises = premisesRepository.findByIdOrNull(premisesId)
      ?: return CasResult.NotFound("premises", premisesId.toString())

    if (startDate.isAfter(endDate)) {
      return CasResult.GeneralValidationError("Start Date $startDate should be before End Date $endDate")
    }

    val dateRange = if (ChronoUnit.YEARS.between(startDate, endDate) > 1) {
      log.warn(
        """Capacity requested for more than 2 years, will only return the first few years. 
        |Arrival Date: $startDate, Departure Date: $endDate, Premises: ${premises.name}
        """.trimMargin(),
      )
      DateRange(startDate, startDate.plusYears(2).minusDays(1))
    } else {
      DateRange(startDate, endDate)
    }

    return CasResult.Success(
      spacePlanningService.capacity(
        premises = premises,
        rangeInclusive = dateRange,
        excludeSpaceBookingId = excludeSpaceBookingId,
      ),
    )
  }

  fun getBeds(premisesId: UUID) = bedRepository.findAllCas1BedSummariesForPremises(premisesId)

  data class Cas1PremisesInfo(
    val entity: ApprovedPremisesEntity,
    val bedCount: Int,
    val availableBeds: Int,
    val outOfServiceBeds: Int,
    val overbookingSummary: List<Cas1OverbookingRange>,
  )
}
