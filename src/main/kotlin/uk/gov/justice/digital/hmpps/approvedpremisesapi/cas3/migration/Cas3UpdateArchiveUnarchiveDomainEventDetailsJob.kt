package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesUnarchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_BEDSPACE_ARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_BEDSPACE_UNARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_PREMISES_ARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_PREMISES_UNARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Component
class Cas3UpdateArchiveUnarchiveDomainEventDetailsJob(
  private val domainEventRepository: DomainEventRepository,
  private val migrationLogger: MigrationLogger,
  private val objectMapper: ObjectMapper,
) : MigrationJob() {
  override val shouldRunInTransaction = true

  @SuppressWarnings("TooGenericExceptionCaught", "NestedBlockDepth")
  override fun process(pageSize: Int) {
    val domainEvents = domainEventRepository.findCas3DomainEventsByTypes(
      listOf(
        CAS3_PREMISES_ARCHIVED,
        CAS3_PREMISES_UNARCHIVED,
        CAS3_BEDSPACE_ARCHIVED,
        CAS3_BEDSPACE_UNARCHIVED,
      ),
    )

    val domainEventIds = domainEvents.map { it.id }.toSet()

    try {
      migrationLogger.info("Updating CAS3 archive/unarchive domain event Ids ${domainEventIds.map { it }}")

      domainEvents.forEach { domainEvent ->
        when (domainEvent.type) {
          CAS3_PREMISES_ARCHIVED -> {
            try {
              val domainEventData = objectMapper.readValue(domainEvent.data, CAS3PremisesArchiveEventV1::class.java)

              val updatedDomainEvent = CAS3PremisesArchiveEvent(
                id = domainEventData.id,
                timestamp = domainEventData.timestamp,
                eventType = domainEventData.eventType,
                eventDetails = CAS3PremisesArchiveEventDetails(
                  premisesId = domainEventData.premisesId,
                  endDate = domainEventData.eventDetails.endDate,
                  userId = domainEventData.eventDetails.userId,
                  transactionId = domainEvent.cas3TransactionId,
                ),
              )
              domainEventRepository.updateData(
                domainEvent.id,
                objectMapper.writeValueAsString(updatedDomainEvent),
              )
            } catch (exception: Exception) {
              migrationLogger.error("Domain events with Id $domainEvent.id is not v1", exception)

              val domainEventData = objectMapper.readValue(domainEvent.data, CAS3PremisesArchiveEvent::class.java)

              val updatedDomainEvent = CAS3PremisesArchiveEvent(
                id = domainEventData.id,
                timestamp = domainEventData.timestamp,
                eventType = domainEventData.eventType,
                eventDetails = CAS3PremisesArchiveEventDetails(
                  premisesId = domainEventData.eventDetails.premisesId,
                  endDate = domainEventData.eventDetails.endDate,
                  userId = domainEventData.eventDetails.userId,
                  transactionId = domainEvent.cas3TransactionId,
                ),
              )
              domainEventRepository.updateData(
                domainEvent.id,
                objectMapper.writeValueAsString(updatedDomainEvent),
              )
            }
          }
          CAS3_PREMISES_UNARCHIVED -> {
            try {
              val domainEventData = objectMapper.readValue(domainEvent.data, CAS3PremisesUnarchiveEventV1::class.java)

              val updatedDomainEvent = CAS3PremisesUnarchiveEvent(
                id = domainEventData.id,
                timestamp = domainEventData.timestamp,
                eventType = domainEventData.eventType,
                eventDetails = CAS3PremisesUnarchiveEventDetails(
                  premisesId = domainEventData.premisesId,
                  currentStartDate = domainEventData.eventDetails.currentStartDate,
                  currentEndDate = domainEventData.eventDetails.currentEndDate,
                  newStartDate = domainEventData.eventDetails.newStartDate,
                  userId = domainEventData.eventDetails.userId,
                  transactionId = domainEvent.cas3TransactionId,
                ),
              )
              domainEventRepository.updateData(
                domainEvent.id,
                objectMapper.writeValueAsString(updatedDomainEvent),
              )
            } catch (exception: Exception) {
              migrationLogger.error("Domain events with Id $domainEvent.id is not v1", exception)

              val domainEventData = objectMapper.readValue(domainEvent.data, CAS3PremisesUnarchiveEvent::class.java)

              val updatedDomainEvent = CAS3PremisesUnarchiveEvent(
                id = domainEventData.id,
                timestamp = domainEventData.timestamp,
                eventType = domainEventData.eventType,
                eventDetails = CAS3PremisesUnarchiveEventDetails(
                  premisesId = domainEventData.eventDetails.premisesId,
                  currentStartDate = domainEventData.eventDetails.currentStartDate,
                  currentEndDate = domainEventData.eventDetails.currentEndDate,
                  newStartDate = domainEventData.eventDetails.newStartDate,
                  userId = domainEventData.eventDetails.userId,
                  transactionId = domainEvent.cas3TransactionId,
                ),
              )
              domainEventRepository.updateData(
                domainEvent.id,
                objectMapper.writeValueAsString(updatedDomainEvent),
              )
            }
          }
          CAS3_BEDSPACE_ARCHIVED -> {
            try {
              val domainEventData = objectMapper.readValue(domainEvent.data, CAS3BedspaceArchiveEventV1::class.java)
              val updatedDomainEvent = CAS3BedspaceArchiveEvent(
                id = domainEventData.id,
                timestamp = domainEventData.timestamp,
                eventType = domainEventData.eventType,
                eventDetails = CAS3BedspaceArchiveEventDetails(
                  bedspaceId = domainEventData.bedspaceId,
                  premisesId = domainEventData.premisesId,
                  endDate = domainEventData.eventDetails.endDate,
                  currentEndDate = domainEventData.eventDetails.currentEndDate,
                  userId = domainEventData.eventDetails.userId,
                  transactionId = domainEvent.cas3TransactionId,
                ),
              )
              domainEventRepository.updateData(
                domainEvent.id,
                objectMapper.writeValueAsString(updatedDomainEvent),
              )
            } catch (exception: Exception) {
              migrationLogger.error("Domain events with Id $domainEvent.id is not v1", exception)

              val domainEventData = objectMapper.readValue(domainEvent.data, CAS3BedspaceArchiveEvent::class.java)
              val updatedDomainEvent = CAS3BedspaceArchiveEvent(
                id = domainEventData.id,
                timestamp = domainEventData.timestamp,
                eventType = domainEventData.eventType,
                eventDetails = CAS3BedspaceArchiveEventDetails(
                  bedspaceId = domainEventData.eventDetails.bedspaceId,
                  premisesId = domainEventData.eventDetails.premisesId,
                  endDate = domainEventData.eventDetails.endDate,
                  currentEndDate = domainEventData.eventDetails.currentEndDate,
                  userId = domainEventData.eventDetails.userId,
                  transactionId = domainEvent.cas3TransactionId,
                ),
              )
              domainEventRepository.updateData(
                domainEvent.id,
                objectMapper.writeValueAsString(updatedDomainEvent),
              )
            }
          }
          CAS3_BEDSPACE_UNARCHIVED -> {
            try {
              val domainEventData = objectMapper.readValue(domainEvent.data, CAS3BedspaceUnarchiveEventV1::class.java)
              val updatedDomainEvent = CAS3BedspaceUnarchiveEvent(
                id = domainEventData.id,
                timestamp = domainEventData.timestamp,
                eventType = domainEventData.eventType,
                eventDetails = CAS3BedspaceUnarchiveEventDetails(
                  bedspaceId = domainEventData.bedspaceId,
                  premisesId = domainEventData.premisesId,
                  currentStartDate = domainEventData.eventDetails.currentStartDate,
                  currentEndDate = domainEventData.eventDetails.currentEndDate,
                  newStartDate = domainEventData.eventDetails.newStartDate,
                  userId = domainEventData.eventDetails.userId,
                  transactionId = domainEvent.cas3TransactionId,
                ),
              )
              domainEventRepository.updateData(
                domainEvent.id,
                objectMapper.writeValueAsString(updatedDomainEvent),
              )
            } catch (exception: Exception) {
              migrationLogger.error("Domain events with Id $domainEvent.id is not v1", exception)
              val domainEventData = objectMapper.readValue(domainEvent.data, CAS3BedspaceUnarchiveEvent::class.java)

              val updatedDomainEvent = CAS3BedspaceUnarchiveEvent(
                id = domainEventData.id,
                timestamp = domainEventData.timestamp,
                eventType = domainEventData.eventType,
                eventDetails = CAS3BedspaceUnarchiveEventDetails(
                  bedspaceId = domainEventData.eventDetails.bedspaceId,
                  premisesId = domainEventData.eventDetails.premisesId,
                  currentStartDate = domainEventData.eventDetails.currentStartDate,
                  currentEndDate = domainEventData.eventDetails.currentEndDate,
                  newStartDate = domainEventData.eventDetails.newStartDate,
                  userId = domainEventData.eventDetails.userId,
                  transactionId = domainEvent.cas3TransactionId,
                ),
              )
              domainEventRepository.updateData(
                domainEvent.id,
                objectMapper.writeValueAsString(updatedDomainEvent),
              )
            }
          }
          else -> null
        }
      }
      migrationLogger.info("Updating CAS3 archive/unarchive domain events with Ids ${domainEventIds.map { it }} is completed")
    } catch (exception: Exception) {
      migrationLogger.error("Unable to update domain events with Ids ${domainEventIds.joinToString()}", exception)
    }
  }
}

