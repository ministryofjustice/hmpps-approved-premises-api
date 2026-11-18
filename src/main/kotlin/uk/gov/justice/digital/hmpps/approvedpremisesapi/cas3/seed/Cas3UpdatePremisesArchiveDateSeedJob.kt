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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3ArchiveService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_BEDSPACE_ARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_PREMISES_ARCHIVED
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

/**
 * This seed job can be used to modify the most recent archive date for a premises
 *
 * This date will propagate to any bedspaces archived at the same point in time
 *
 * The required archive date must be today or in the past.
 *
 * The same validation applied when archiving via the UI will be applied (e.g.
 * if there is a booking that ends after the archive date, this will fail)
 *
 * This seed job should be used as an option of last resort. If possible the user
 * should unarchive and rearchive the premises on the correct date. A limit to this
 * approach is that after unarchiving the user cannot re-archive to an earlier date
 * in time.
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
  requiredHeaders = setOf("premises_id", "current_archive_date", "required_archive_date"),
) {
  override fun deserializeRow(columns: Map<String, String>) = Row(
    premisesId = UUID.fromString(columns.getValue("premises_id").trim()),
    currentArchiveDate = LocalDate.parse(columns.getValue("current_archive_date").trim()),
    requiredArchiveDate = LocalDate.parse(columns.getValue("required_archive_date").trim()),
  )

  override fun processRow(row: Row) {
    val premises = premisesRepository.findByIdOrNull(row.premisesId)
      ?: throw SeedException("CAS3 premises ${row.premisesId} not found")

    validatePremisesIsArchived(premises)
    validateRequiredArchiveDateIsNotInFuture(row, premises)
    validateDates(row, "CAS3 premises ${premises.id}", premises.startDate, premises.endDate)
    validateCanArchivePremises(row)

    updatePremisesArchiveDate(row, premises)
    val transactionId = updatePremisesArchiveDomainEvent(row, premises)

    updateBedspaceArchiveDates(row, transactionId)
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
    premises.endDate = row.requiredArchiveDate
    premisesRepository.save(premises)
    logArchiveDateUpdate(row, "CAS3 premises", premises.id)
  }

  private fun updatePremisesArchiveDomainEvent(row: Row, premises: Cas3PremisesEntity): UUID {
    val premisesDomainEvent = domainEventRepository.findLastCas3PremisesActiveDomainEventByPremisesIdAndType(premises.id, CAS3_PREMISES_ARCHIVED)
      ?: throw SeedException("No active archive event found for CAS3 premises ${premises.id}")
    val premisesDomainEventPayload = readArchiveDomainEvent(premisesDomainEvent, CAS3PremisesArchiveEvent::class.java)
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
    transactionId: UUID,
  ) {
    domainEventRepository.findByCas3TransactionIdAndType(transactionId, CAS3_BEDSPACE_ARCHIVED)
      .filter { bedspaceDomainEvent -> bedspaceDomainEvent.cas3CancelledAt == null }
      .forEach { bedspaceDomainEvent ->
        val bedspace = findArchivedBedspace(row.premisesId, bedspaceDomainEvent)
        validateLatestBedspaceArchiveDomainEvent(bedspaceDomainEvent, bedspace.id)
        validateDates(row, "CAS3 bedspace ${bedspace.id}", bedspace.startDate, bedspace.endDate)

        bedspace.endDate = row.requiredArchiveDate
        bedspacesRepository.save(bedspace)
        logArchiveDateUpdate(row, "CAS3 bedspace", bedspace.id)

        updateBedspaceArchiveDomainEvent(row, bedspaceDomainEvent)
      }
  }

  private fun updateBedspaceArchiveDomainEvent(
    row: Row,
    bedspaceDomainEvent: DomainEventEntity,
  ) {
    val bedspaceDomainEventPayload = readArchiveDomainEvent(bedspaceDomainEvent, CAS3BedspaceArchiveEvent::class.java)
    validateDomainEventEndDateIsMostRecent(bedspaceDomainEvent, bedspaceDomainEventPayload.eventDetails.endDate, row.currentArchiveDate)
    val updatedBedspaceDomainEventPayload = bedspaceDomainEventPayload.copy(
      eventDetails = bedspaceDomainEventPayload.eventDetails.copy(endDate = row.requiredArchiveDate),
    )
    domainEventRepository.save(bedspaceDomainEvent.copy(data = jsonMapper.writeValueAsString(updatedBedspaceDomainEventPayload)))
    logArchiveDateUpdate(row, "CAS3 bedspace archive domain event", bedspaceDomainEvent.id)
  }

  private fun logArchiveDateUpdate(row: Row, entityType: String, entityId: UUID) {
    val message = "Updated archive date for $entityType $entityId from ${row.currentArchiveDate} to ${row.requiredArchiveDate}"
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

  private fun <T> readArchiveDomainEvent(domainEvent: DomainEventEntity, type: Class<T>): T = try {
    jsonMapper.readerFor(type)
      // Do not silently drop fields we cannot write back.
      .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .readValue(domainEvent.data)
  } catch (exception: JsonProcessingException) {
    throw SeedException("Archive event ${domainEvent.id} does not match the ${type.simpleName} payload", exception)
  }

  private fun validateDomainEventEndDateIsMostRecent(domainEvent: DomainEventEntity, endDate: LocalDate, expectedEndDate: LocalDate) {
    if (endDate != expectedEndDate) {
      throw SeedException("Most recent premises archive event ${domainEvent.id} does not have the expected end date $expectedEndDate")
    }
  }

  data class Row(
    val premisesId: UUID,
    val currentArchiveDate: LocalDate,
    val requiredArchiveDate: LocalDate,
  )
}
