package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3UpdateArchiveUnarchiveDomainEventTransactionJobTest : MigrationJobTestBase() {
  @Test
  fun `all archive and unarchive domain events are updated with transaction Id`() {
    val probationRegion = givenAProbationRegion()

    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    val premisesOne = UUID.randomUUID()
    val premisesTwo = UUID.randomUUID()
    val premisesThree = UUID.randomUUID()
    val premisesFour = UUID.randomUUID()
    val premisesFive = UUID.randomUUID()

    val premisesOneArchiveDomainEvent =
      createPremisesArchiveDomainEvent(premisesOne, user, LocalDate.now().minusDays(15))
    updateCreatedAtDomainEvent(premisesOneArchiveDomainEvent, OffsetDateTime.now().minusDays(15))

    val premisesTwoArchiveDomainEvent =
      createPremisesArchiveDomainEvent(premisesTwo, user, LocalDate.now().minusDays(10))
    updateCreatedAtDomainEvent(premisesTwoArchiveDomainEvent, OffsetDateTime.now().minusDays(10))

    val premisesThreeArchiveDomainEvent = createPremisesArchiveDomainEvent(
      premisesThree,
      user,
      LocalDate.now().minusDays(7),
      OffsetDateTime.now().minusDays(3),
    )
    updateCreatedAtDomainEvent(premisesThreeArchiveDomainEvent, OffsetDateTime.now().minusDays(7))

    val bedspaceOnePremisesThree = UUID.randomUUID()
    val bedspaceOnePremisesThreeArchiveDomainEvent = createBedspaceArchiveDomainEvent(
      bedspaceOnePremisesThree,
      premisesThree,
      user.id,
      null,
      LocalDate.now().minusDays(7),
    )
    updateCreatedAtDomainEvent(
      bedspaceOnePremisesThreeArchiveDomainEvent,
      OffsetDateTime.now().minusDays(7).minusMinutes(1),
    )

    val bedspaceTwoPremisesThree = UUID.randomUUID()
    val bedspaceTwoPremisesThreeArchiveDomainEvent = createBedspaceArchiveDomainEvent(
      bedspaceTwoPremisesThree,
      premisesThree,
      user.id,
      null,
      LocalDate.now().minusDays(7),
    )
    updateCreatedAtDomainEvent(
      bedspaceTwoPremisesThreeArchiveDomainEvent,
      OffsetDateTime.now().minusDays(7).plusSeconds(30),
    )

    val bedspaceOnePremisesFour = UUID.randomUUID()
    val bedspaceOnePremisesFourArchiveDomainEvent = createBedspaceArchiveDomainEvent(
      bedspaceOnePremisesFour,
      premisesFour,
      user.id,
      null,
      LocalDate.now().plusDays(7),
    )
    updateCreatedAtDomainEvent(bedspaceOnePremisesFourArchiveDomainEvent, OffsetDateTime.now().minusDays(2))

    val premisesFourArchiveDomainEvent = createPremisesArchiveDomainEvent(
      premisesFour,
      user,
      LocalDate.now().plusDays(7),
      OffsetDateTime.now().minusDays(3),
    )
    updateCreatedAtDomainEvent(premisesFourArchiveDomainEvent, OffsetDateTime.now().minusDays(1))

    val premisesFiveArchiveDomainEvent = createPremisesArchiveDomainEvent(
      premisesFive,
      user,
      LocalDate.now().plusDays(7),
      transactionId = UUID.randomUUID(),
    )
    updateCreatedAtDomainEvent(premisesFiveArchiveDomainEvent, OffsetDateTime.now().minusDays(6))

    migrationJobService.runMigrationJob(MigrationJobType.updateCas3DomainEventArchiveUnarchiveTransaction, 10)

    val domainEvents = domainEventRepository.findByTypes(
      listOf(
        DomainEventType.CAS3_BEDSPACE_ARCHIVED,
        DomainEventType.CAS3_BEDSPACE_UNARCHIVED,
        DomainEventType.CAS3_PREMISES_ARCHIVED,
        DomainEventType.CAS3_PREMISES_UNARCHIVED,
      ),
    )

    domainEvents.forEach {
      assertThat(it.cas3TransactionId).isNotNull()
    }

    assertDomainEventByTransactionId(premisesOneArchiveDomainEvent.id, 1)

    assertDomainEventByTransactionId(premisesTwoArchiveDomainEvent.id, 1)

    assertDomainEventByTransactionId(premisesThreeArchiveDomainEvent.id, 3)

    assertDomainEventByTransactionId(premisesFourArchiveDomainEvent.id, 1)

    assertDomainEventByTransactionId(bedspaceOnePremisesFourArchiveDomainEvent.id, 1)

    assertDomainEventByTransactionId(premisesFiveArchiveDomainEvent.id, 1)
  }

  private fun assertDomainEventByTransactionId(domainEventId: UUID, count: Int) {
    val domainEvent = domainEventRepository.findById(domainEventId).get()
    val domainEvents = domainEventRepository.findByCas3TransactionId(domainEvent.cas3TransactionId!!)
    assertThat(domainEvents.size).isEqualTo(count)
  }

  private fun updateCreatedAtDomainEvent(domainEvent: DomainEventEntity, createdAt: OffsetDateTime) {
    val updatedDomainEvent = domainEvent.copy(createdAt = createdAt)
    domainEventRepository.save(updatedDomainEvent)
  }

  private fun createPremisesArchiveDomainEvent(
    premisesId: UUID,
    userEntity: UserEntity,
    date: LocalDate,
    cancelledAt: OffsetDateTime? = null,
    transactionId: UUID? = null,
  ) = domainEventFactory.produceAndPersist {
    withService(ServiceName.temporaryAccommodation)
    withCas3PremisesId(premisesId)
    withType(DomainEventType.CAS3_PREMISES_ARCHIVED)
    withCas3CancelledAt(cancelledAt)
    withData(
      objectMapper.writeValueAsString(
        CAS3PremisesArchiveEvent(
          id = UUID.randomUUID(),
          timestamp = Instant.now(),
          eventType = EventType.premisesArchived,
          premisesId = premisesId,
          transactionId = transactionId,
          eventDetails =
          CAS3PremisesArchiveEventDetails(
            userId = userEntity.id,
            endDate = date,
          ),
        ),
      ),
    )
  }

  @SuppressWarnings("LongParameterList")
  private fun createBedspaceArchiveDomainEvent(
    bedspaceId: UUID,
    premisesId: UUID,
    userId: UUID,
    currentEndDate: LocalDate?,
    endDate: LocalDate,
    cancelledAt: OffsetDateTime? = null,
    transactionId: UUID? = null,
  ) = domainEventFactory.produceAndPersist {
    withService(ServiceName.temporaryAccommodation)
    withCas3BedspaceId(bedspaceId)
    withCas3PremisesId(premisesId)
    withType(DomainEventType.CAS3_BEDSPACE_ARCHIVED)
    withCas3CancelledAt(cancelledAt)
    withData(
      objectMapper.writeValueAsString(
        CAS3BedspaceArchiveEvent(
          id = UUID.randomUUID(),
          timestamp = OffsetDateTime.now().toInstant(),
          eventType = EventType.bedspaceArchived,
          bedspaceId = bedspaceId,
          premisesId = premisesId,
          transactionId = transactionId,
          eventDetails = CAS3BedspaceArchiveEventDetails(
            userId = userId,
            currentEndDate = currentEndDate,
            endDate = endDate,
          ),
        ),
      ),
    )
  }
}
