package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_BEDSPACE_ARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_PREMISES_ARCHIVED
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3UpdatePremisesArchiveDateSeedJobTest : SeedTestBase() {
  private val currentDate = LocalDate.of(2026, 11, 15)
  private val requiredDate = currentDate.minusDays(7)
  private val startDate = requiredDate.minusMonths(1)

  @Test
  fun `rejects a correction for a premises that has no end date`() {
    val premises = givenACas3Premises(id = UUID.randomUUID(), status = Cas3PremisesStatus.archived, startDate = startDate, endDate = null)
    val premisesDomainEvent = createPremisesArchiveDomainEvent(premises.id, UUID.randomUUID())

    seedCorrection(
      correctionRow(
        premisesId = premises.id,
        currentArchiveDate = LocalDate.now(),
        requiredArchiveDate = requiredDate,
        requiredStartDate = startDate,
      ),
    )

    assertError(1, "CAS3 premises ${premises.id} is not currently archived")
    assertThat(cas3PremisesRepository.findById(premises.id).get().endDate).isNull()
    assertDomainEventEndDate(premisesDomainEvent, currentDate)
  }

  @Test
  fun `rejects a correction for a premises that has an archive date in the future`() {
    val futureArchiveDate = LocalDate.now().plusDays(30)
    val premises = givenACas3Premises(id = UUID.randomUUID(), status = Cas3PremisesStatus.archived, startDate = startDate, endDate = futureArchiveDate)
    val premisesDomainEvent = createPremisesArchiveDomainEvent(premises.id, UUID.randomUUID())

    seedCorrection(
      correctionRow(
        premisesId = premises.id,
        currentArchiveDate = futureArchiveDate,
        requiredArchiveDate = requiredDate,
        requiredStartDate = startDate,
      ),
    )

    assertError(1, "CAS3 premises ${premises.id} is not currently archived")
    assertThat(cas3PremisesRepository.findById(premises.id).get().endDate).isEqualTo(futureArchiveDate)
    assertDomainEventEndDate(premisesDomainEvent, currentDate)
  }

  @Test
  fun `rejects a correction for a premises whose status is not archived`() {
    val premises = givenACas3Premises(id = UUID.randomUUID(), status = Cas3PremisesStatus.online, startDate = startDate, endDate = currentDate)
    val premisesDomainEvent = createPremisesArchiveDomainEvent(premises.id, UUID.randomUUID())

    seedCorrection(
      correctionRow(
        premisesId = premises.id,
        currentArchiveDate = currentDate,
        requiredArchiveDate = requiredDate,
        requiredStartDate = startDate,
      ),
    )

    assertError(1, "CAS3 premises ${premises.id} has status online, expected archived")
    assertThat(cas3PremisesRepository.findById(premises.id).get().endDate).isEqualTo(currentDate)
    assertDomainEventEndDate(premisesDomainEvent, currentDate)
  }

  @Test
  fun `rejects a correction with a required archive date in the future`() {
    val premises = givenACas3Premises(id = UUID.randomUUID(), status = Cas3PremisesStatus.archived, startDate = startDate, endDate = currentDate)
    val premisesDomainEvent = createPremisesArchiveDomainEvent(premises.id, UUID.randomUUID())
    val futureDate = LocalDate.now(clock).plusDays(1)

    seedCorrection(
      correctionRow(
        premisesId = premises.id,
        currentArchiveDate = currentDate,
        requiredArchiveDate = futureDate,
        requiredStartDate = startDate,
      ),
    )

    assertError(1, "CAS3 premises ${premises.id} cannot have an archive date in the future ($futureDate)")
    assertThat(cas3PremisesRepository.findById(premises.id).get().endDate).isEqualTo(currentDate)
    assertDomainEventEndDate(premisesDomainEvent, currentDate)
  }

  @Test
  fun `rejects a correction with a required archive date before the required start date`() {
    val requiredStartDate = requiredDate.plusDays(1)
    val premises = givenACas3Premises(id = UUID.randomUUID(), status = Cas3PremisesStatus.archived, startDate = startDate, endDate = currentDate)
    val premisesDomainEvent = createPremisesArchiveDomainEvent(premises.id, UUID.randomUUID())

    seedCorrection(
      correctionRow(
        premisesId = premises.id,
        currentArchiveDate = currentDate,
        requiredArchiveDate = requiredDate,
        requiredStartDate = requiredStartDate,
      ),
    )

    assertError(1, "CAS3 premises ${premises.id} cannot have an archive date before its start date $requiredStartDate")
    val unchangedPremises = cas3PremisesRepository.findById(premises.id).get()
    assertThat(unchangedPremises.startDate).isEqualTo(startDate)
    assertThat(unchangedPremises.endDate).isEqualTo(currentDate)
    assertDomainEventEndDate(premisesDomainEvent, currentDate)
  }

  @Test
  fun `updates premises start date and validates archive date against the required start date`() {
    val requiredStartDate = requiredDate.minusDays(2)
    val premises = givenACas3Premises(id = UUID.randomUUID(), status = Cas3PremisesStatus.archived, startDate = requiredDate.plusDays(1), endDate = currentDate)
    createPremisesArchiveDomainEvent(premises.id, UUID.randomUUID())

    seedCorrection(
      correctionRow(
        premisesId = premises.id,
        currentArchiveDate = currentDate,
        requiredArchiveDate = requiredDate,
        requiredStartDate = requiredStartDate,
      ),
    )

    val updatedPremises = cas3PremisesRepository.findById(premises.id).get()
    assertThat(updatedPremises.startDate).isEqualTo(requiredStartDate)
    assertThat(updatedPremises.endDate).isEqualTo(requiredDate)
  }

  @Test
  fun `rolls back earlier corrections when a row has a stale premises archive date`() {
    val first = givenACas3Premises(id = UUID.randomUUID(), status = Cas3PremisesStatus.archived, startDate = startDate, endDate = currentDate)
    val second = givenACas3Premises(id = UUID.randomUUID(), status = Cas3PremisesStatus.archived, startDate = startDate, endDate = currentDate)
    val firstDomainEvent = createPremisesArchiveDomainEvent(first.id, UUID.randomUUID())
    val staleDate = currentDate.minusDays(1)

    seedCorrection(
      correctionRow(
        premisesId = first.id,
        currentArchiveDate = currentDate,
        requiredArchiveDate = requiredDate,
        requiredStartDate = startDate,
      ),
      correctionRow(
        premisesId = second.id,
        currentArchiveDate = staleDate,
        requiredArchiveDate = requiredDate,
        requiredStartDate = startDate,
      ),
    )

    assertError(2, "CAS3 premises ${second.id} has end date $currentDate, expected $staleDate")
    assertThat(cas3PremisesRepository.findById(first.id).get().endDate).isEqualTo(currentDate)
    assertDomainEventEndDate(firstDomainEvent, currentDate)
    assertThat(archiveDateUpdateMessages()).isEmpty()
  }

  @ParameterizedTest
  @CsvSource("existingBookings, 3", "existingTurnaround, 2", "existingVoid, 4")
  fun `rejects a correction that conflicts with bedspace use`(conflict: String, earliestArchiveDaysAfterRequiredDate: Long) {
    clock.setNow(requiredDate.atStartOfDay())
    val premises = givenACas3Premises(id = UUID.randomUUID(), status = Cas3PremisesStatus.archived, startDate = startDate, endDate = currentDate)
    val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
      withPremises(premises)
      withStartDate(startDate)
      withEndDate(currentDate)
    }
    val transactionId = UUID.randomUUID()
    val premisesDomainEvent = createPremisesArchiveDomainEvent(premises.id, transactionId)
    val bedspaceDomainEvent = createBedspaceArchiveDomainEvent(premises.id, bedspace.id, transactionId)

    if (conflict == "existingVoid") {
      cas3VoidBedspaceEntityFactory.produceAndPersist {
        withBedspace(bedspace)
        withStartDate(requiredDate.minusDays(3))
        withEndDate(requiredDate.plusDays(3))
        withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
      }
    } else {
      val booking = cas3BookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBedspace(bedspace)
        withArrivalDate(requiredDate.minusDays(5))
        withDepartureDate(if (conflict == "existingTurnaround") requiredDate else requiredDate.plusDays(2))
        withStatus(Cas3BookingStatus.closed)
      }
      if (conflict == "existingTurnaround") {
        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()
        cas3TurnaroundFactory.produceAndPersist {
          withBooking(booking)
          withWorkingDayCount(1)
        }
      }
    }

    seedCorrection(
      correctionRow(
        premisesId = premises.id,
        currentArchiveDate = currentDate,
        requiredArchiveDate = requiredDate,
        requiredStartDate = startDate,
      ),
    )

    val earliestArchiveDate = requiredDate.plusDays(earliestArchiveDaysAfterRequiredDate)
    assertError(1, "CAS3 premises ${premises.id} cannot be archived on $requiredDate: $conflict (bedspace ${bedspace.id}, earliest archive date $earliestArchiveDate)")
    assertThat(cas3PremisesRepository.findById(premises.id).get().endDate).isEqualTo(currentDate)
    assertThat(cas3BedspacesRepository.findById(bedspace.id).get().endDate).isEqualTo(currentDate)
    assertDomainEventEndDate(premisesDomainEvent, currentDate)
    assertDomainEventEndDate(bedspaceDomainEvent, currentDate)
  }

  @Test
  fun `rejects a correction that would archive a linked bedspace before its start date`() {
    val premises = givenACas3Premises(id = UUID.randomUUID(), status = Cas3PremisesStatus.archived, startDate = startDate, endDate = currentDate)
    val bedspaceStart = requiredDate.plusDays(1)
    val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
      withPremises(premises)
      withStartDate(bedspaceStart)
      withEndDate(currentDate)
    }
    val transactionId = UUID.randomUUID()
    val premisesDomainEvent = createPremisesArchiveDomainEvent(premises.id, transactionId)
    val bedspaceDomainEvent = createBedspaceArchiveDomainEvent(premises.id, bedspace.id, transactionId)

    seedCorrection(
      correctionRow(
        premisesId = premises.id,
        currentArchiveDate = currentDate,
        requiredArchiveDate = requiredDate,
        requiredStartDate = startDate,
      ),
    )

    assertError(1, "CAS3 bedspace ${bedspace.id} cannot have an archive date before its start date $bedspaceStart")
    assertThat(cas3PremisesRepository.findById(premises.id).get().endDate).isEqualTo(currentDate)
    assertThat(cas3BedspacesRepository.findById(bedspace.id).get().endDate).isEqualTo(currentDate)
    assertDomainEventEndDate(premisesDomainEvent, currentDate)
    assertDomainEventEndDate(bedspaceDomainEvent, currentDate)
  }

  @Test
  fun `updates premises and linked bedspaces (including domain events) and preserves unrelated archives and event history`() {
    val premises = givenACas3Premises(id = UUID.randomUUID(), status = Cas3PremisesStatus.archived, startDate = startDate, endDate = currentDate)
    val transactionId = UUID.randomUUID()
    val premisesDomainEvent = createPremisesArchiveDomainEvent(premises.id, transactionId)
    val bedspaces = cas3BedspaceEntityFactory.produceAndPersistMultiple(4) {
      withPremises(premises)
      withStartDate(startDate)
      withEndDate(currentDate)
    }
    val linkedDomainEvents = bedspaces.take(2).map { createBedspaceArchiveDomainEvent(premises.id, it.id, transactionId) }
    val unrelatedDomainEvent = createBedspaceArchiveDomainEvent(premises.id, bedspaces[2].id, UUID.randomUUID())
    val cancelledDomainEvent = createBedspaceArchiveDomainEvent(premises.id, bedspaces[3].id, transactionId, OffsetDateTime.parse("2026-11-14T12:00:00Z"))

    seedCorrection(
      correctionRow(
        premisesId = premises.id,
        currentArchiveDate = currentDate,
        requiredArchiveDate = requiredDate,
        requiredStartDate = startDate,
      ),
    )

    val updatedPremises = cas3PremisesRepository.findById(premises.id).get()
    assertThat(updatedPremises.endDate).isEqualTo(requiredDate)
    assertThat(updatedPremises.status).isEqualTo(Cas3PremisesStatus.archived)
    assertThat(bedspaces.map { cas3BedspacesRepository.findById(it.id).get().endDate })
      .containsExactly(requiredDate, requiredDate, currentDate, currentDate)
    (listOf(premisesDomainEvent) + linkedDomainEvents).forEach { domainEvent -> assertDomainEventEndDate(domainEvent, requiredDate) }
    listOf(unrelatedDomainEvent, cancelledDomainEvent).forEach { domainEvent -> assertDomainEventEndDate(domainEvent, currentDate) }
    assertThat(domainEventRepository.findByCas3TransactionId(transactionId).map(DomainEventEntity::id))
      .containsExactlyInAnyOrderElementsOf((listOf(premisesDomainEvent, cancelledDomainEvent) + linkedDomainEvents).map(DomainEventEntity::id))
    val dateChange = "from $currentDate to $requiredDate"
    assertThat(archiveDateUpdateMessages()).containsExactlyInAnyOrderElementsOf(
      listOf(
        "Updated archive date for CAS3 premises ${premises.id} $dateChange",
        "Updated archive date for CAS3 premises archive domain event ${premisesDomainEvent.id} $dateChange",
      ) + bedspaces.take(2).map { "Updated archive date for CAS3 bedspace ${it.id} $dateChange" } +
        linkedDomainEvents.map { "Updated archive date for CAS3 bedspace archive domain event ${it.id} $dateChange" },
    )
  }

  @Test
  fun `accepts a correction with a required archive date of today`() {
    val today = LocalDate.now(clock)
    val premises = givenACas3Premises(id = UUID.randomUUID(), status = Cas3PremisesStatus.archived, startDate = startDate, endDate = currentDate)
    createPremisesArchiveDomainEvent(premises.id, UUID.randomUUID())

    seedCorrection(
      correctionRow(
        premisesId = premises.id,
        currentArchiveDate = currentDate,
        requiredArchiveDate = today,
        requiredStartDate = startDate,
      ),
    )

    assertThat(cas3PremisesRepository.findById(premises.id).get().endDate).isEqualTo(today)
  }

  private fun archiveDateUpdateMessages() = logEntries
    .filter { it.level == "info" && it.message.startsWith("Updated archive date for ") }
    .map { it.message }

  private fun seedCorrection(vararg rows: String) = seed(
    SeedFileType.cas3UpdatePremisesArchiveDate,
    rows.joinToString("\n", prefix = "premises_id,current_archive_date,required_archive_date,required_start_date\n"),
  )

  private fun correctionRow(
    premisesId: UUID,
    currentArchiveDate: LocalDate,
    requiredArchiveDate: LocalDate,
    requiredStartDate: LocalDate,
  ) = "$premisesId,$currentArchiveDate,$requiredArchiveDate,$requiredStartDate"

  private fun createPremisesArchiveDomainEvent(
    premisesId: UUID,
    transactionId: UUID,
  ): DomainEventEntity = persistArchiveDomainEvent(
    type = CAS3_PREMISES_ARCHIVED,
    premisesId = premisesId,
    bedspaceId = null,
    transactionId = transactionId,
    cancelledAt = null,
  ) { domainEventId, timestamp, userId ->
    CAS3PremisesArchiveEvent(
      eventDetails = CAS3PremisesArchiveEventDetails(premisesId, currentDate, userId, transactionId),
      id = domainEventId,
      timestamp = timestamp,
      eventType = EventType.premisesArchived,
    )
  }

  private fun createBedspaceArchiveDomainEvent(
    premisesId: UUID,
    bedspaceId: UUID,
    transactionId: UUID,
    cancelledAt: OffsetDateTime? = null,
  ): DomainEventEntity = persistArchiveDomainEvent(
    type = CAS3_BEDSPACE_ARCHIVED,
    premisesId = premisesId,
    bedspaceId = bedspaceId,
    transactionId = transactionId,
    cancelledAt = cancelledAt,
  ) { domainEventId, timestamp, userId ->
    CAS3BedspaceArchiveEvent(
      eventDetails = CAS3BedspaceArchiveEventDetails(bedspaceId, premisesId, currentDate, currentDate.plusDays(1), userId, transactionId),
      id = domainEventId,
      timestamp = timestamp,
      eventType = EventType.bedspaceArchived,
    )
  }

  private fun persistArchiveDomainEvent(
    type: DomainEventType,
    premisesId: UUID,
    bedspaceId: UUID?,
    transactionId: UUID,
    cancelledAt: OffsetDateTime?,
    payload: (domainEventId: UUID, timestamp: Instant, userId: UUID) -> Any,
  ): DomainEventEntity = domainEventFactory.produceAndPersist {
    val domainEventId = UUID.randomUUID()
    withId(domainEventId)
    withApplicationId(null)
    withService(ServiceName.temporaryAccommodation)
    withCas3PremisesId(premisesId)
    withCas3BedspaceId(bedspaceId)
    withCas3TransactionId(transactionId)
    withCas3CancelledAt(cancelledAt)
    withType(type)
    withData(jsonMapper.writeValueAsString(payload(domainEventId, Instant.parse("2026-11-14T10:00:00Z"), UUID.randomUUID())))
  }

  private fun assertDomainEventEndDate(domainEvent: DomainEventEntity, date: LocalDate) {
    val persistedDomainEvent = domainEventRepository.findById(domainEvent.id).get()
    val expectedDomainEventData = domainEvent.data.replace("\"endDate\":\"$currentDate\"", "\"endDate\":\"$date\"")
    assertThat(jsonMapper.readTree(persistedDomainEvent.data)).isEqualTo(jsonMapper.readTree(expectedDomainEventData))
    assertThat(persistedDomainEvent.occurredAt).isEqualTo(domainEvent.occurredAt)
    assertThat(persistedDomainEvent.cas3TransactionId).isEqualTo(domainEvent.cas3TransactionId)
    assertThat(persistedDomainEvent.cas3CancelledAt).isEqualTo(domainEvent.cas3CancelledAt)
  }
}
