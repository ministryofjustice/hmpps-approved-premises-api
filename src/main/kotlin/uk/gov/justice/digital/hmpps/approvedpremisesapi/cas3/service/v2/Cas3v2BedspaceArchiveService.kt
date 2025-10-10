package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ValidationMessage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3PremisesService.Companion.MAX_DAYS_ARCHIVE_BEDSPACE_IN_PAST
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3PremisesService.Companion.MAX_MONTHS_ARCHIVE_BEDSPACE_IN_FUTURE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_BEDSPACE_ARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult.Cas3FieldValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
class Cas3v2BedspaceArchiveService(
  private val cas3BedspacesRepository: Cas3BedspacesRepository,
  private val cas3PremisesRepository: Cas3PremisesRepository,
  private val cas3v2BookingRepository: Cas3v2BookingRepository,
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
  private val cas3v2DomainEventService: Cas3v2DomainEventService,
  private val workingDayService: WorkingDayService,
  private val objectMapper: ObjectMapper,
  private val clock: Clock,
) {

  @Transactional
  fun archiveBedspace(
    bedspaceId: UUID,
    premises: Cas3PremisesEntity,
    endDate: LocalDate,
  ): CasResult<Cas3BedspacesEntity> = validatedCasResult {
    val bedspace = cas3BedspacesRepository.findByIdOrNull(bedspaceId)
      ?: return CasResult.NotFound("Bedspace", bedspaceId.toString())

    if (endDate.isBefore(LocalDate.now(clock).minusDays(MAX_DAYS_ARCHIVE_BEDSPACE_IN_PAST))) {
      return "$.endDate" hasSingleValidationError "invalidEndDateInThePast"
    }

    if (endDate.isAfter(LocalDate.now(clock).plusMonths(MAX_MONTHS_ARCHIVE_BEDSPACE_IN_FUTURE))) {
      return "$.endDate" hasSingleValidationError "invalidEndDateInTheFuture"
    }

    if (endDate.isBefore(bedspace.startDate)) {
      return Cas3FieldValidationError(
        mapOf(
          "$.endDate" to Cas3ValidationMessage(
            entityId = bedspace.id.toString(),
            message = "endDateBeforeBedspaceStartDate",
            value = bedspace.startDate.toString(),
          ),
        ),
      )
    }

    cas3v2DomainEventService.getBedspaceActiveDomainEvents(bedspace.id, listOf(CAS3_BEDSPACE_ARCHIVED))
      .sortedByDescending { it.createdAt }
      .asSequence()
      .map { objectMapper.readValue(it.data, CAS3BedspaceArchiveEvent::class.java).eventDetails.endDate }
      .firstOrNull { it >= endDate }
      ?.let { archiveDate ->
        return Cas3FieldValidationError(
          mapOf(
            "$.endDate" to Cas3ValidationMessage(
              entityId = bedspace.id.toString(),
              message = "endDateOverlapPreviousBedspaceArchiveEndDate",
              value = archiveDate.toString(),
            ),
          ),
        )
      }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val domainEventTransactionId = UUID.randomUUID()

    canArchiveBedspace(bedspaceId, endDate)?.let { return it }

    val updatedBedspace = archiveBedspaceAndSaveDomainEvent(bedspace, premises.id, endDate, domainEventTransactionId)

    archivePremisesIfAllBedspacesArchived(premises, domainEventTransactionId)

    return success(updatedBedspace)
  }

  private fun canArchiveBedspace(bedspaceId: UUID, endDate: LocalDate) = canArchiveBedspace(filterByPremisesId = null, filterByBedspaceId = bedspaceId, endDate = endDate)

  @SuppressWarnings("CyclomaticComplexMethod")
  private fun canArchiveBedspace(filterByPremisesId: UUID?, filterByBedspaceId: UUID?, endDate: LocalDate): Cas3FieldValidationError<Cas3BedspacesEntity>? {
    val allBookings = when {
      filterByPremisesId != null -> cas3v2BookingRepository.findActiveOverlappingBookingByPremisesId(filterByPremisesId, LocalDate.now(clock)).sortedByDescending { it.departureDate }
      filterByBedspaceId != null -> cas3v2BookingRepository.findActiveOverlappingBookingByBedspace(filterByBedspaceId, LocalDate.now(clock)).sortedByDescending { it.departureDate }
      else -> emptyList()
    }
    val overlapBookings = allBookings.filter { it.departureDate > endDate }
    val lastOverlapVoid = when {
      filterByPremisesId != null -> cas3VoidBedspacesRepository.findOverlappingBedspaceEndDateByPremisesId(filterByPremisesId, endDate).maxByOrNull { it.endDate }
      filterByBedspaceId != null -> cas3VoidBedspacesRepository.findOverlappingBedspaceEndDateV2(filterByBedspaceId, endDate).maxByOrNull { it.endDate }
      else -> null
    }

    val lastBookingsTurnaroundDate = allBookings
      .asSequence()
      .mapNotNull { booking ->
        val turnaroundDate = workingDayService.addWorkingDays(booking.departureDate, booking.turnaround?.workingDayCount ?: 0)
        if (turnaroundDate > endDate) {
          booking.id to turnaroundDate
        } else {
          null
        }
      }
      .toMap()

    val (lastTurnaroundBookingId, lastTurnaroundDate) = lastBookingsTurnaroundDate.entries.maxByOrNull { it.value }?.let { it.key to it.value } ?: Pair(null, null)

    return when {
      isVoidLastOverlapBedspaceArchiveDate(lastOverlapVoid, lastTurnaroundDate) -> {
        Cas3FieldValidationError(
          mapOf(
            "$.endDate" to Cas3ValidationMessage(
              entityId = lastOverlapVoid?.bedspace?.id.toString(),
              message = "existingVoid",
              value = lastOverlapVoid?.endDate?.plusDays(1).toString(),
            ),
          ),
        )
      }
      lastTurnaroundDate != null -> {
        val lastOverlapBooking = getLastBookingOverlapBedspaceArchiveDate(overlapBookings, lastBookingsTurnaroundDate, lastTurnaroundDate)
        if (lastOverlapBooking != null && lastOverlapBooking.departureDate == lastTurnaroundDate) {
          Cas3FieldValidationError(
            mapOf(
              "$.endDate" to Cas3ValidationMessage(
                entityId = lastOverlapBooking.bedspace.id.toString(),
                message = "existingBookings",
                value = lastOverlapBooking.departureDate.plusDays(1).toString(),
              ),
            ),
          )
        } else {
          Cas3FieldValidationError(
            mapOf(
              "$.endDate" to Cas3ValidationMessage(
                entityId = allBookings.firstOrNull { it.id == lastTurnaroundBookingId }?.bedspace?.id.toString(),
                message = "existingTurnaround",
                value = lastTurnaroundDate.plusDays(1).toString(),
              ),
            ),
          )
        }
      }
      else -> null
    }
  }

  private fun isVoidLastOverlapBedspaceArchiveDate(
    lastOverlapVoid: Cas3VoidBedspaceEntity?,
    lastTurnaroundDate: LocalDate?,
  ) = lastOverlapVoid != null && (lastTurnaroundDate == null || (lastTurnaroundDate < lastOverlapVoid.endDate))

  private fun getLastBookingOverlapBedspaceArchiveDate(
    overlapBookings: List<Cas3BookingEntity>,
    lastBookingsTurnaroundDate: Map<UUID, LocalDate>,
    lastTurnaroundDate: LocalDate?,
  ): Cas3BookingEntity? {
    val lastOverlapBookingId = lastBookingsTurnaroundDate.entries.firstOrNull { it.value == lastTurnaroundDate }?.key
    return overlapBookings.firstOrNull { it.id == lastOverlapBookingId }
  }

  private fun archiveBedspaceAndSaveDomainEvent(bedspace: Cas3BedspacesEntity, premisesId: UUID, endDate: LocalDate, transactionId: UUID): Cas3BedspacesEntity {
    val originalEndDate = bedspace.endDate
    bedspace.endDate = endDate
    val updatedBedspace = cas3BedspacesRepository.save(bedspace)
    cas3v2DomainEventService.saveBedspaceArchiveEvent(updatedBedspace, premisesId, originalEndDate, transactionId)
    return updatedBedspace
  }

  private fun archivePremisesIfAllBedspacesArchived(premises: Cas3PremisesEntity, transactionId: UUID) {
    val bedspaces = cas3BedspacesRepository.findByPremisesId(premises.id)
    val lastBedspaceEndDate = bedspaces
      .asSequence()
      .mapNotNull { it.endDate }
      .maxOrNull()
    if (lastBedspaceEndDate != null && bedspaces.all { it.endDate != null }) {
      archivePremisesAndSaveDomainEvent(premises, lastBedspaceEndDate, transactionId)
    }
  }

  private fun archivePremisesAndSaveDomainEvent(premises: Cas3PremisesEntity, endDate: LocalDate, transactionId: UUID): Cas3PremisesEntity {
    premises.endDate = endDate
    premises.status = Cas3PremisesStatus.archived
    val updatedPremises = cas3PremisesRepository.save(premises)
    cas3v2DomainEventService.savePremisesArchiveEvent(premises, endDate, transactionId)
    return updatedPremises
  }
}
