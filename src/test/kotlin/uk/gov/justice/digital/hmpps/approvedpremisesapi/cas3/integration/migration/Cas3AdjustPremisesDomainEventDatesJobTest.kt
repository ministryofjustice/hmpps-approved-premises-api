package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesUnarchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class Cas3AdjustPremisesDomainEventDatesJobTest : MigrationJobTestBase() {

  private val cutoff = OffsetDateTime.parse("2025-10-15T00:00:00Z")
  private val bedIds: List<UUID> = listOf(
    UUID.fromString("6d481c4d-e1b1-40a5-84bf-b194851a377f"),
    UUID.fromString("37262519-4ef0-4381-902d-e6fc07e0644d"),
    UUID.fromString("6f4ed5ba-cf99-4b72-b607-6a8e57d6dbeb"),
  )

  @Test
  fun `updates occurredAt, createdAt and JSON date fields for matching IDs after cutoff`() {
    createBeds()

    val matchingId1 = UUID.fromString("69f21552-fc3d-44cc-a8d1-0480ee9f80b9")
    val matchingId2 = UUID.fromString("abc9660e-dac3-4b5d-8bc8-7cd3fc2b5bdc")

    val createdAt = OffsetDateTime.of(2025, 10, 20, 12, 0, 0, 0, ZoneOffset.UTC)
    val occurredAt = createdAt.minusHours(1)

    val premisesId = UUID.randomUUID()
    val userId = UUID.randomUUID()

    val archivePayloadBefore = CAS3PremisesArchiveEvent(
      id = UUID.randomUUID(),
      timestamp = Instant.parse("2025-10-20T11:00:00Z"),
      eventType = EventType.premisesArchived,
      eventDetails = CAS3PremisesArchiveEventDetails(
        premisesId = premisesId,
        endDate = LocalDate.parse("2025-10-25"),
        userId = userId,
        transactionId = UUID.randomUUID(),
      ),
    )

    val unarchivePayloadBefore = CAS3PremisesUnarchiveEvent(
      id = UUID.randomUUID(),
      timestamp = Instant.parse("2025-10-21T09:30:00Z"),
      eventType = EventType.premisesUnarchived,
      eventDetails = CAS3PremisesUnarchiveEventDetails(
        premisesId = premisesId,
        currentStartDate = LocalDate.parse("2025-10-10"),
        newStartDate = LocalDate.parse("2025-10-22"),
        currentEndDate = LocalDate.parse("2025-11-05"),
        userId = userId,
        transactionId = UUID.randomUUID(),
      ),
    )

    val e1 = domainEventFactory.produceAndPersist {
      withId(matchingId1)
      withService(ServiceName.temporaryAccommodation)
      withCas3PremisesId(premisesId)
      withType(DomainEventType.CAS3_PREMISES_ARCHIVED)
      withOccurredAt(occurredAt)
      withCreatedAt(createdAt)
      withData(objectMapper.writeValueAsString(archivePayloadBefore))
    }

    val e2 = domainEventFactory.produceAndPersist {
      withId(matchingId2)
      withService(ServiceName.temporaryAccommodation)
      withCas3PremisesId(premisesId)
      withType(DomainEventType.CAS3_PREMISES_UNARCHIVED)
      withOccurredAt(occurredAt)
      withCreatedAt(createdAt)
      withData(objectMapper.writeValueAsString(unarchivePayloadBefore))
    }

    migrationJobService.runMigrationJob(MigrationJobType.updateCas3PremisesDomainEventDates, 50)

    assertBedStartDate()

    val updated1 = domainEventRepository.findById(e1.id).get()
    val updated2 = domainEventRepository.findById(e2.id).get()

    assertThat(updated1.occurredAt).isEqualTo(occurredAt.minusDays(2))
    assertThat(updated1.createdAt).isEqualTo(createdAt.minusDays(2))
    assertThat(updated2.occurredAt).isEqualTo(occurredAt.minusDays(2))
    assertThat(updated2.createdAt).isEqualTo(createdAt.minusDays(2))

    val archiveAfter = objectMapper.readValue(updated1.data, CAS3PremisesArchiveEvent::class.java)
    assertThat(archiveAfter.timestamp).isEqualTo(archivePayloadBefore.timestamp.minusSeconds(2 * 24 * 60 * 60))

    assertThat(archiveAfter.eventDetails.endDate).isEqualTo(archivePayloadBefore.eventDetails.endDate)

    val unarchiveAfter = objectMapper.readValue(updated2.data, CAS3PremisesUnarchiveEvent::class.java)
    assertThat(unarchiveAfter.timestamp).isEqualTo(unarchivePayloadBefore.timestamp.minusSeconds(2 * 24 * 60 * 60))
    assertThat(unarchiveAfter.eventDetails.currentStartDate).isEqualTo(unarchivePayloadBefore.eventDetails.currentStartDate.minusDays(2))
    assertThat(unarchiveAfter.eventDetails.newStartDate).isEqualTo(unarchivePayloadBefore.eventDetails.newStartDate.minusDays(2))
    assertThat(unarchiveAfter.eventDetails.currentEndDate).isEqualTo(unarchivePayloadBefore.eventDetails.currentEndDate?.minusDays(2))
  }

  @Test
  fun `does not update events before cutoff`() {
    val matchingId = UUID.fromString("753df5e1-750f-44b6-971f-34caacd75156")

    val createdAt = cutoff.minusDays(1)
    val occurredAt = createdAt.minusHours(2)

    val payload = CAS3PremisesArchiveEvent(
      id = UUID.randomUUID(),
      timestamp = Instant.parse("2025-10-10T10:00:00Z"),
      eventType = EventType.premisesArchived,
      eventDetails = CAS3PremisesArchiveEventDetails(
        premisesId = UUID.randomUUID(),
        endDate = LocalDate.parse("2025-10-12"),
        userId = UUID.randomUUID(),
        transactionId = UUID.randomUUID(),
      ),
    )

    val original = domainEventFactory.produceAndPersist {
      withId(matchingId)
      withService(ServiceName.temporaryAccommodation)
      withCas3PremisesId(UUID.randomUUID())
      withType(DomainEventType.CAS3_PREMISES_ARCHIVED)
      withOccurredAt(occurredAt)
      withCreatedAt(createdAt)
      withData(objectMapper.writeValueAsString(payload))
    }

    migrationJobService.runMigrationJob(MigrationJobType.updateCas3PremisesDomainEventDates, 50)

    val after = domainEventRepository.findById(original.id).get()
    assertThat(after.createdAt).isEqualTo(createdAt)
    assertThat(after.occurredAt).isEqualTo(occurredAt)

    val afterPayload = objectMapper.readValue(after.data, CAS3PremisesArchiveEvent::class.java)
    assertThat(afterPayload.timestamp).isEqualTo(payload.timestamp)
    assertThat(afterPayload.eventDetails.endDate).isEqualTo(payload.eventDetails.endDate)
  }

  @Test
  fun `does not update events with IDs not in list`() {
    val notInListId = UUID.randomUUID()

    val createdAt = cutoff.plusDays(5)
    val occurredAt = createdAt.minusHours(1)

    val payload = CAS3PremisesUnarchiveEvent(
      id = UUID.randomUUID(),
      timestamp = Instant.parse("2025-10-25T12:00:00Z"),
      eventType = EventType.premisesUnarchived,
      eventDetails = CAS3PremisesUnarchiveEventDetails(
        premisesId = UUID.randomUUID(),
        currentStartDate = LocalDate.parse("2025-10-01"),
        newStartDate = LocalDate.parse("2025-10-26"),
        currentEndDate = LocalDate.parse("2025-11-30"),
        userId = UUID.randomUUID(),
        transactionId = UUID.randomUUID(),
      ),
    )

    val original = domainEventFactory.produceAndPersist {
      withId(notInListId)
      withService(ServiceName.temporaryAccommodation)
      withCas3PremisesId(UUID.randomUUID())
      withType(DomainEventType.CAS3_PREMISES_UNARCHIVED)
      withOccurredAt(occurredAt)
      withCreatedAt(createdAt)
      withData(objectMapper.writeValueAsString(payload))
    }

    migrationJobService.runMigrationJob(MigrationJobType.updateCas3PremisesDomainEventDates, 50)

    val after = domainEventRepository.findById(original.id).get()
    assertThat(after.createdAt).isEqualTo(createdAt)
    assertThat(after.occurredAt).isEqualTo(occurredAt)

    val afterPayload = objectMapper.readValue(after.data, CAS3PremisesUnarchiveEvent::class.java)
    assertThat(afterPayload.timestamp).isEqualTo(payload.timestamp)
    assertThat(afterPayload.eventDetails.currentStartDate).isEqualTo(payload.eventDetails.currentStartDate)
    assertThat(afterPayload.eventDetails.newStartDate).isEqualTo(payload.eventDetails.newStartDate)
    assertThat(afterPayload.eventDetails.currentEndDate).isEqualTo(payload.eventDetails.currentEndDate)
  }

  @Test
  fun `malformed JSON results in occurredAt and createdAt updated but data unchanged`() {
    val matchingId = UUID.fromString("36375c5f-1c0f-4c49-a8f9-0a645ff432ec")

    val createdAt = cutoff.plusDays(3)
    val occurredAt = createdAt.minusHours(3)

    val badJson = "{" +
      "\"id\": \"${UUID.randomUUID()}\", " +
      "\"eventType\": \"premisesArchived\", " +
      "\"timestamp\": \"not-an-instant\", " +
      "\"eventDetails\": {\"endDate\": \"not-a-date\"}" +
      "}"

    val original = domainEventFactory.produceAndPersist {
      withId(matchingId)
      withService(ServiceName.temporaryAccommodation)
      withCas3PremisesId(UUID.randomUUID())
      withType(DomainEventType.CAS3_PREMISES_ARCHIVED)
      withOccurredAt(occurredAt)
      withCreatedAt(createdAt)
      withData(badJson)
    }

    migrationJobService.runMigrationJob(MigrationJobType.updateCas3PremisesDomainEventDates, 50)

    val after = domainEventRepository.findById(original.id).get()

    assertThat(after.createdAt).isEqualTo(createdAt.minusDays(2))
    assertThat(after.occurredAt).isEqualTo(occurredAt.minusDays(2))
    assertThat(after.data).isEqualTo(badJson)
  }

  private fun assertBedStartDate() {
    bedRepository.findAllById(bedIds).forEach { bed ->
      assertThat(bed.startDate).isEqualTo(LocalDate.parse("2025-10-13"))
    }
  }

  private fun createBeds() {
    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        givenAProbationRegion(name = "the region")
      }
    }

    bedIds.forEach { bedId ->
      bedEntityFactory.produceAndPersist {
        withId(bedId)
        withStartDate(LocalDate.parse("2025-10-15"))
        withRoom(
          roomEntityFactory.produceAndPersist {
            withName("room for $bedId")
            withPremises(premises)
          },
        )
      }
    }
  }
}
