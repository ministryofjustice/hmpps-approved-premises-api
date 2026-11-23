package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.seed

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3ArchiveService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_BEDSPACE_ARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_BEDSPACE_UNARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_PREMISES_ARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_PREMISES_UNARCHIVED
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

/**
 * This seed job can be used to modify the most recent archive date for a premises
 *
 * Because a premises start date is also modified when unarchiving (set to the
 * current date), the seed job also allows this date to be modified too
 *
 * These date changes will propagate to bedspaces
 *
 * The same validation applied when archiving via the UI will be applied (e.g.
 * if there is a booking that ends after the archive date, this will fail)
 *
 * This seed job should be used as an option of last resort and with care, reviewing
 * the changes made. Note that this can leave some domain event fields in an
 * inconsistent state (e.g. unarchive event currentEndDate)
 *
 * If possible the user should unarchive and rearchive the premises on the correct date.
 * A limit to this approach is that after unarchiving the user cannot re-archive to an earlier
 * date in time because unarchiving changes the start date to today's date
 *
 * If this seed job is used regularly, consider revising the UI/API logic to NOT
 * set the start date to today's date on unarchive and leave it as-is OR if that's
 * not possible, set it to the original start date (found via domain events).
 *
 * Users would then be able to 'self-service' fixing archive dates, making use of
 * Cas3UpdatePremisesSuppressArchiveLimitsSeedJob when necessary.
 *
 * This is assuming there is a reason that it is set to the current date on unarchive.
 *
 * For more on this, see the documentation on Cas3ArchiveService.unarchivePremises
 */