class CAS3PremisesArchiveEventV1(
  val eventDetails: CAS3PremisesArchiveEventDetailsV1,
  override val id: UUID,
  override val timestamp: Instant,
  override val eventType: EventType,
  val premisesId: UUID,
  val transactionId: UUID?,
) : CAS3Event

data class CAS3PremisesArchiveEventDetailsV1(
  val userId: UUID,
  val endDate: LocalDate,
)

data class CAS3PremisesUnarchiveEventV1(
  val eventDetails: CAS3PremisesUnarchiveEventDetailsV1,
  override val id: UUID,
  override val timestamp: Instant,
  override val eventType: EventType,
  val premisesId: UUID,
  val transactionId: UUID,
) : CAS3Event

data class CAS3PremisesUnarchiveEventDetailsV1(
  val userId: UUID,
  val currentStartDate: LocalDate,
  val newStartDate: LocalDate,
  val currentEndDate: LocalDate?,
)

data class CAS3BedspaceArchiveEventV1(
  val eventDetails: CAS3BedspaceArchiveEventDetailsV1,
  override val id: UUID,
  override val timestamp: Instant,
  override val eventType: EventType,
  val bedspaceId: UUID,
  val premisesId: UUID,
  val transactionId: UUID,
) : CAS3Event

data class CAS3BedspaceArchiveEventDetailsV1(
  val endDate: LocalDate,
  val currentEndDate: LocalDate?,
  val userId: UUID,
)

data class CAS3BedspaceUnarchiveEventV1(
  val eventDetails: CAS3BedspaceUnarchiveEventDetailsV1,
  override val id: UUID,
  override val timestamp: Instant,
  override val eventType: EventType,
  val bedspaceId: UUID,
  val premisesId: UUID,
  val transactionId: UUID,
) : CAS3Event

data class CAS3BedspaceUnarchiveEventDetailsV1(
  val userId: UUID,
  val currentStartDate: LocalDate,
  val currentEndDate: LocalDate,
  val newStartDate: LocalDate,
)
