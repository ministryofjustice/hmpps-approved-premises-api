package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.CAS3BedspaceArchiveEventDetailsV1
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.CAS3BedspaceArchiveEventV1
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.CAS3BedspaceUnarchiveEventDetailsV1
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.CAS3BedspaceUnarchiveEventV1
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.CAS3PremisesArchiveEventDetailsV1
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.CAS3PremisesArchiveEventV1
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.CAS3PremisesUnarchiveEventDetailsV1
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.CAS3PremisesUnarchiveEventV1
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesUnarchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3UpdateArchiveUnarchiveDomainEventDetailsJobTest : MigrationJobTestBase() {
  @Test
  fun `all v1 archive and unarchive domain events are updated with new event details class`() {
    val probationRegion = givenAProbationRegion()

    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    val premisesOneId = UUID.randomUUID()
    val premisesTwoId = UUID.randomUUID()
    val premisesThreeId = UUID.randomUUID()
    val premisesFourId = UUID.randomUUID()

    val premisesOneArchiveDomainEventData = createPremisesArchiveEventData(premisesOneId, user.id, LocalDate.now().minusDays(15), UUID.randomUUID())
    val premisesOneArchiveDomainEvent = createPremisesArchiveDomainEvent(premisesOneId, objectMapper.writeValueAsString(premisesOneArchiveDomainEventData))

    val premisesOneUnarchiveDomainEventData = createPremisesUnarchiveEventData(
      premisesOneId,
      user.id,
      LocalDate.now().minusDays(90),
      LocalDate.now().minusDays(2),
      LocalDate.now().minusDays(15),
      UUID.randomUUID(),
    )
    val premisesOneUnarchiveDomainEvent = createPremisesUnarchiveDomainEvent(premisesOneId, objectMapper.writeValueAsString(premisesOneUnarchiveDomainEventData))

    val premisesTwoArchiveDomainEventData = createPremisesArchiveEventDataV1(premisesTwoId, user.id, LocalDate.now().minusDays(10), UUID.randomUUID())
    val premisesTwoArchiveDomainEvent = createPremisesArchiveDomainEvent(premisesTwoId, objectMapper.writeValueAsString(premisesTwoArchiveDomainEventData))

    val premisesThreeArchiveTransactionId = UUID.randomUUID()
    val premisesThreeArchiveDomainEventData = createPremisesArchiveEventData(premisesThreeId, user.id, LocalDate.now().minusDays(7), premisesThreeArchiveTransactionId)
    createPremisesArchiveDomainEvent(premisesThreeId, objectMapper.writeValueAsString(premisesThreeArchiveDomainEventData), OffsetDateTime.now().minusDays(3))

    val premisesThreeUnarchiveDomainEventData = createPremisesUnarchiveEventDataV1(
      premisesThreeId,
      user.id,
      LocalDate.now().minusDays(90),
      LocalDate.now().plusDays(2),
      LocalDate.now().minusDays(7),
      UUID.randomUUID(),
    )
    val premisesThreeUnarchiveDomainEvent = createPremisesUnarchiveDomainEvent(premisesThreeId, objectMapper.writeValueAsString(premisesThreeUnarchiveDomainEventData))

    val bedspaceOnePremisesThreeId = UUID.randomUUID()
    val bedspaceOnePremisesThreeArchiveDomainEventData = createBedspaceArchiveEventData(
      bedspaceOnePremisesThreeId,
      premisesThreeId,
      user.id,
      null,
      LocalDate.now().minusDays(7),
      premisesThreeArchiveTransactionId,
    )
    val bedspaceOnePremisesThreeArchiveDomainEvent = createBedspaceArchiveDomainEvent(
      bedspaceOnePremisesThreeId,
      premisesThreeId,
      objectMapper.writeValueAsString(bedspaceOnePremisesThreeArchiveDomainEventData),
      null,
    )

    val bedspaceOnePremisesThreeUnarchiveDomainEventData = createBedspaceUnarchiveEventData(
      bedspaceOnePremisesThreeId,
      premisesThreeId,
      LocalDate.now().minusDays(100),
      LocalDate.now().minusDays(7),
      LocalDate.now().minusDays(2),
      user.id,
      null,
    )
    val bedspaceOnePremisesThreeUnarchiveDomainEvent = createBedspaceUnarchiveDomainEvent(
      bedspaceOnePremisesThreeId,
      premisesThreeId,
      objectMapper.writeValueAsString(bedspaceOnePremisesThreeUnarchiveDomainEventData),
    )

    val bedspaceTwoPremisesThreeId = UUID.randomUUID()
    val bedspaceTwoPremisesThreeArchiveDomainEventData = createBedspaceArchiveEventData(
      bedspaceTwoPremisesThreeId,
      premisesThreeId,
      user.id,
      null,
      LocalDate.now().minusDays(7),
      premisesThreeArchiveTransactionId,
    )
    val bedspaceTwoPremisesThreeArchiveDomainEvent = createBedspaceArchiveDomainEvent(
      bedspaceTwoPremisesThreeId,
      premisesThreeId,
      objectMapper.writeValueAsString(bedspaceTwoPremisesThreeArchiveDomainEventData),
    )

    val bedspaceOnePremisesFourId = UUID.randomUUID()
    val bedspaceOnePremisesFourArchiveDomainEventData = createBedspaceArchiveEventDataV1(
      bedspaceOnePremisesFourId,
      premisesFourId,
      user.id,
      null,
      LocalDate.now().plusDays(13),
    )
    val bedspaceOnePremisesFourArchiveDomainEvent = createBedspaceArchiveDomainEvent(
      bedspaceOnePremisesFourId,
      premisesFourId,
      objectMapper.writeValueAsString(bedspaceOnePremisesFourArchiveDomainEventData),
    )

    val bedspaceOnePremisesFourUnarchiveDomainEventData = createBedspaceUnarchiveEventDataV1(
      bedspaceOnePremisesFourId,
      premisesFourId,
      LocalDate.now().minusDays(120),
      LocalDate.now().minusDays(7),
      LocalDate.now().minusDays(15),
      user.id,
      UUID.randomUUID(),
    )
    val bedspaceOnePremisesFourUnarchiveDomainEvent = createBedspaceUnarchiveDomainEvent(
      bedspaceOnePremisesFourId,
      premisesFourId,
      objectMapper.writeValueAsString(bedspaceOnePremisesFourUnarchiveDomainEventData),
    )

    val premisesFourArchiveDomainEventData = createPremisesArchiveEventData(premisesFourId, user.id, LocalDate.now().plusDays(7), null)
    val premisesFourArchiveDomainEvent = createPremisesArchiveDomainEvent(
      premisesFourId,
      objectMapper.writeValueAsString(premisesFourArchiveDomainEventData),
    )

    val premisesFourUnarchiveDomainEventData = createPremisesUnarchiveEventData(
      premisesFourId,
      user.id,
      LocalDate.now().minusDays(120),
      LocalDate.now().minusDays(7),
      LocalDate.now().minusDays(15),
      null,
    )
    val premisesFourUnarchiveDomainEvent = createPremisesUnarchiveDomainEvent(premisesFourId, objectMapper.writeValueAsString(premisesFourUnarchiveDomainEventData))

    migrationJobService.runMigrationJob(MigrationJobType.updateCas3ArchiveUnarchiveDomainEventDetails, 10)

    val updatedDomainEvents = domainEventRepository.findByTypes(
      listOf(
        DomainEventType.CAS3_BEDSPACE_ARCHIVED,
        DomainEventType.CAS3_BEDSPACE_UNARCHIVED,
        DomainEventType.CAS3_PREMISES_ARCHIVED,
        DomainEventType.CAS3_PREMISES_UNARCHIVED,
      ),
    )

    assertPremisesArchiveDomainEvent(updatedDomainEvents, premisesOneArchiveDomainEvent)
    assertPremisesArchiveDomainEvent(updatedDomainEvents, premisesTwoArchiveDomainEvent)
    assertPremisesUnarchiveDomainEvent(updatedDomainEvents, premisesOneUnarchiveDomainEvent)
    assertPremisesArchiveDomainEvent(updatedDomainEvents, premisesFourArchiveDomainEvent)

    assertPremisesUnarchiveDomainEvent(updatedDomainEvents, premisesThreeUnarchiveDomainEvent)
    assertPremisesUnarchiveDomainEvent(updatedDomainEvents, premisesFourUnarchiveDomainEvent)

    assertBedspaceArchiveDomainEvent(updatedDomainEvents, bedspaceOnePremisesThreeArchiveDomainEvent)
    assertBedspaceArchiveDomainEvent(updatedDomainEvents, bedspaceTwoPremisesThreeArchiveDomainEvent)
    assertBedspaceArchiveDomainEvent(updatedDomainEvents, bedspaceOnePremisesFourArchiveDomainEvent)

    assertBedspaceUnarchiveDomainEvent(updatedDomainEvents, bedspaceOnePremisesThreeUnarchiveDomainEvent)
    assertBedspaceUnarchiveDomainEvent(updatedDomainEvents, bedspaceOnePremisesFourUnarchiveDomainEvent)
  }

  private fun assertPremisesArchiveDomainEvent(updatedDomainEvents: List<DomainEventEntity>, originalDomainEvent: DomainEventEntity) {
    val updatedDomainEvent = updatedDomainEvents.firstOrNull { it.id == originalDomainEvent.id }
    assertThat(updatedDomainEvent).isNotNull()
    val updatedDomainEventData = objectMapper.readValue(updatedDomainEvent?.data, CAS3PremisesArchiveEvent::class.java)
    assertThat(updatedDomainEvent?.id).isEqualTo(originalDomainEvent.id)
    assertThat(updatedDomainEvent?.type).isEqualTo(originalDomainEvent.type)
    assertThat(updatedDomainEvent?.cas3PremisesId).isEqualTo(originalDomainEvent.cas3PremisesId)
    assertThat(updatedDomainEvent?.cas3CancelledAt).isEqualTo(originalDomainEvent.cas3CancelledAt)
    assertThat(updatedDomainEventData.eventDetails.premisesId).isEqualTo(originalDomainEvent.cas3PremisesId)
    assertThat(updatedDomainEventData.eventDetails.transactionId).isEqualTo(originalDomainEvent.cas3TransactionId)
  }

  private fun assertPremisesUnarchiveDomainEvent(updatedDomainEvents: List<DomainEventEntity>, originalDomainEvent: DomainEventEntity) {
    val updatedDomainEvent = updatedDomainEvents.firstOrNull { it.id == originalDomainEvent.id }
    assertThat(updatedDomainEvent).isNotNull()
    val updatedDomainEventData = objectMapper.readValue(updatedDomainEvent?.data, CAS3PremisesUnarchiveEvent::class.java)
    assertThat(updatedDomainEvent?.id).isEqualTo(originalDomainEvent.id)
    assertThat(updatedDomainEvent?.type).isEqualTo(originalDomainEvent.type)
    assertThat(updatedDomainEvent?.cas3PremisesId).isEqualTo(originalDomainEvent.cas3PremisesId)
    assertThat(updatedDomainEvent?.cas3CancelledAt).isEqualTo(originalDomainEvent.cas3CancelledAt)
    assertThat(updatedDomainEventData.eventDetails.premisesId).isEqualTo(originalDomainEvent.cas3PremisesId)
    assertThat(updatedDomainEventData.eventDetails.transactionId).isEqualTo(originalDomainEvent.cas3TransactionId)
  }

  private fun assertBedspaceArchiveDomainEvent(updatedDomainEvents: List<DomainEventEntity>, originalDomainEvent: DomainEventEntity) {
    val updatedDomainEvent = updatedDomainEvents.firstOrNull { it.id == originalDomainEvent.id }
    assertThat(updatedDomainEvent).isNotNull()
    val updatedDomainEventData = objectMapper.readValue(updatedDomainEvent?.data, CAS3BedspaceArchiveEvent::class.java)
    assertThat(updatedDomainEvent?.id).isEqualTo(originalDomainEvent.id)
    assertThat(updatedDomainEvent?.type).isEqualTo(originalDomainEvent.type)
    assertThat(updatedDomainEvent?.cas3PremisesId).isEqualTo(originalDomainEvent.cas3PremisesId)
    assertThat(updatedDomainEvent?.cas3BedspaceId).isEqualTo(originalDomainEvent.cas3BedspaceId)
    assertThat(updatedDomainEvent?.cas3CancelledAt).isEqualTo(originalDomainEvent.cas3CancelledAt)
    assertThat(updatedDomainEventData.eventDetails.premisesId).isEqualTo(originalDomainEvent.cas3PremisesId)
    assertThat(updatedDomainEventData.eventDetails.bedspaceId).isEqualTo(originalDomainEvent.cas3BedspaceId)
    assertThat(updatedDomainEventData.eventDetails.transactionId).isEqualTo(originalDomainEvent.cas3TransactionId)
  }

  private fun assertBedspaceUnarchiveDomainEvent(updatedDomainEvents: List<DomainEventEntity>, originalDomainEvent: DomainEventEntity) {
    val updatedDomainEvent = updatedDomainEvents.firstOrNull { it.id == originalDomainEvent.id }
    assertThat(updatedDomainEvent).isNotNull()
    val updatedDomainEventData = objectMapper.readValue(updatedDomainEvent?.data, CAS3BedspaceUnarchiveEvent::class.java)
    assertThat(updatedDomainEvent?.id).isEqualTo(originalDomainEvent.id)
    assertThat(updatedDomainEvent?.type).isEqualTo(originalDomainEvent.type)
    assertThat(updatedDomainEvent?.cas3PremisesId).isEqualTo(originalDomainEvent.cas3PremisesId)
    assertThat(updatedDomainEvent?.cas3BedspaceId).isEqualTo(originalDomainEvent.cas3BedspaceId)
    assertThat(updatedDomainEvent?.cas3CancelledAt).isEqualTo(originalDomainEvent.cas3CancelledAt)
    assertThat(updatedDomainEventData.eventDetails.premisesId).isEqualTo(originalDomainEvent.cas3PremisesId)
    assertThat(updatedDomainEventData.eventDetails.bedspaceId).isEqualTo(originalDomainEvent.cas3BedspaceId)
    assertThat(updatedDomainEventData.eventDetails.transactionId).isEqualTo(originalDomainEvent.cas3TransactionId)
  }

  private fun createPremisesArchiveDomainEvent(
    premisesId: UUID,
    data: String,
    cancelledAt: OffsetDateTime? = null,
  ) = domainEventFactory.produceAndPersist {
    withService(ServiceName.temporaryAccommodation)
    withCas3PremisesId(premisesId)
    withType(DomainEventType.CAS3_PREMISES_ARCHIVED)
    withCas3CancelledAt(cancelledAt)
    withData(data)
  }

  private fun createPremisesArchiveEventData(
    premisesId: UUID,
    userId: UUID,
    date: LocalDate,
    transactionId: UUID? = null,
  ) = CAS3PremisesArchiveEvent(
    id = UUID.randomUUID(),
    timestamp = Instant.now(),
    eventType = EventType.premisesArchived,
    eventDetails = CAS3PremisesArchiveEventDetails(
      premisesId = premisesId,
      endDate = date,
      userId = userId,
      transactionId = transactionId,
    ),
  )

  private fun createPremisesArchiveEventDataV1(
    premisesId: UUID,
    userId: UUID,
    date: LocalDate,
    transactionId: UUID,
  ) = CAS3PremisesArchiveEventV1(
    id = UUID.randomUUID(),
    timestamp = Instant.now(),
    eventType = EventType.premisesArchived,
    premisesId = premisesId,
    transactionId = transactionId,
    eventDetails = CAS3PremisesArchiveEventDetailsV1(
      endDate = date,
      userId = userId,
    ),
  )

  private fun createPremisesUnarchiveDomainEvent(
    premisesId: UUID,
    data: String,
    cancelledAt: OffsetDateTime? = null,
    transactionId: UUID = UUID.randomUUID(),
  ) = domainEventFactory.produceAndPersist {
    withService(ServiceName.temporaryAccommodation)
    withCas3PremisesId(premisesId)
    withType(DomainEventType.CAS3_PREMISES_UNARCHIVED)
    withCas3TransactionId(transactionId)
    withCas3CancelledAt(cancelledAt)
    withData(data)
  }

  @SuppressWarnings("LongParameterList")
  private fun createPremisesUnarchiveEventData(
    premisesId: UUID,
    userId: UUID,
    currentStartDate: LocalDate,
    newStartDate: LocalDate,
    currentEndDate: LocalDate,
    transactionId: UUID? = null,
  ) = CAS3PremisesUnarchiveEvent(
    id = UUID.randomUUID(),
    timestamp = Instant.now(),
    eventType = EventType.premisesUnarchived,
    eventDetails = CAS3PremisesUnarchiveEventDetails(
      premisesId = premisesId,
      currentStartDate = currentStartDate,
      newStartDate = newStartDate,
      currentEndDate = currentEndDate,
      userId = userId,
      transactionId = transactionId,
    ),
  )

  @SuppressWarnings("LongParameterList")
  private fun createPremisesUnarchiveEventDataV1(
    premisesId: UUID,
    userId: UUID,
    currentStartDate: LocalDate,
    newStartDate: LocalDate,
    currentEndDate: LocalDate,
    transactionId: UUID = UUID.randomUUID(),
  ) = CAS3PremisesUnarchiveEventV1(
    id = UUID.randomUUID(),
    timestamp = Instant.now(),
    eventType = EventType.premisesUnarchived,
    premisesId = premisesId,
    transactionId = transactionId,
    eventDetails = CAS3PremisesUnarchiveEventDetailsV1(
      currentStartDate = currentStartDate,
      newStartDate = newStartDate,
      currentEndDate = currentEndDate,
      userId = userId,
    ),
  )

  @SuppressWarnings("LongParameterList")
  private fun createBedspaceArchiveDomainEvent(
    bedspaceId: UUID,
    premisesId: UUID,
    data: String,
    cancelledAt: OffsetDateTime? = null,
  ) = domainEventFactory.produceAndPersist {
    withService(ServiceName.temporaryAccommodation)
    withCas3BedspaceId(bedspaceId)
    withCas3PremisesId(premisesId)
    withType(DomainEventType.CAS3_BEDSPACE_ARCHIVED)
    withCas3CancelledAt(cancelledAt)
    withData(data)
  }

  @SuppressWarnings("LongParameterList")
  private fun createBedspaceArchiveEventData(
    bedspaceId: UUID,
    premisesId: UUID,
    userId: UUID,
    currentEndDate: LocalDate?,
    endDate: LocalDate,
    transactionId: UUID = UUID.randomUUID(),
  ) = CAS3BedspaceArchiveEvent(
    id = UUID.randomUUID(),
    timestamp = OffsetDateTime.now().toInstant(),
    eventType = EventType.bedspaceArchived,
    eventDetails = CAS3BedspaceArchiveEventDetails(
      bedspaceId = bedspaceId,
      premisesId = premisesId,
      currentEndDate = currentEndDate,
      endDate = endDate,
      userId = userId,
      transactionId = transactionId,
    ),
  )

  @SuppressWarnings("LongParameterList")
  private fun createBedspaceArchiveEventDataV1(
    bedspaceId: UUID,
    premisesId: UUID,
    userId: UUID,
    currentEndDate: LocalDate?,
    endDate: LocalDate,
    transactionId: UUID = UUID.randomUUID(),
  ) = CAS3BedspaceArchiveEventV1(
    id = UUID.randomUUID(),
    timestamp = OffsetDateTime.now().toInstant(),
    eventType = EventType.bedspaceArchived,
    bedspaceId = bedspaceId,
    premisesId = premisesId,
    transactionId = transactionId,
    eventDetails = CAS3BedspaceArchiveEventDetailsV1(
      currentEndDate = currentEndDate,
      endDate = endDate,
      userId = userId,
    ),
  )

  @SuppressWarnings("LongParameterList")
  private fun createBedspaceUnarchiveDomainEvent(
    bedspaceId: UUID,
    premisesId: UUID,
    data: String,
    cancelledAt: OffsetDateTime? = null,
    transactionId: UUID = UUID.randomUUID(),
  ) = domainEventFactory.produceAndPersist {
    withService(ServiceName.temporaryAccommodation)
    withCas3BedspaceId(bedspaceId)
    withCas3PremisesId(premisesId)
    withType(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)
    withCas3TransactionId(transactionId)
    withCas3CancelledAt(cancelledAt)
    withData(data)
  }

  @SuppressWarnings("LongParameterList")
  private fun createBedspaceUnarchiveEventData(
    bedspaceId: UUID,
    premisesId: UUID,
    bedspaceStartDate: LocalDate,
    bedspaceEndDate: LocalDate,
    newStartDate: LocalDate,
    userId: UUID,
    transactionId: UUID?,
  ) = CAS3BedspaceUnarchiveEvent(
    id = UUID.randomUUID(),
    timestamp = OffsetDateTime.now().toInstant(),
    eventType = EventType.bedspaceUnarchived,
    eventDetails = CAS3BedspaceUnarchiveEventDetails(
      bedspaceId = bedspaceId,
      premisesId = premisesId,
      userId = userId,
      currentStartDate = bedspaceStartDate,
      currentEndDate = bedspaceEndDate,
      newStartDate = newStartDate,
      transactionId = transactionId,
    ),
  )

  @SuppressWarnings("LongParameterList")
  private fun createBedspaceUnarchiveEventDataV1(
    bedspaceId: UUID,
    premisesId: UUID,
    bedspaceStartDate: LocalDate,
    bedspaceEndDate: LocalDate,
    newStartDate: LocalDate,
    userId: UUID,
    transactionId: UUID,
  ) = CAS3BedspaceUnarchiveEventV1(
    id = UUID.randomUUID(),
    timestamp = OffsetDateTime.now().toInstant(),
    eventType = EventType.bedspaceUnarchived,
    bedspaceId = bedspaceId,
    premisesId = premisesId,
    transactionId = transactionId,
    eventDetails = CAS3BedspaceUnarchiveEventDetailsV1(
      userId = userId,
      currentStartDate = bedspaceStartDate,
      currentEndDate = bedspaceEndDate,
      newStartDate = newStartDate,
    ),
  )
}