@Component
class Cas3UpdatePremisesArchiveDateSeedJob(
  private val premisesRepository: Cas3PremisesRepository,
  private val bedspacesRepository: Cas3BedspacesRepository,
  private val domainEventRepository: DomainEventRepository,
  private val jsonMapper: JsonMapper,
  private val archiveService: Cas3ArchiveService,
  private val seedLogger: SeedLogger,
  private val clock: Clock,
) : SeedJob<Cas3UpdatePremisesArchiveDateSeedJob.Row>(
  requiredHeaders = setOf("premises_id", "current_archive_date", "required_archive_date", "required_start_date"),
) {
  override fun deserializeRow(columns: Map<String, String>) = Row(
    premisesId = UUID.fromString(columns.getValue("premises_id").trim()),
    currentArchiveDate = LocalDate.parse(columns.getValue("current_archive_date").trim()),
    requiredArchiveDate = LocalDate.parse(columns.getValue("required_archive_date").trim()),
    requiredStartDate = LocalDate.parse(columns.getValue("required_start_date").trim()),
  )

  override fun processRow(row: Row) {
    val premises = premisesRepository.findByIdOrNull(row.premisesId)
      ?: throw SeedException("CAS3 premises ${row.premisesId} not found")

    validatePremisesIsArchived(premises)
    validateRequiredArchiveDateIsNotInFuture(row, premises)
    validateDates(row, "CAS3 premises ${premises.id}", row.requiredStartDate, premises.endDate)
    validateCanArchivePremises(row)

    val premisesUnarchiveTransactionId = updatePremisesUnarchiveDomainEvent(row, premises)
    updatePremisesArchiveDate(row, premises)
    val transactionId = updatePremisesArchiveDomainEvent(row, premises)

    updateBedspaceArchiveDates(row, transactionId, premisesUnarchiveTransactionId)
  }

  private fun validatePremisesIsArchived(premises: Cas3PremisesEntity) {
    if (!premises.isPremisesArchived()) {
      throw SeedException("CAS3 premises ${premises.id} is not currently archived")
    }
    if (premises.status != Cas3PremisesStatus.archived) {
      throw SeedException("CAS3 premises ${premises.id} has status ${premises.status}, expected ${Cas3PremisesStatus.archived}")
    }
  }

  private fun validateRequiredArchiveDateIsNotInFuture(row: Row, premises: Cas3PremisesEntity) {
    if (row.requiredArchiveDate.isAfter(LocalDate.now(clock))) {
      throw SeedException("CAS3 premises ${premises.id} cannot have an archive date in the future (${row.requiredArchiveDate})")
    }
  }

  private fun validateDates(row: Row, record: String, startDate: LocalDate, endDate: LocalDate?) {
    if (endDate != row.currentArchiveDate) {
      throw SeedException("$record has end date $endDate, expected ${row.currentArchiveDate}")
    }
    if (row.requiredArchiveDate < startDate) {
      throw SeedException("$record cannot have an archive date before its start date $startDate")
    }
  }

  private fun validateCanArchivePremises(row: Row) {
    val validationError = archiveService.canArchivePremisesBedspaces(row.premisesId, row.requiredArchiveDate) ?: return
    val conflicts = validationError.validationMessages.values.joinToString { "${it.message} (bedspace ${it.entityId}, earliest archive date ${it.value})" }
    throw SeedException("CAS3 premises ${row.premisesId} cannot be archived on ${row.requiredArchiveDate}: $conflicts")
  }

  private fun updatePremisesArchiveDate(row: Row, premises: Cas3PremisesEntity) {
    val currentStartDate = premises.startDate
    premises.startDate = row.requiredStartDate
    premises.endDate = row.requiredArchiveDate
    premisesRepository.save(premises)
    if (currentStartDate != row.requiredStartDate) {
      logDateUpdate("CAS3 premises", premises.id, "start date", currentStartDate, row.requiredStartDate)
    }
    logArchiveDateUpdate(row, "CAS3 premises", premises.id)
  }

  private fun updatePremisesUnarchiveDomainEvent(row: Row, premises: Cas3PremisesEntity): UUID? {
    if (premises.startDate == row.requiredStartDate) return null

    val premisesUnarchiveDomainEvent = domainEventRepository.findLastCas3PremisesActiveDomainEventByPremisesIdAndType(
      premises.id,
      CAS3_PREMISES_UNARCHIVED,
    ) ?: return null
    val premisesUnarchiveEventPayload = readDomainEventPayload(premisesUnarchiveDomainEvent, CAS3PremisesUnarchiveEvent::class.java)
    val currentStartDate = premises.startDate
    validateDomainEventStartDateIsMostRecent(premisesUnarchiveDomainEvent, premisesUnarchiveEventPayload.eventDetails.newStartDate, currentStartDate)

    val updatedPremisesUnarchiveEventPayload = premisesUnarchiveEventPayload.copy(
      eventDetails = premisesUnarchiveEventPayload.eventDetails.copy(newStartDate = row.requiredStartDate),
    )
    domainEventRepository.save(premisesUnarchiveDomainEvent.copy(data = jsonMapper.writeValueAsString(updatedPremisesUnarchiveEventPayload)))
    logDateUpdate("CAS3 premises unarchive domain event", premisesUnarchiveDomainEvent.id, "start date", currentStartDate, row.requiredStartDate)

    return premisesUnarchiveDomainEvent.cas3TransactionId
      ?: throw SeedException("Unarchive event ${premisesUnarchiveDomainEvent.id} has no transaction ID")
  }

  private fun updatePremisesArchiveDomainEvent(row: Row, premises: Cas3PremisesEntity): UUID {
    val premisesDomainEvent = domainEventRepository.findLastCas3PremisesActiveDomainEventByPremisesIdAndType(premises.id, CAS3_PREMISES_ARCHIVED)
      ?: throw SeedException("No active archive event found for CAS3 premises ${premises.id}")
    val premisesDomainEventPayload = readDomainEventPayload(premisesDomainEvent, CAS3PremisesArchiveEvent::class.java)
    validateDomainEventEndDateIsMostRecent(premisesDomainEvent, premisesDomainEventPayload.eventDetails.endDate, row.currentArchiveDate)

    val updatedPremisesDomainEventPayload = premisesDomainEventPayload.copy(
      eventDetails = premisesDomainEventPayload.eventDetails.copy(endDate = row.requiredArchiveDate),
    )
    domainEventRepository.save(premisesDomainEvent.copy(data = jsonMapper.writeValueAsString(updatedPremisesDomainEventPayload)))
    logArchiveDateUpdate(row, "CAS3 premises archive domain event", premisesDomainEvent.id)

    return premisesDomainEvent.cas3TransactionId
      ?: throw SeedException("Archive event ${premisesDomainEvent.id} has no transaction ID")
  }

  private fun updateBedspaceArchiveDates(
    row: Row,
    premisesArchiveTransactionId: UUID,
    premisesUnarchiveTransactionId: UUID?,
  ) {
    val bedspaceUnarchiveEvents = premisesUnarchiveTransactionId?.let {
      domainEventRepository.findByCas3TransactionIdAndType(it, CAS3_BEDSPACE_UNARCHIVED)
        .filter { event -> event.cas3CancelledAt == null }
    }.orEmpty()

    domainEventRepository.findByCas3TransactionIdAndType(premisesArchiveTransactionId, CAS3_BEDSPACE_ARCHIVED)
      .filter { bedspaceDomainEvent -> bedspaceDomainEvent.cas3CancelledAt == null }
      .forEach { bedspaceDomainEvent ->
        val bedspace = findArchivedBedspace(row.premisesId, bedspaceDomainEvent)
        validateLatestBedspaceArchiveDomainEvent(bedspaceDomainEvent, bedspace.id)
        val matchingUnarchiveEvents = bedspaceUnarchiveEvents.filter { it.cas3BedspaceId == bedspace.id }
        if (matchingUnarchiveEvents.size > 1) {
          throw SeedException("Multiple matching active unarchive events found for CAS3 bedspace ${bedspace.id}")
        }
        val bedspaceUnarchiveDomainEvent = matchingUnarchiveEvents.singleOrNull()
        val isBedspaceStartDateBeingCorrected = bedspaceUnarchiveDomainEvent != null
        val correctedStartDate = if (isBedspaceStartDateBeingCorrected) row.requiredStartDate else bedspace.startDate
        validateDates(row, "CAS3 bedspace ${bedspace.id}", correctedStartDate, bedspace.endDate)

        if (bedspaceUnarchiveDomainEvent != null) {
          updateBedspaceUnarchiveDomainEvent(row, bedspace, bedspaceUnarchiveDomainEvent)
        }

        bedspacesRepository.save(
          bedspace.copy(
            startDate = correctedStartDate,
            endDate = row.requiredArchiveDate,
          ),
        )
        if (isBedspaceStartDateBeingCorrected) {
          logDateUpdate("CAS3 bedspace", bedspace.id, "start date", bedspace.startDate, row.requiredStartDate)
        }
        logArchiveDateUpdate(row, "CAS3 bedspace", bedspace.id)

        updateBedspaceArchiveDomainEvent(row, bedspaceDomainEvent)
      }
  }

  private fun updateBedspaceUnarchiveDomainEvent(
    row: Row,
    bedspace: Cas3BedspacesEntity,
    bedspaceUnarchiveDomainEvent: DomainEventEntity,
  ) {
    val bedspaceUnarchiveEventPayload = readDomainEventPayload(bedspaceUnarchiveDomainEvent, CAS3BedspaceUnarchiveEvent::class.java)

    val latestUnarchiveDomainEvent = domainEventRepository.findLastCas3BedspaceActiveDomainEventByBedspaceIdAndType(
      bedspace.id,
      CAS3_BEDSPACE_UNARCHIVED,
    )
    if (latestUnarchiveDomainEvent?.id != bedspaceUnarchiveDomainEvent.id) {
      throw SeedException("Unarchive event ${bedspaceUnarchiveDomainEvent.id} is no longer the latest active unarchive for CAS3 bedspace ${bedspace.id}")
    }

    validateDomainEventStartDateIsMostRecent(bedspaceUnarchiveDomainEvent, bedspaceUnarchiveEventPayload.eventDetails.newStartDate, bedspace.startDate)

    val updatedBedspaceUnarchiveEventPayload = bedspaceUnarchiveEventPayload.copy(
      eventDetails = bedspaceUnarchiveEventPayload.eventDetails.copy(newStartDate = row.requiredStartDate),
    )
    domainEventRepository.save(bedspaceUnarchiveDomainEvent.copy(data = jsonMapper.writeValueAsString(updatedBedspaceUnarchiveEventPayload)))
    logDateUpdate("CAS3 bedspace unarchive domain event", bedspaceUnarchiveDomainEvent.id, "start date", bedspace.startDate, row.requiredStartDate)
  }

  private fun updateBedspaceArchiveDomainEvent(
    row: Row,
    bedspaceDomainEvent: DomainEventEntity,
  ) {
    val bedspaceDomainEventPayload = readDomainEventPayload(bedspaceDomainEvent, CAS3BedspaceArchiveEvent::class.java)
    validateDomainEventEndDateIsMostRecent(bedspaceDomainEvent, bedspaceDomainEventPayload.eventDetails.endDate, row.currentArchiveDate)
    val updatedBedspaceDomainEventPayload = bedspaceDomainEventPayload.copy(
      eventDetails = bedspaceDomainEventPayload.eventDetails.copy(endDate = row.requiredArchiveDate),
    )
    domainEventRepository.save(bedspaceDomainEvent.copy(data = jsonMapper.writeValueAsString(updatedBedspaceDomainEventPayload)))
    logArchiveDateUpdate(row, "CAS3 bedspace archive domain event", bedspaceDomainEvent.id)
  }

  private fun logArchiveDateUpdate(row: Row, entityType: String, entityId: UUID) {
    val message = "Updated archive date for $entityType $entityId from ${row.currentArchiveDate} to ${row.requiredArchiveDate}"
    logAfterCommit(message)
  }

  private fun logDateUpdate(entityType: String, entityId: UUID, dateType: String, currentDate: LocalDate, requiredDate: LocalDate) {
    val message = "Updated $dateType for $entityType $entityId from $currentDate to $requiredDate"
    logAfterCommit(message)
  }

  private fun logAfterCommit(message: String) {
    TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
      override fun afterCommit() {
        seedLogger.info(message)
      }
    })
  }

  private fun findArchivedBedspace(premisesId: UUID, domainEvent: DomainEventEntity): Cas3BedspacesEntity {
    val bedspaceId = domainEvent.cas3BedspaceId
      ?: throw SeedException("Archive event ${domainEvent.id} has no bedspace ID")
    return bedspacesRepository.findCas3Bedspace(premisesId, bedspaceId)
      ?: throw SeedException("CAS3 bedspace $bedspaceId not found for premises $premisesId")
  }

  private fun validateLatestBedspaceArchiveDomainEvent(domainEvent: DomainEventEntity, bedspaceId: UUID) {
    val latestDomainEvent = domainEventRepository.findLastCas3BedspaceActiveDomainEventByBedspaceIdAndType(bedspaceId, CAS3_BEDSPACE_ARCHIVED)
    if (latestDomainEvent?.id != domainEvent.id) {
      throw SeedException("Archive event ${domainEvent.id} is no longer the latest active archive for CAS3 bedspace $bedspaceId")
    }
  }

  private fun <T> readDomainEventPayload(domainEvent: DomainEventEntity, type: Class<T>): T = try {
    jsonMapper.readerFor(type)
      // Do not silently drop fields we cannot write back.
      .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .readValue(domainEvent.data)
  } catch (exception: JsonProcessingException) {
    throw SeedException("Domain event ${domainEvent.id} does not match the ${type.simpleName} payload", exception)
  }

  private fun validateDomainEventEndDateIsMostRecent(domainEvent: DomainEventEntity, endDate: LocalDate, expectedEndDate: LocalDate) {
    if (endDate != expectedEndDate) {
      throw SeedException("Most recent premises archive event ${domainEvent.id} does not have the expected end date $expectedEndDate")
    }
  }

  private fun validateDomainEventStartDateIsMostRecent(domainEvent: DomainEventEntity, startDate: LocalDate, expectedStartDate: LocalDate) {
    if (startDate != expectedStartDate) {
      throw SeedException("Most recent unarchive event ${domainEvent.id} does not have the expected start date $expectedStartDate")
    }
  }

  data class Row(
    val premisesId: UUID,
    val currentArchiveDate: LocalDate,
    val requiredArchiveDate: LocalDate,
    val requiredStartDate: LocalDate,
  )
}
