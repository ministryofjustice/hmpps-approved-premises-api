package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1PremisesLocalRestrictionSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesJdbcRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1BedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1OccupancyReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1PremisesLocalRestrictionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1PremisesLocalRestrictionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.repository.CsvJdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.io.OutputStream
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class Cas1PremisesService(
  val premisesRepository: ApprovedPremisesRepository,
  val premisesJdbcRepository: ApprovedPremisesJdbcRepository,
  val bedRepository: BedRepository,
  val premisesService: PremisesService,
  val outOfServiceBedService: Cas1OutOfServiceBedService,
  val spacePlanningService: SpacePlanningService,
  val spaceBookingRepository: Cas1SpaceBookingRepository,
  val cas1OccupancyReportRepository: Cas1OccupancyReportRepository,
  val cas1PremisesLocalRestrictionRepository: Cas1PremisesLocalRestrictionRepository,
  private val clock: Clock,
  private val cas1BedsRepository: Cas1BedsRepository,
) {

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
    val activeSpaceBookingCount = spaceBookingRepository.countActiveSpaceBookings(premisesId).toInt()
    val localRestrictions = cas1PremisesLocalRestrictionRepository.findAllByApprovedPremisesIdAndArchivedFalseOrderByCreatedAtDesc(premisesId)
      .map { restriction ->
        Cas1PremisesLocalRestrictionSummary(
          id = restriction.id,
          description = restriction.description,
          createdAt = restriction.createdAt.toLocalDate(),
        )
      }
    val characteristicPropertyNames = premisesJdbcRepository.findAllCharacteristicPropertyNames(premisesId)

    return CasResult.Success(
      Cas1PremisesInfo(
        entity = premise,
        bedCount = bedCount,
        availableBeds = bedCount - outOfServiceBedsCount - activeSpaceBookingCount,
        outOfServiceBeds = outOfServiceBedsCount,
        localRestrictions = localRestrictions,
        characteristicPropertyNames = characteristicPropertyNames,
      ),
    )
  }

  fun getPremisesBasicInfo(gender: ApprovedPremisesGender?, apAreaId: UUID?, cruManagementAreaId: UUID?) = premisesRepository.findForSummaries(gender, apAreaId, cruManagementAreaId)

  fun findPremisesById(id: UUID) = premisesRepository.findByIdOrNull(id)

  fun premisesExistsById(id: UUID) = premisesRepository.existsById(id)

  fun getPremisesCapacities(
    premisesIds: List<UUID>,
    startDate: LocalDate,
    endDate: LocalDate,
    excludeSpaceBookingId: UUID? = null,
  ): CasResult<Cas1PremisesCapacities> {
    if (premisesIds.isEmpty()) {
      return CasResult.Success(Cas1PremisesCapacities(startDate, endDate, emptyList()))
    }

    val premises = premisesRepository.findAllById(premisesIds)

    if (premises.size != premisesIds.size) {
      val missingIds = premisesIds - premises.map { it.id }
      return CasResult.GeneralValidationError("Could not resolve all premises IDs. Missing IDs are $missingIds")
    }

    if (startDate.isAfter(endDate)) {
      return CasResult.GeneralValidationError("Start Date $startDate should be before End Date $endDate")
    }

    val dateRange = if (ChronoUnit.YEARS.between(startDate, endDate) > 1) {
      log.warn(
        """Capacity requested for more than 2 years, will only return the first few years. 
        |Arrival Date: $startDate, Departure Date: $endDate
        """.trimMargin(),
      )
      DateRange(startDate, startDate.plusYears(2).minusDays(1))
    } else {
      DateRange(startDate, endDate)
    }

    return CasResult.Success(
      Cas1PremisesCapacities(
        startDate = startDate,
        endDate = endDate,
        spacePlanningService.capacity(
          forPremisesIds = premises.map { it.id },
          rangeInclusive = dateRange,
          excludeSpaceBookingId = excludeSpaceBookingId,
        ),
      ),
    )
  }

  fun getActiveBeds(premisesId: UUID) = cas1BedsRepository.bedSummary(
    premisesIds = listOf(premisesId),
    excludeEndedBeds = true,
  )

  fun addLocalRestriction(premisesId: UUID, createdByUserId: UUID, description: String): CasResult<Unit> {
    val restriction = Cas1PremisesLocalRestrictionEntity(
      id = UUID.randomUUID(),
      approvedPremisesId = premisesId,
      description = description,
      createdAt = OffsetDateTime.now(),
      createdByUserId = createdByUserId,
    )

    cas1PremisesLocalRestrictionRepository.saveAndFlush(restriction)

    return CasResult.Success(Unit)
  }

  fun getLocalRestrictions(premisesId: UUID) = cas1PremisesLocalRestrictionRepository.findAllByApprovedPremisesIdAndArchivedFalseOrderByCreatedAtDesc(premisesId)

  fun deleteLocalRestriction(premisesId: UUID, localRestrictionId: UUID): CasResult<Unit> {
    val localRestriction = cas1PremisesLocalRestrictionRepository.findByIdOrNull(localRestrictionId)
      ?: return CasResult.NotFound("localRestriction", localRestrictionId.toString())

    if (localRestriction.approvedPremisesId != premisesId) {
      return CasResult.GeneralValidationError("localRestriction does not belong to premises")
    }

    localRestriction.archived = true

    cas1PremisesLocalRestrictionRepository.save(localRestriction)

    return CasResult.Success(Unit)
  }

  data class Cas1PremisesInfo(
    val entity: ApprovedPremisesEntity,
    val bedCount: Int,
    val availableBeds: Int,
    val outOfServiceBeds: Int,
    val localRestrictions: List<Cas1PremisesLocalRestrictionSummary> = emptyList(),
    val characteristicPropertyNames: List<String>,
  )

  data class Cas1PremisesCapacities(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val results: List<PremiseCapacity>,
  )
}
