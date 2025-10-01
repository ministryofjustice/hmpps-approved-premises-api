package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.v2

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceCharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceArchiveAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BedspacesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DomainEventEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_BEDSPACE_ARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_BEDSPACE_UNARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class Cas3v2BedspaceServiceTest {
  private val mockCharacteristicService = mockk<CharacteristicService>()
  private val mockCas3BedspacesRepository = mockk<Cas3BedspacesRepository>()
  private val mockCas3v2PremisesService = mockk<Cas3v2PremisesService>()
  private val mockCas3v2DomainEventService = mockk<Cas3v2DomainEventService>()

  private val objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper()

  private val cas3v2BedspacesService = Cas3v2BedspacesService(
    mockCharacteristicService,
    mockCas3BedspacesRepository,
    mockCas3v2PremisesService,
    mockCas3v2DomainEventService,
    objectMapper,
  )

  @Nested
  inner class CreateBedspace {
    @Test
    fun `When create a new bedspace returns Success with correct result when validation passed`() {
      val (premises, bedspace) = createPremisesAndBedspace(
        bedspaceStartDate = LocalDate.now().plusDays(5),
      )

      every { mockCas3BedspacesRepository.save(any()) } returns bedspace

      val result = cas3v2BedspacesService.createBedspace(
        premises,
        bedspaceReference = bedspace.reference,
        startDate = bedspace.startDate,
        notes = null,
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.reference).isEqualTo(bedspace.reference)
        assertThat(it.notes).isEqualTo(bedspace.notes)
        assertThat(it.startDate).isEqualTo(bedspace.startDate)
        assertThat(it.endDate).isEqualTo(bedspace.endDate)
        assertThat(it.createdDate).isEqualTo(bedspace.createdDate)
      }
    }

    @Test
    fun `When create a new bedspace in a scheduled to archive premises returns Success and unarchive the premises`() {
      val bedspaceStartDate = LocalDate.now().plusDays(5)
      val (premises, bedspace) = createPremisesAndBedspace(
        premisesStatus = Cas3PremisesStatus.archived,
        premisesEndDate = LocalDate.now().plusDays(2),
        bedspaceStartDate,
      )

      every { mockCas3BedspacesRepository.save(any()) } returns bedspace
      every { mockCas3v2PremisesService.unarchivePremisesAndSaveDomainEvent(any(), any(), any()) } returns Unit

      val result = cas3v2BedspacesService.createBedspace(
        premises,
        bedspaceReference = bedspace.reference,
        startDate = bedspace.startDate,
        notes = null,
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.reference).isEqualTo(bedspace.reference)
        assertThat(it.notes).isEqualTo(bedspace.notes)
        assertThat(it.startDate).isEqualTo(bedspace.startDate)
        assertThat(it.endDate).isEqualTo(bedspace.endDate)
      }

      verify(exactly = 1) {
        mockCas3BedspacesRepository.save(any())
        mockCas3v2PremisesService.unarchivePremisesAndSaveDomainEvent(eq(premises), eq(bedspaceStartDate), any())
      }
    }

    @Test
    fun `When create a new bedspace with empty bedspace reference returns FieldValidationError with the correct message`() {
      val premises = createPremises(status = Cas3PremisesStatus.online)

      val result = cas3v2BedspacesService.createBedspace(
        premises,
        bedspaceReference = "",
        startDate = LocalDate.now().minusDays(7),
        notes = null,
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "empty")
    }

    @Test
    fun `When create a new bedspace with a start date more than 7 days in the past returns FieldValidationError with the correct message`() {
      val premises = createPremises(status = Cas3PremisesStatus.online)

      val result = cas3v2BedspacesService.createBedspace(
        premises,
        bedspaceReference = randomStringMultiCaseWithNumbers(10),
        startDate = LocalDate.now().minusDays(10),
        notes = null,
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.startDate", "invalidStartDateInThePast")
    }

    @Test
    fun `When create a new bedspace with a start date more than 7 days in the future returns FieldValidationError with the correct message`() {
      val premises = createPremises(status = Cas3PremisesStatus.online)

      val result = cas3v2BedspacesService.createBedspace(
        premises,
        bedspaceReference = randomStringMultiCaseWithNumbers(10),
        startDate = LocalDate.now().plusDays(15),
        notes = null,
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.startDate", "invalidStartDateInTheFuture")
    }

    @Test
    fun `When create a new bedspace with a start date before premises start date returns FieldValidationError with the correct message`() {
      val premises = createPremises(status = Cas3PremisesStatus.online)

      val result = cas3v2BedspacesService.createBedspace(
        premises,
        bedspaceReference = randomStringMultiCaseWithNumbers(10),
        startDate = premises.startDate.minusDays(5),
        notes = null,
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.startDate", premises.id.toString(), "startDateBeforePremisesStartDate", premises.startDate.toString())
    }

    @Test
    fun `When create a new bedspace with a non exist characteristic returns FieldValidationError with the correct message`() {
      val premises = createPremises(status = Cas3PremisesStatus.online)

      val nonExistCharacteristicId = UUID.randomUUID()

      every { mockCharacteristicService.getCas3BedspaceCharacteristic(nonExistCharacteristicId) } returns null

      val result = cas3v2BedspacesService.createBedspace(
        premises,
        bedspaceReference = randomStringMultiCaseWithNumbers(10),
        startDate = LocalDate.now().plusDays(3),
        notes = null,
        characteristicIds = listOf(nonExistCharacteristicId),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.characteristics[0]", "doesNotExist")
    }

    @Test
    fun `When create a new bedspace with reference less than 3 characters returns FieldValidationError with the correct message`() {
      val premises = Cas3PremisesEntityFactory()
        .withDefaults()
        .produce()

      val result = cas3v2BedspacesService.createBedspace(
        premises,
        bedspaceReference = "AB",
        startDate = LocalDate.now().minusDays(1),
        notes = null,
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "bedspaceReferenceNotMeetMinimumLength")
    }

    @Test
    fun `When create a new bedspace with reference containing only special characters returns FieldValidationError with the correct message`() {
      val premises = Cas3PremisesEntityFactory()
        .withDefaults()
        .produce()

      val result = cas3v2BedspacesService.createBedspace(
        premises,
        bedspaceReference = "!@#$%",
        startDate = LocalDate.now().minusDays(1),
        notes = null,
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "bedspaceReferenceMustIncludeLetterOrNumber")
    }

    @Test
    fun `When create a new bedspace with duplicate reference returns FieldValidationError with the correct message`() {
      val premises = Cas3PremisesEntityFactory()
        .withDefaults()
        .produce()

      val existingBedspace = Cas3BedspaceEntityFactory()
        .withPremises(premises)
        .withReference("EXISTING_REF")
        .produce()

      premises.bedspaces.add(existingBedspace)

      val result = cas3v2BedspacesService.createBedspace(
        premises,
        bedspaceReference = "existing_ref",
        startDate = LocalDate.now().minusDays(1),
        notes = null,
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "bedspaceReferenceExists")
    }
  }

  @Nested
  inner class GetBedspaceArchiveHistory {
    @Test
    fun `When getBedspaceArchiveHistory returns Success with empty list of histories`() {
      val bedspaceId = UUID.randomUUID()

      every { mockCas3v2DomainEventService.getBedspaceActiveDomainEvents(bedspaceId, listOf(CAS3_BEDSPACE_ARCHIVED, CAS3_BEDSPACE_UNARCHIVED)) } returns emptyList()

      val result = cas3v2BedspacesService.getBedspaceArchiveHistory(bedspaceId)

      assertThatCasResult(result).isSuccess().with { archiveHistory ->
        assertThat(archiveHistory).isEqualTo(emptyList<Cas3BedspaceArchiveAction>())
      }
    }

    @Test
    fun `When getBedspaceArchiveHistory returns Success with list of events`() {
      val bedspaceId = UUID.randomUUID()
      val premisesId = UUID.randomUUID()
      val userId = UUID.randomUUID()

      // Archive event scheduled for six months ago
      val endDateSixMonthsAgoArchive = LocalDate.now().minusMonths(6)
      val dataSixMonthsAgoArchive = createBedspaceArchiveEvent(
        premisesId = premisesId,
        bedspaceId = bedspaceId,
        userId = userId,
        currentEndDate = null,
        endDate = endDateSixMonthsAgoArchive,
        transactionId = UUID.randomUUID(),
      )
      val domainEventSixMonthsAgo = createBedspaceArchiveDomainEvent(dataSixMonthsAgoArchive)

      // Archive event scheduled for four months ago
      val endDateFourMonthsAgoArchive = LocalDate.now().minusMonths(4)
      val dataFourMonthsAgoArchive = createBedspaceArchiveEvent(
        premisesId = premisesId,
        bedspaceId = bedspaceId,
        userId = userId,
        currentEndDate = null,
        endDate = endDateFourMonthsAgoArchive,
        transactionId = UUID.randomUUID(),
      )
      val domainEventFourMonthsAgo = createBedspaceArchiveDomainEvent(dataFourMonthsAgoArchive)

      // Archive event scheduled one week ago
      val endDateOneWeekAgoArchive = LocalDate.now().minusWeeks(1)
      val dataOneWeekAgoArchive = createBedspaceArchiveEvent(
        premisesId = premisesId,
        bedspaceId = bedspaceId,
        userId = userId,
        currentEndDate = null,
        endDate = endDateOneWeekAgoArchive,
        transactionId = UUID.randomUUID(),
      )
      val domainEventOneWeekAgo = createBedspaceArchiveDomainEvent(dataOneWeekAgoArchive)

      // Unarchive event scheduled for five months ago
      val newStartDateFiveMonthsAgoUnarchive = LocalDate.now().minusMonths(5)
      val dataFiveMonthsAgoUnarchive = createBedspaceUnarchiveEvent(
        premisesId = premisesId,
        bedspaceId = bedspaceId,
        userId = userId,
        newStartDate = newStartDateFiveMonthsAgoUnarchive,
        currentStartDate = LocalDate.now().minusMonths(10),
        currentEndDate = LocalDate.now().minusMonths(6),
        transactionId = UUID.randomUUID(),
      )
      val domainEventFiveMonthsAgo = createBedspaceUnarchiveDomainEvent(dataFiveMonthsAgoUnarchive)

      // Unarchive event scheduled for six weeks ago
      val newStartDateSixWeeksAgoUnarchive = LocalDate.now().minusWeeks(6)
      val dataSixWeeksAgoUnarchive = createBedspaceUnarchiveEvent(
        premisesId = premisesId,
        bedspaceId = bedspaceId,
        userId = userId,
        newStartDate = newStartDateSixWeeksAgoUnarchive,
        currentStartDate = LocalDate.now().minusMonths(6),
        currentEndDate = LocalDate.now().minusMonths(4),
        transactionId = UUID.randomUUID(),
      )
      val domainEventSixWeeksAgo = createBedspaceUnarchiveDomainEvent(dataSixWeeksAgoUnarchive)

      // Unarchive event scheduled in 3 days
      val newStartDateIn3DaysUnarchive = LocalDate.now().plusDays(3)
      val dataIn3DaysUnarchive = createBedspaceUnarchiveEvent(
        premisesId = premisesId,
        bedspaceId = bedspaceId,
        userId = userId,
        newStartDate = newStartDateIn3DaysUnarchive,
        currentStartDate = LocalDate.now().minusWeeks(6),
        currentEndDate = LocalDate.now().minusWeeks(1),
        transactionId = UUID.randomUUID(),
      )
      val domainEventIn3Days = createBedspaceUnarchiveDomainEvent(dataIn3DaysUnarchive)

      every { mockCas3v2DomainEventService.getBedspaceActiveDomainEvents(bedspaceId, listOf(CAS3_BEDSPACE_ARCHIVED, CAS3_BEDSPACE_UNARCHIVED)) } returns
        listOf(
          domainEventFourMonthsAgo,
          domainEventOneWeekAgo,
          domainEventSixMonthsAgo,
          domainEventFiveMonthsAgo,
          domainEventSixWeeksAgo,
          domainEventIn3Days,
        )

      val result = cas3v2BedspacesService.getBedspaceArchiveHistory(bedspaceId)

      assertThatCasResult(result).isSuccess().with { archiveHistory ->
        assertThat(archiveHistory).isEqualTo(
          listOf(
            Cas3BedspaceArchiveAction(
              Cas3BedspaceStatus.archived,
              dataSixMonthsAgoArchive.eventDetails.endDate,
            ),
            Cas3BedspaceArchiveAction(
              Cas3BedspaceStatus.online,
              dataFiveMonthsAgoUnarchive.eventDetails.newStartDate,
            ),
            Cas3BedspaceArchiveAction(
              Cas3BedspaceStatus.archived,
              dataFourMonthsAgoArchive.eventDetails.endDate,
            ),
            Cas3BedspaceArchiveAction(
              Cas3BedspaceStatus.online,
              dataSixWeeksAgoUnarchive.eventDetails.newStartDate,
            ),
            Cas3BedspaceArchiveAction(
              Cas3BedspaceStatus.archived,
              dataOneWeekAgoArchive.eventDetails.endDate,
            ),
          ),
        )
      }
    }

    @Test
    fun `When getBedspacesArchiveHistory returns Success with list of events`() {
      val bedspaceOneId = UUID.randomUUID()
      val bedspaceTwoId = UUID.randomUUID()
      val bedspaceThreeId = UUID.randomUUID()
      val bedspacesIds = listOf(bedspaceOneId, bedspaceTwoId, bedspaceThreeId)
      val premisesId = UUID.randomUUID()
      val userId = UUID.randomUUID()

      val bedOneArchiveEventOneMonthAgo = createBedspaceArchiveEvent(
        premisesId = premisesId,
        bedspaceId = bedspaceOneId,
        userId = userId,
        currentEndDate = null,
        endDate = LocalDate.now().minusMonths(1),
        transactionId = UUID.randomUUID(),
      )
      val bedOneArchiveDomainEventOneMonthAgo = createBedspaceArchiveDomainEvent(bedOneArchiveEventOneMonthAgo)

      val bedOneArchiveEventOneWeekAgo = createBedspaceArchiveEvent(
        premisesId = premisesId,
        bedspaceId = bedspaceOneId,
        userId = userId,
        currentEndDate = null,
        endDate = LocalDate.now().minusWeeks(1),
        transactionId = UUID.randomUUID(),
      )
      val bedOneArchiveDomainEventOneWeekAgo = createBedspaceArchiveDomainEvent(bedOneArchiveEventOneWeekAgo)

      val bedThreeArchiveEventTwoMonthsAgo = createBedspaceArchiveEvent(
        premisesId = premisesId,
        bedspaceId = bedspaceThreeId,
        userId = userId,
        currentEndDate = null,
        endDate = LocalDate.now().minusWeeks(2),
        transactionId = UUID.randomUUID(),
      )
      val bedThreeArchiveDomainEventTwoMonthsAgo = createBedspaceArchiveDomainEvent(bedThreeArchiveEventTwoMonthsAgo)

      val bedOneUnarchiveEventTwoWeeksAgo = createBedspaceUnarchiveEvent(
        premisesId = premisesId,
        bedspaceId = bedspaceOneId,
        userId = userId,
        newStartDate = LocalDate.now().minusWeeks(2),
        currentStartDate = LocalDate.now().minusWeeks(7),
        currentEndDate = LocalDate.now().minusWeeks(3),
        transactionId = UUID.randomUUID(),
      )
      val bedOneUnarchiveDomainEventTwoWeeksAgo = createUnarchiveDomainEvent(bedOneUnarchiveEventTwoWeeksAgo)

      val bedThreeUnarchiveEventTwoWeeksAgo = createBedspaceUnarchiveEvent(
        premisesId = premisesId,
        bedspaceId = bedspaceThreeId,
        userId = userId,
        newStartDate = LocalDate.now().minusWeeks(1),
        currentStartDate = LocalDate.now().minusWeeks(4),
        currentEndDate = LocalDate.now().minusWeeks(2),
        transactionId = UUID.randomUUID(),
      )
      val bedThreeUnarchiveDomainEventOneWeeksAgo = createUnarchiveDomainEvent(bedThreeUnarchiveEventTwoWeeksAgo)

      every {
        mockCas3v2DomainEventService.getBedspacesActiveDomainEvents(
          bedspacesIds,
          listOf(CAS3_BEDSPACE_ARCHIVED, CAS3_BEDSPACE_UNARCHIVED),
        )
      } returns
        listOf(
          bedOneArchiveDomainEventOneMonthAgo,
          bedOneArchiveDomainEventOneWeekAgo,
          bedThreeArchiveDomainEventTwoMonthsAgo,
          bedOneUnarchiveDomainEventTwoWeeksAgo,
          bedThreeUnarchiveDomainEventOneWeeksAgo,
        )

      val result = cas3v2BedspacesService.getBedspacesArchiveHistory(bedspacesIds)

      assertAll(
        {
          assertThat(result.count()).isEqualTo(3)
          assertThat(result[0].bedspaceId).isEqualTo(bedspaceOneId)
          assertThat(result[0].actions).isEqualTo(
            listOf(
              Cas3BedspaceArchiveAction(
                Cas3BedspaceStatus.archived,
                bedOneArchiveEventOneMonthAgo.eventDetails.endDate,
              ),
              Cas3BedspaceArchiveAction(
                Cas3BedspaceStatus.online,
                bedOneUnarchiveEventTwoWeeksAgo.eventDetails.newStartDate,
              ),
              Cas3BedspaceArchiveAction(
                Cas3BedspaceStatus.archived,
                bedOneArchiveEventOneWeekAgo.eventDetails.endDate,
              ),
            ),
          )
          assertThat(result[1].bedspaceId).isEqualTo(bedspaceTwoId)
          assertThat(result[1].actions).isEmpty()
          assertThat(result[2].bedspaceId).isEqualTo(bedspaceThreeId)
          assertThat(result[2].actions).isEqualTo(
            listOf(
              Cas3BedspaceArchiveAction(
                Cas3BedspaceStatus.archived,
                bedThreeArchiveEventTwoMonthsAgo.eventDetails.endDate,
              ),
              Cas3BedspaceArchiveAction(
                Cas3BedspaceStatus.online,
                bedThreeUnarchiveEventTwoWeeksAgo.eventDetails.newStartDate,
              ),
            ),
          )
        },
      )
    }
  }

  @Nested
  inner class GetBedspaceStatus {
    @Test
    fun `Get bedspace status returns Online status when the bedspace does not have an end date today or in the past or a start date in the future`() {
      val (_, bedspace) = createPremisesAndBedspace(
        bedspaceStartDate = LocalDate.now().minusDays(30),
      )

      every { mockCas3v2DomainEventService.getBedspaceActiveDomainEvents(bedspace.id, listOf(CAS3_BEDSPACE_UNARCHIVED)) } returns emptyList()

      val result = cas3v2BedspacesService.getBedspaceStatus(bedspace)

      assertThat(result).isEqualTo(Cas3BedspaceStatus.online)
    }

    @Test
    fun `Get bedspace status returns archive status when the bedspace end date is today or earlier`() {
      val (_, bedspace) = createPremisesAndBedspace(
        bedspaceStartDate = LocalDate.now().minusDays(75),
        bedspaceEndDate = LocalDate.now(),
      )

      every { mockCas3v2DomainEventService.getBedspaceActiveDomainEvents(bedspace.id, listOf(CAS3_BEDSPACE_UNARCHIVED)) } returns emptyList()

      val result = cas3v2BedspacesService.getBedspaceStatus(bedspace)

      assertThat(result).isEqualTo(Cas3BedspaceStatus.archived)
    }

    @Test
    fun `Get bedspace status returns upcoming status when the bedspace is scheduled to be unarchive in the future`() {
      val bedspaceUnarchiveDate = LocalDate.now().plusDays(3)
      val (premises, bedspace) = createPremisesAndBedspace(
        bedspaceStartDate = bedspaceUnarchiveDate,
      )
      val bedspaceUnarchiveEvent = createBedspaceUnarchiveEvent(
        premisesId = premises.id,
        bedspaceId = bedspace.id,
        userId = UUID.randomUUID(),
        newStartDate = bedspaceUnarchiveDate,
        LocalDate.now().minusWeeks(3),
        LocalDate.now().minusWeeks(1),
        transactionId = UUID.randomUUID(),
      )
      val bedspaceUnarchiveDomainEvent = createBedspaceUnarchiveDomainEvent(bedspaceUnarchiveEvent)

      val bedspaceArchiveEvent = createBedspaceArchiveEvent(
        premisesId = premises.id,
        bedspaceId = bedspace.id,
        userId = UUID.randomUUID(),
        currentEndDate = null,
        endDate = LocalDate.now().minusDays(21),
        transactionId = UUID.randomUUID(),
      )
      val bedspaceArchiveDomainEvent = createBedspaceArchiveDomainEvent(bedspaceArchiveEvent)

      every { mockCas3v2DomainEventService.getBedspaceActiveDomainEvents(bedspace.id, listOf(CAS3_BEDSPACE_UNARCHIVED)) } returns listOf(bedspaceUnarchiveDomainEvent)
      every { mockCas3v2DomainEventService.getBedspaceActiveDomainEvents(bedspace.id, listOf(CAS3_BEDSPACE_ARCHIVED)) } returns listOf(bedspaceArchiveDomainEvent)

      val result = cas3v2BedspacesService.getBedspaceStatus(bedspace)

      assertThat(result).isEqualTo(Cas3BedspaceStatus.upcoming)
    }

    @Test
    fun `Get bedspace status returns upcoming status when the bedspace has a future start date and is newly created`() {
      val (_, bedspace) = createPremisesAndBedspace(
        bedspaceStartDate = LocalDate.now().plusDays(7),
      )
      every { mockCas3v2DomainEventService.getBedspaceActiveDomainEvents(bedspace.id, listOf(CAS3_BEDSPACE_UNARCHIVED)) } returns emptyList()

      val result = cas3v2BedspacesService.getBedspaceStatus(bedspace)

      assertThat(result).isEqualTo(Cas3BedspaceStatus.upcoming)
    }
  }

  @Nested
  inner class UpdateBedspace {
    @Test
    fun `When update a bedspace returns Success with correct result when validation passed`() {
      val (premises, bedspace) = createPremisesAndBedspace()
      val updateBedspaceReference = randomStringMultiCaseWithNumbers(10)
      val updateBedspaceNotes = randomStringMultiCaseWithNumbers(100)
      val updateBedspaceCharacteristic = Cas3BedspaceCharacteristicEntityFactory()
        .produceMany()
        .take(3)
        .toList()

      val updatedBedspace = Cas3BedspacesEntity(
        id = bedspace.id,
        reference = updateBedspaceReference,
        startDate = bedspace.startDate,
        endDate = bedspace.endDate,
        createdAt = bedspace.createdAt,
        notes = updateBedspaceNotes,
        premises = premises,
        characteristics = updateBedspaceCharacteristic.toMutableList(),
        createdDate = bedspace.createdDate,
      )

      every { mockCas3BedspacesRepository.findCas3Bedspace(premises.id, bedspace.id) } returns bedspace
      every { mockCharacteristicService.getCas3BedspaceCharacteristic(any()) } answers {
        val characteristicId = it.invocation.args[0] as UUID
        updateBedspaceCharacteristic.firstOrNull { it.id == characteristicId }
      }
      every { mockCas3BedspacesRepository.save(any()) } returns updatedBedspace

      val result = cas3v2BedspacesService.updateBedspace(
        premises,
        bedspace.id,
        bedspaceReference = updateBedspaceReference,
        notes = updateBedspaceNotes,
        characteristicIds = updateBedspaceCharacteristic.map { it.id },
      )

      assertThatCasResult(result).isSuccess().with { bed ->
        assertThat(updatedBedspace.reference).isEqualTo(updateBedspaceReference)
        assertThat(updatedBedspace.notes).isEqualTo(updateBedspaceNotes)
        assertThat(updatedBedspace).isEqualTo(updatedBedspace)
        assertThat(updatedBedspace.premises).isEqualTo(premises)
      }
    }

    @Test
    fun `When updating a non existing bedspace returns a NotFound with the correct message`() {
      val (premises, _) = createPremisesAndBedspace()
      val nonExistingBedspaceId = UUID.randomUUID()

      every { mockCas3BedspacesRepository.findCas3Bedspace(premises.id, nonExistingBedspaceId) } returns null

      val result = cas3v2BedspacesService.updateBedspace(
        premises,
        bedspaceId = nonExistingBedspaceId,
        bedspaceReference = randomStringMultiCaseWithNumbers(10),
        notes = randomStringMultiCaseWithNumbers(100),
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isNotFound("Bedspace", nonExistingBedspaceId)
    }

    @Test
    fun `When updating a bedspace that belongs to different premises returns a NotFound with the correct message`() {
      val anotherPremises = createPremises()
      val (_, bedspace) = createPremisesAndBedspace()

      every { mockCas3BedspacesRepository.findCas3Bedspace(anotherPremises.id, bedspace.id) } returns null

      val result = cas3v2BedspacesService.updateBedspace(
        anotherPremises,
        bedspace.id,
        bedspaceReference = randomStringMultiCaseWithNumbers(10),
        notes = randomStringMultiCaseWithNumbers(100),
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isNotFound("Bedspace", bedspace.id)
    }

    @Test
    fun `When update a bedspace with an empty bedspace reference returns FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()

      every { mockCas3BedspacesRepository.findCas3Bedspace(premises.id, bedspace.id) } returns bedspace

      val result = cas3v2BedspacesService.updateBedspace(
        premises,
        bedspace.id,
        bedspaceReference = "",
        notes = randomStringMultiCaseWithNumbers(100),
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "empty")
    }

    @Test
    fun `When update a bedspace with reference less than 3 characters returns FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()

      every { mockCas3BedspacesRepository.findCas3Bedspace(premises.id, bedspace.id) } returns bedspace

      val result = cas3v2BedspacesService.updateBedspace(
        premises,
        bedspace.id,
        bedspaceReference = "AB",
        notes = randomStringMultiCaseWithNumbers(100),
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "bedspaceReferenceNotMeetMinimumLength")
    }

    @Test
    fun `When update a bedspace with reference containing only special characters returns FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()

      every { mockCas3BedspacesRepository.findCas3Bedspace(premises.id, bedspace.id) } returns bedspace

      val result = cas3v2BedspacesService.updateBedspace(
        premises,
        bedspace.id,
        bedspaceReference = "!@#$%",
        notes = randomStringMultiCaseWithNumbers(100),
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "bedspaceReferenceMustIncludeLetterOrNumber")
    }

    @Test
    fun `When update a bedspace with duplicate reference returns FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()

      val existingRoom = Cas3BedspaceEntityFactory()
        .withReference("EXISTING_REF")
        .withPremises(premises)
        .produce()

      premises.bedspaces.add(existingRoom)

      every { mockCas3BedspacesRepository.findCas3Bedspace(premises.id, bedspace.id) } returns bedspace

      val result = cas3v2BedspacesService.updateBedspace(
        premises,
        bedspace.id,
        bedspaceReference = "existing_ref",
        notes = null,
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "bedspaceReferenceExists")
    }
  }

  private fun createPremisesAndBedspace(
    premisesStatus: Cas3PremisesStatus = Cas3PremisesStatus.online,
    premisesEndDate: LocalDate? = null,
    bedspaceStartDate: LocalDate = LocalDate.now(),
    bedspaceEndDate: LocalDate? = null,
  ): Pair<Cas3PremisesEntity, Cas3BedspacesEntity> {
    val premises = createPremises(status = premisesStatus, endDate = premisesEndDate)
    val bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .withStartDate(bedspaceStartDate)
      .withCreatedDate(bedspaceStartDate)
      .withEndDate(bedspaceEndDate)
      .produce()
    return Pair(premises, bedspace)
  }

  private fun createPremises(
    id: UUID = UUID.randomUUID(),
    status: Cas3PremisesStatus = Cas3PremisesStatus.online,
    startDate: LocalDate = LocalDate.now().minusDays(180),
    endDate: LocalDate? = null,
  ) = Cas3PremisesEntityFactory()
    .withDefaults()
    .withId(id)
    .withStatus(status)
    .withStartDate(startDate)
    .withEndDate(endDate)
    .produce()

  private fun createUnarchiveDomainEvent(data: CAS3BedspaceUnarchiveEvent) = createDomainEvent(
    data.id,
    data.eventDetails.premisesId,
    data.eventDetails.bedspaceId,
    data.timestamp.atOffset(ZoneOffset.UTC),
    objectMapper.writeValueAsString(data),
    CAS3_BEDSPACE_UNARCHIVED,
  )

  @SuppressWarnings("LongParameterList")
  private fun createBedspaceUnarchiveEvent(
    premisesId: UUID,
    bedspaceId: UUID,
    userId: UUID,
    newStartDate: LocalDate,
    currentStartDate: LocalDate,
    currentEndDate: LocalDate,
    transactionId: UUID,
  ): CAS3BedspaceUnarchiveEvent {
    val eventId = UUID.randomUUID()
    val occurredAt = OffsetDateTime.now()
    return CAS3BedspaceUnarchiveEvent(
      id = eventId,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.bedspaceUnarchived,
      eventDetails = CAS3BedspaceUnarchiveEventDetails(
        bedspaceId = bedspaceId,
        premisesId = premisesId,
        currentStartDate = currentStartDate,
        currentEndDate = currentEndDate,
        newStartDate = newStartDate,
        userId = userId,
        transactionId = transactionId,
      ),
    )
  }

  private fun createBedspaceUnarchiveDomainEvent(data: CAS3BedspaceUnarchiveEvent) = createDomainEvent(
    data.id,
    data.eventDetails.premisesId,
    data.eventDetails.bedspaceId,
    data.timestamp.atOffset(ZoneOffset.UTC),
    objectMapper.writeValueAsString(data),
    CAS3_BEDSPACE_UNARCHIVED,
  )

  @SuppressWarnings("LongParameterList")
  private fun createBedspaceArchiveEvent(premisesId: UUID, bedspaceId: UUID, userId: UUID, currentEndDate: LocalDate?, endDate: LocalDate, transactionId: UUID): CAS3BedspaceArchiveEvent {
    val eventId = UUID.randomUUID()
    val occurredAt = OffsetDateTime.now()
    return CAS3BedspaceArchiveEvent(
      id = eventId,
      timestamp = occurredAt.toInstant(),
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
  }

  private fun createBedspaceArchiveDomainEvent(data: CAS3BedspaceArchiveEvent) = createDomainEvent(
    data.id,
    data.eventDetails.premisesId,
    data.eventDetails.bedspaceId,
    data.timestamp.atOffset(ZoneOffset.UTC),
    objectMapper.writeValueAsString(data),
    CAS3_BEDSPACE_ARCHIVED,
  )

  @SuppressWarnings("LongParameterList")
  private fun createDomainEvent(id: UUID, premisesId: UUID, bedspaceId: UUID? = null, occurredAt: OffsetDateTime, data: String, type: DomainEventType) = DomainEventEntityFactory()
    .withId(id)
    .withCas3PremisesId(premisesId)
    .withCas3BedspaceId(bedspaceId)
    .withType(type)
    .withData(data)
    .withOccurredAt(occurredAt)
    .produce()
}
