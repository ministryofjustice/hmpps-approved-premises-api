package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_BEDSPACE_ARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_BEDSPACE_UNARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_PREMISES_ARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_PREMISES_UNARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.time.OffsetDateTime
import java.util.UUID

@Component
class Cas3UpdateArchiveUnarchiveDomainEventTransactionJob(
  private val domainEventRepository: DomainEventRepository,
  private val migrationLogger: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = true

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught")
  override fun process(pageSize: Int) {
    val domainEvents = domainEventRepository.findCas3DomainEventsByTypeWithoutTransactionId(
      listOf(
        CAS3_PREMISES_ARCHIVED,
        CAS3_PREMISES_UNARCHIVED,
        CAS3_BEDSPACE_ARCHIVED,
        CAS3_BEDSPACE_UNARCHIVED,
      ),
    )

    val premisesIds = domainEvents.map { it.id }.toList()

    try {
      migrationLogger.info("Updating CAS3 archive/unarchive domain events with premises Ids ${premisesIds.map { it }}")
      var lastDomainEventPremisesId: UUID? = null
      var lastDomainEventCreatedAt: OffsetDateTime? = null
      var currentTransactionId: UUID? = null

      domainEvents.forEach { domainEvent ->
        val isNewPremises = lastDomainEventPremisesId != domainEvent.cas3PremisesId
        val isSameTransaction = (
          lastDomainEventCreatedAt != null &&
            domainEvent.createdAt <= lastDomainEventCreatedAt.plusMinutes(3) &&
            lastDomainEventPremisesId == domainEvent.cas3PremisesId
          )

        if (isNewPremises || !isSameTransaction) {
          currentTransactionId = UUID.randomUUID()
        }

        val updatedDomainEvent = domainEvent.copy(cas3TransactionId = currentTransactionId)
        domainEventRepository.save(updatedDomainEvent)

        lastDomainEventPremisesId = domainEvent.cas3PremisesId
        lastDomainEventCreatedAt = domainEvent.createdAt
      }

      migrationLogger.info("Updating CAS3 archive/unarchive domain events with premises Ids ${premisesIds.map { it }} is completed")
    } catch (exception: Exception) {
      migrationLogger.error("Unable to update domain events with premises Ids ${premisesIds.joinToString()}", exception)
    }
  }
}
