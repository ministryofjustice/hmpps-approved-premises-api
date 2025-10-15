package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.v2

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.v2.Cas3v2TurnaroundEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2ArchiveService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DomainEventEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_BEDSPACE_ARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_BEDSPACE_UNARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_PREMISES_ARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class Cas3v2ArchiveServiceTest {
  private val cas3PremisesRepositoryMock = mockk<Cas3PremisesRepository>()
  private val cas3VoidBedspacesRepositoryMock = mockk<Cas3VoidBedspacesRepository>()
  private val domainEventRepositoryMock = mockk<DomainEventRepository>()
  private val cas3v2BookingRepositoryMock = mockk<Cas3v2BookingRepository>()
  private val cas3BedspaceRepositoryMock = mockk<Cas3BedspacesRepository>()
  private val workingDayServiceMock = mockk<WorkingDayService>()
  private val cas3DomainEventServiceMock = mockk<Cas3v2DomainEventService>()
  private val objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper()

  private val premisesService = Cas3v2ArchiveService(
    cas3BedspaceRepositoryMock,
    cas3PremisesRepositoryMock,
    cas3v2BookingRepositoryMock,
    cas3VoidBedspacesRepositoryMock,
    domainEventRepositoryMock,
    cas3DomainEventServiceMock,
    workingDayServiceMock,
    objectMapper,
    Clock.systemDefaultZone(),
  )

  @Nested
  inner class ArchiveBedspace {
    @Test
    fun `When archive a bedspace returns Success with correct result when validation passed`() {
      val (premises, bedspaceOne) = createPremisesAndBedspace()
      val bedspaceTwo = createBedspace(premises)

      val archiveDate = LocalDate.now().plusDays(3)
      val archivedBedspaceOne = bedspaceOne.copy(
        endDate = archiveDate,
      )

      every { cas3DomainEventServiceMock.getBedspaceActiveDomainEvents(bedspaceOne.id, listOf(CAS3_BEDSPACE_ARCHIVED)) } returns emptyList()
      every { cas3BedspaceRepositoryMock.findByIdOrNull(bedspaceOne.id) } returns bedspaceOne
      every { cas3v2BookingRepositoryMock.findActiveOverlappingBookingByBedspace(bedspaceOne.id, LocalDate.now()) } returns emptyList()
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDateV2(bedspaceOne.id, archiveDate) } returns emptyList()
      every { cas3BedspaceRepositoryMock.save(any()) } returns bedspaceOne
      every { cas3DomainEventServiceMock.saveBedspaceArchiveEvent(bedspaceOne, premises.id, null, any()) } returns Unit
      every { cas3BedspaceRepositoryMock.findByPremisesId(premises.id) } returns listOf(archivedBedspaceOne, bedspaceTwo)

      val result = premisesService.archiveBedspace(bedspaceOne.id, premises, archiveDate)

      assertThatCasResult(result).isSuccess().with { bedspace ->
        assertThat(bedspace.reference).isEqualTo(bedspaceOne.reference)
        assertThat(bedspace.startDate).isEqualTo(bedspaceOne.startDate)
        assertThat(bedspace.endDate).isEqualTo(archiveDate)
        assertThat(bedspace.premises).isEqualTo(bedspaceOne.premises)
      }
    }

    @Test
    fun `When archive the last online bedspace returns Success with correct result and archive the premises`() {
      val (premises, bedspaceOne) = createPremisesAndBedspace()
      val latestBedspaceArchiveDate = LocalDate.now().plusDays(100)
      val bedspaceTwo = createBedspace(premises, endDate = LocalDate.now().minusDays(5))
      val bedspaceThree = createBedspace(premises, endDate = latestBedspaceArchiveDate)

      val archiveDate = LocalDate.now().plusDays(3)
      val archivedBedspaceOne = bedspaceOne.copy(
        endDate = archiveDate,
      )

      every { cas3DomainEventServiceMock.getBedspaceActiveDomainEvents(bedspaceOne.id, listOf(CAS3_BEDSPACE_ARCHIVED)) } returns emptyList()
      every { cas3BedspaceRepositoryMock.findByIdOrNull(bedspaceOne.id) } returns bedspaceOne
      every { cas3v2BookingRepositoryMock.findActiveOverlappingBookingByBedspace(bedspaceOne.id, LocalDate.now()) } returns emptyList()
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDateV2(bedspaceOne.id, archiveDate) } returns emptyList()
      every { cas3BedspaceRepositoryMock.save(match { it.id == bedspaceOne.id }) } returns bedspaceOne
      every { cas3DomainEventServiceMock.saveBedspaceArchiveEvent(bedspaceOne, premises.id, null, any()) } returns Unit
      every { cas3BedspaceRepositoryMock.findByPremisesId(premises.id) } returns listOf(archivedBedspaceOne, bedspaceTwo, bedspaceThree)
      every { cas3PremisesRepositoryMock.save(match { it.id == premises.id && it.status == Cas3PremisesStatus.archived }) } returns premises
      every { cas3DomainEventServiceMock.savePremisesArchiveEvent(match { it.id == premises.id }, latestBedspaceArchiveDate, any()) } returns Unit

      val result = premisesService.archiveBedspace(bedspaceOne.id, premises, archiveDate)

      assertThatCasResult(result).isSuccess().with { bed ->
        assertThat(bed.reference).isEqualTo(bedspaceOne.reference)
        assertThat(bed.startDate).isEqualTo(bedspaceOne.startDate)
        assertThat(bed.endDate).isEqualTo(archiveDate)
        assertThat(bed.premises).isEqualTo(bedspaceOne.premises)
      }

      verify(exactly = 1) {
        cas3PremisesRepositoryMock.save(
          match<Cas3PremisesEntity> {
            it.id == premises.id &&
              it.status == Cas3PremisesStatus.archived &&
              it.endDate == latestBedspaceArchiveDate
          },
        )

        cas3DomainEventServiceMock.savePremisesArchiveEvent(
          match { it.id == premises.id },
          latestBedspaceArchiveDate,
          any(),
        )
      }
    }

    @Test
    fun `When archive a non existing bedspace returns a NotFound with the correct message`() {
      val (premises, _) = createPremisesAndBedspace()
      val nonExistingBedspaceId = UUID.randomUUID()

      every { cas3BedspaceRepositoryMock.findByIdOrNull(nonExistingBedspaceId) } returns null

      val result = premisesService.archiveBedspace(nonExistingBedspaceId, premises, LocalDate.now())

      assertThatCasResult(result).isNotFound("Bedspace", nonExistingBedspaceId)
    }

    @Test
    fun `When archive a bedspace with an end date in the past returns FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()

      every { cas3BedspaceRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace

      val result = premisesService.archiveBedspace(bedspace.id, premises, LocalDate.now().minusDays(8))

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.endDate", "invalidEndDateInThePast")
    }

    @Test
    fun `When archive a bedspace with an end date in the future more than three months returns FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()

      every { cas3BedspaceRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace

      val result = premisesService.archiveBedspace(bedspace.id, premises, LocalDate.now().plusMonths(3).plusDays(1))

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.endDate", "invalidEndDateInTheFuture")
    }

    @Test
    fun `When archive a bedspace with an end date before bedspace start date returns FieldValidationError with the correct message`() {
      val (premises, _) = createPremisesAndBedspace()
      val bedspaceTwo = createBedspace(premises, startDate = LocalDate.now().minusDays(3))

      every { cas3BedspaceRepositoryMock.findByIdOrNull(bedspaceTwo.id) } returns bedspaceTwo

      val result = premisesService.archiveBedspace(bedspaceTwo.id, premises, LocalDate.now().minusDays(5))

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspaceTwo.id.toString(), "endDateBeforeBedspaceStartDate", bedspaceTwo.startDate.toString())
    }

    @Test
    fun `When archive a bedspace with an end date that clashes with an earlier archive bedspace end date returns FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()

      val previousBedspaceArchiveDate = LocalDate.now().minusDays(5)

      val bedspaceArchiveEvent = createBedspaceArchiveEvent(premisesId = premises.id, bedspaceId = bedspace.id, userId = UUID.randomUUID(), currentEndDate = bedspace.endDate, endDate = previousBedspaceArchiveDate)
      val bedspaceArchiveDomainEvent = createBedspaceArchiveDomainEvent(bedspaceArchiveEvent)

      every { cas3BedspaceRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace
      every {
        cas3DomainEventServiceMock.getBedspaceActiveDomainEvents(bedspace.id, listOf(CAS3_BEDSPACE_ARCHIVED))
      } returns listOf(bedspaceArchiveDomainEvent)

      val result = premisesService.archiveBedspace(bedspace.id, premises, LocalDate.now().minusDays(5))

      assertThatCasResult(result).isCas3FieldValidationError()
        .hasMessage("$.endDate", bedspace.id.toString(), "endDateOverlapPreviousBedspaceArchiveEndDate", previousBedspaceArchiveDate.toString())
    }

    @Test
    fun `When archive a bedspace with a void that has an end date after bedspace archive date returns Cas3FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(7)
      val voidBedspace = createVoidBedspace(bedspace, archiveDate.minusDays(3), archiveDate.plusDays(1))

      every { cas3DomainEventServiceMock.getBedspaceActiveDomainEvents(bedspace.id, listOf(CAS3_BEDSPACE_ARCHIVED)) } returns emptyList()
      every { cas3BedspaceRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace
      every { cas3v2BookingRepositoryMock.findActiveOverlappingBookingByBedspace(bedspace.id, LocalDate.now()) } returns listOf()
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDateV2(bedspace.id, archiveDate) } returns listOf(voidBedspace)

      val result = premisesService.archiveBedspace(bedspace.id, premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError()
        .hasMessage("$.endDate", bedspace.id.toString(), "existingVoid", voidBedspace.endDate.plusDays(1).toString())
    }

    @Test
    fun `When archive a bedspace with a booking departure date after bedspace archive date returns Cas3FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(7)
      val booking = createBooking(premises, bedspace, archiveDate.minusDays(3), archiveDate.plusDays(4), Cas3BookingStatus.arrived)

      every { cas3DomainEventServiceMock.getBedspaceActiveDomainEvents(bedspace.id, listOf(CAS3_BEDSPACE_ARCHIVED)) } returns emptyList()
      every { cas3BedspaceRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace
      every { cas3v2BookingRepositoryMock.findActiveOverlappingBookingByBedspace(bedspace.id, LocalDate.now()) } returns listOf(booking)
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDateV2(bedspace.id, archiveDate) } returns listOf()
      every { workingDayServiceMock.addWorkingDays(booking.departureDate, any()) } returns booking.departureDate

      val result = premisesService.archiveBedspace(bedspace.id, premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspace.id.toString(), "existingBookings", booking.departureDate.plusDays(1).toString())
    }

    @Test
    fun `When archive a bedspace with a booking turnaround date after bedspace archive date returns Cas3FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(7)
      val booking = createBooking(premises, bedspace, archiveDate.minusDays(3), archiveDate.plusDays(4), Cas3BookingStatus.arrived)

      createBookingTurnaround(booking, 2)

      every { cas3DomainEventServiceMock.getBedspaceActiveDomainEvents(bedspace.id, listOf(CAS3_BEDSPACE_ARCHIVED)) } returns emptyList()
      every { cas3BedspaceRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace
      every { cas3v2BookingRepositoryMock.findActiveOverlappingBookingByBedspace(bedspace.id, LocalDate.now()) } returns listOf(booking)
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDateV2(bedspace.id, archiveDate) } returns listOf()
      every { workingDayServiceMock.addWorkingDays(booking.departureDate, any()) } returns booking.departureDate.plusDays(2)

      val result = premisesService.archiveBedspace(bedspace.id, premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspace.id.toString(), "existingTurnaround", booking.departureDate.plusDays(3).toString())
    }

    @Test
    fun `When archive a bedspace with a booking and void dates after bedspace archive date returns Cas3FieldValidationError with the correct message and last blocking date`() {
      val (premises, bedspace) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(7)
      val booking = createBooking(premises, bedspace, archiveDate.minusDays(3), archiveDate.plusDays(1), Cas3BookingStatus.arrived)
      val voidBedspace = createVoidBedspace(bedspace, archiveDate.minusDays(4), archiveDate.plusDays(2))

      every { cas3DomainEventServiceMock.getBedspaceActiveDomainEvents(bedspace.id, listOf(CAS3_BEDSPACE_ARCHIVED)) } returns emptyList()
      every { cas3BedspaceRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace
      every { cas3v2BookingRepositoryMock.findActiveOverlappingBookingByBedspace(bedspace.id, LocalDate.now()) } returns listOf(booking)
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDateV2(bedspace.id, archiveDate) } returns listOf(voidBedspace)
      every { workingDayServiceMock.addWorkingDays(booking.departureDate, any()) } returns booking.departureDate

      val result = premisesService.archiveBedspace(bedspace.id, premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspace.id.toString(), "existingVoid", voidBedspace.endDate.plusDays(1).toString())
    }

    @Test
    fun `When archive a bedspace with void and booking that has the same end date both after the bedspace archive date returns Cas3FieldValidationError with the correct message and last blocking date`() {
      val (premises, bedspace) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(7)
      val booking = createBooking(premises, bedspace, archiveDate.minusDays(3), archiveDate.plusDays(4), Cas3BookingStatus.arrived)

      createBookingTurnaround(booking, 2)

      every { cas3DomainEventServiceMock.getBedspaceActiveDomainEvents(bedspace.id, listOf(CAS3_BEDSPACE_ARCHIVED)) } returns emptyList()
      every { cas3BedspaceRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace
      every { cas3v2BookingRepositoryMock.findActiveOverlappingBookingByBedspace(bedspace.id, LocalDate.now()) } returns listOf(booking)
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDateV2(bedspace.id, archiveDate) } returns listOf()
      every { workingDayServiceMock.addWorkingDays(booking.departureDate, any()) } returns booking.departureDate.plusDays(2)

      val result = premisesService.archiveBedspace(bedspace.id, premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspace.id.toString(), "existingTurnaround", booking.departureDate.plusDays(3).toString())
    }
  }

  @Nested
  inner class UnarchiveBedspace {
    @Test
    fun `unarchiveBedspace returns Success when bedspace is successfully unarchived`() {
      val premises = createPremisesEntity()
      val currentEndDate = LocalDate.now().minusDays(1)
      val archivedBedspace = createBedspace(premises, endDate = currentEndDate)
      val currentStartDate = archivedBedspace.startDate

      val restartDate = LocalDate.now().plusDays(1)
      val updatedBedspace = archivedBedspace.copy(startDate = restartDate, endDate = null)

      every { cas3BedspaceRepositoryMock.findCas3Bedspace(premises.id, archivedBedspace.id) } returns archivedBedspace
      every { cas3BedspaceRepositoryMock.save(match { it.id == archivedBedspace.id }) } returns updatedBedspace
      every {
        cas3DomainEventServiceMock.saveBedspaceUnarchiveEvent(
          match { it.id == updatedBedspace.id },
          premises.id,
          currentStartDate,
          currentEndDate,
          any(),
        )
      } returns Unit

      val result = premisesService.unarchiveBedspace(premises, archivedBedspace.id, restartDate)

      assertThatCasResult(result).isSuccess().with { resultEntity ->
        assertThat(resultEntity.startDate).isEqualTo(restartDate)
        assertThat(resultEntity.endDate).isNull()
      }

      verify(exactly = 1) {
        cas3BedspaceRepositoryMock.save(
          match<Cas3BedspacesEntity> {
            it.id == archivedBedspace.id &&
              it.startDate == restartDate &&
              it.endDate == null
          },
        )
      }

      verify(exactly = 1) {
        cas3DomainEventServiceMock.saveBedspaceUnarchiveEvent(
          match<Cas3BedspacesEntity> {
            it.id == updatedBedspace.id
          },
          premises.id,
          currentStartDate,
          currentEndDate,
          any(),
        )
      }
    }

    @Test
    fun `unarchiveBedspace returns Success when unarchive a bedspace in archived premises will unarchive premises and bedspace successfully`() {
      val premises = createPremisesEntity(
        endDate = LocalDate.now().minusDays(7),
        status = Cas3PremisesStatus.archived,
      )
      val currentPremisesStartDate = premises.startDate
      val currentPremisesEndDate = premises.endDate!!
      val currentBedspaceEndDate = LocalDate.now().minusDays(1)
      val archivedBedspace = createBedspace(premises, endDate = currentBedspaceEndDate)
      val currentBedspaceStartDate = archivedBedspace.startDate

      val restartDate = LocalDate.now().plusDays(1)
      val updatedBedspace = archivedBedspace.copy(startDate = restartDate, endDate = null)

      every { cas3BedspaceRepositoryMock.findCas3Bedspace(premises.id, archivedBedspace.id) } returns archivedBedspace
      every { cas3BedspaceRepositoryMock.save(match { it.id == archivedBedspace.id }) } returns updatedBedspace
      every {
        cas3DomainEventServiceMock.saveBedspaceUnarchiveEvent(
          match { it.id == updatedBedspace.id },
          match { it == premises.id },
          currentBedspaceStartDate,
          currentBedspaceEndDate,
          any(),
        )
      } returns Unit
      every { cas3PremisesRepositoryMock.save(match { it.id == premises.id }) } returns premises
      every {
        cas3DomainEventServiceMock.savePremisesUnarchiveEvent(
          match { it.id == premises.id },
          premises.startDate,
          restartDate,
          premises.endDate!!,
          any(),
        )
      } returns Unit

      val result = premisesService.unarchiveBedspace(premises, archivedBedspace.id, restartDate)

      assertThatCasResult(result).isSuccess().with { resultEntity ->
        assertThat(resultEntity.startDate).isEqualTo(restartDate)
        assertThat(resultEntity.endDate).isNull()
      }

      verify(exactly = 1) {
        cas3BedspaceRepositoryMock.save(
          match<Cas3BedspacesEntity> {
            it.id == archivedBedspace.id &&
              it.startDate == restartDate &&
              it.endDate == null
          },
        )

        cas3DomainEventServiceMock.saveBedspaceUnarchiveEvent(
          match<Cas3BedspacesEntity> {
            it.id == updatedBedspace.id
          },
          match { it == premises.id },
          currentBedspaceStartDate,
          currentBedspaceEndDate,
          any(),
        )
      }

      verify(exactly = 1) {
        cas3PremisesRepositoryMock.save(
          match<Cas3PremisesEntity> {
            it.id == premises.id &&
              it.startDate == restartDate &&
              it.endDate == null
          },
        )

        cas3DomainEventServiceMock.savePremisesUnarchiveEvent(
          match { it.id == premises.id },
          currentPremisesStartDate,
          restartDate,
          currentPremisesEndDate,
          any(),
        )
      }
    }

    @Test
    fun `unarchiveBedspace returns FieldValidationError when bedspace does not exist`() {
      val premises = createPremisesEntity()
      val nonExistentBedspaceId = UUID.randomUUID()

      every { cas3BedspaceRepositoryMock.findCas3Bedspace(premises.id, nonExistentBedspaceId) } returns null

      val result = premisesService.unarchiveBedspace(premises, nonExistentBedspaceId, LocalDate.now())

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bedspaceId", "doesNotExist")
    }

    @Test
    fun `unarchiveBedspace returns FieldValidationError when bedspace is not archived`() {
      val (premises, onlineBedspace) = createPremisesAndBedspace()

      every { cas3BedspaceRepositoryMock.findCas3Bedspace(premises.id, onlineBedspace.id) } returns onlineBedspace

      val result = premisesService.unarchiveBedspace(premises, onlineBedspace.id, LocalDate.now())

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bedspaceId", "bedspaceNotArchived")
    }

    @Test
    fun `unarchiveBedspace returns FieldValidationError when restart date is more than 7 days in the past`() {
      val premises = createPremisesEntity()
      val archivedBedspace = createBedspace(premises, endDate = LocalDate.now().minusDays(10))

      val restartDate = LocalDate.now().minusDays(8)

      every { cas3BedspaceRepositoryMock.findCas3Bedspace(premises.id, archivedBedspace.id) } returns archivedBedspace

      val result = premisesService.unarchiveBedspace(premises, archivedBedspace.id, restartDate)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.restartDate", "invalidRestartDateInThePast")
    }

    @Test
    fun `unarchiveBedspace returns FieldValidationError when restart date is more than 7 days in the future`() {
      val premises = createPremisesEntity()
      val archivedBedspace = createBedspace(premises, endDate = LocalDate.now().minusDays(1))

      val restartDate = LocalDate.now().plusDays(8)

      every { cas3BedspaceRepositoryMock.findCas3Bedspace(premises.id, archivedBedspace.id) } returns archivedBedspace

      val result = premisesService.unarchiveBedspace(premises, archivedBedspace.id, restartDate)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.restartDate", "invalidRestartDateInTheFuture")
    }

    @Test
    fun `unarchiveBedspace returns FieldValidationError when restart date is before last archive end date`() {
      val premises = createPremisesEntity()
      val lastArchiveEndDate = LocalDate.now().minusDays(5)
      val archivedBedspace = createBedspace(premises, endDate = lastArchiveEndDate)

      val restartDate = lastArchiveEndDate.minusDays(1)

      every { cas3BedspaceRepositoryMock.findCas3Bedspace(premises.id, archivedBedspace.id) } returns archivedBedspace

      val result = premisesService.unarchiveBedspace(premises, archivedBedspace.id, restartDate)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.restartDate", "beforeLastBedspaceArchivedDate")
    }

    @Test
    fun `unarchiveBedspace allows restart date exactly 7 days in the past`() {
      val premises = createPremisesEntity()
      val currentEndDate = LocalDate.now().minusDays(10)
      val archivedBedspace = createBedspace(premises, endDate = currentEndDate)
      val currentStartDate = archivedBedspace.startDate

      val restartDate = LocalDate.now().minusDays(7)
      val updatedBedspace = archivedBedspace.copy(startDate = restartDate, endDate = null)

      every { cas3BedspaceRepositoryMock.findCas3Bedspace(premises.id, archivedBedspace.id) } returns archivedBedspace
      every { cas3BedspaceRepositoryMock.save(any()) } returns updatedBedspace
      every {
        cas3DomainEventServiceMock.saveBedspaceUnarchiveEvent(
          match { it.id == updatedBedspace.id },
          match { it == premises.id },
          currentStartDate,
          currentEndDate,
          any(),
        )
      } returns Unit

      val result = premisesService.unarchiveBedspace(premises, archivedBedspace.id, restartDate)

      assertThatCasResult(result).isSuccess().with { resultEntity ->
        assertThat(resultEntity.startDate).isEqualTo(restartDate)
        assertThat(resultEntity.endDate).isNull()
      }

      verify(exactly = 1) {
        cas3BedspaceRepositoryMock.save(
          match<Cas3BedspacesEntity> {
            it.id == archivedBedspace.id &&
              it.startDate == restartDate &&
              it.endDate == null
          },
        )

        cas3DomainEventServiceMock.saveBedspaceUnarchiveEvent(
          match<Cas3BedspacesEntity> {
            it.id == updatedBedspace.id
          },
          match { it == premises.id },
          currentStartDate,
          currentEndDate,
          any(),
        )
      }
    }

    @Test
    fun `unarchiveBedspace allows restart date exactly 7 days in the future`() {
      val premises = createPremisesEntity()
      val currentEndDate = LocalDate.now().minusDays(10)
      val archivedBedspace = createBedspace(premises, endDate = currentEndDate)
      val currentStartDate = archivedBedspace.startDate

      val restartDate = LocalDate.now().plusDays(7)
      val updatedBedspace = archivedBedspace.copy(startDate = restartDate, endDate = null)

      every { cas3BedspaceRepositoryMock.findCas3Bedspace(premises.id, archivedBedspace.id) } returns archivedBedspace
      every {
        cas3DomainEventServiceMock.saveBedspaceUnarchiveEvent(
          match { it.id == archivedBedspace.id },
          match { it == premises.id },
          currentStartDate,
          currentEndDate,
          any(),
        )
      } returns Unit
      every { cas3BedspaceRepositoryMock.save(updatedBedspace) } returns updatedBedspace

      val result = premisesService.unarchiveBedspace(premises, archivedBedspace.id, restartDate)

      assertThatCasResult(result).isSuccess().with { resultEntity ->
        assertThat(resultEntity.startDate).isEqualTo(restartDate)
        assertThat(resultEntity.endDate).isNull()
      }

      verify(exactly = 1) {
        cas3BedspaceRepositoryMock.save(
          match<Cas3BedspacesEntity> {
            it.id == archivedBedspace.id &&
              it.startDate == restartDate &&
              it.endDate == null
          },
        )

        cas3DomainEventServiceMock.saveBedspaceUnarchiveEvent(
          match<Cas3BedspacesEntity> {
            it.id == updatedBedspace.id
          },
          match { it == premises.id },
          currentStartDate,
          currentEndDate,
          any(),
        )
      }
    }
  }

  @Nested
  inner class ArchivePremises {
    @Test
    fun `When archive a premises returns Success with correct result when validation passed`() {
      val (premises, bedspaceOne) = createPremisesAndBedspace()
      val bedspaceTwo = createBedspace(premises, startDate = LocalDate.now().minusDays(90))
      val allBedspaces = listOf(bedspaceOne, bedspaceTwo)
      val archiveDate = LocalDate.now().plusDays(3)

      every { cas3DomainEventServiceMock.getPremisesActiveDomainEvents(premises.id, listOf(CAS3_PREMISES_ARCHIVED)) } returns emptyList()
      every { cas3BedspaceRepositoryMock.findByPremisesId(premises.id) } returns allBedspaces
      every { cas3v2BookingRepositoryMock.findActiveOverlappingBookingByPremisesId(premises.id, LocalDate.now()) } returns emptyList()
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDateByPremisesId(premises.id, archiveDate) } returns emptyList()
      every { cas3BedspaceRepositoryMock.save(match { it.id == bedspaceOne.id }) } returns bedspaceOne
      every { cas3BedspaceRepositoryMock.save(match { it.id == bedspaceTwo.id }) } returns bedspaceTwo
      every { cas3PremisesRepositoryMock.save(match { it.id == premises.id }) } returns premises
      every { cas3DomainEventServiceMock.savePremisesArchiveEvent(premises, archiveDate, any()) } returns Unit
      every {
        cas3DomainEventServiceMock.saveBedspaceArchiveEvent(
          match { it.id == bedspaceOne.id || it.id == bedspaceTwo.id },
          premises.id,
          null,
          any(),
        )
      } returns Unit

      val result = premisesService.archivePremises(premises, archiveDate)

      assertThatCasResult(result).isSuccess().with { updatedPremises ->
        assertThat(updatedPremises.id).isEqualTo(premises.id)
        assertThat(updatedPremises.startDate).isEqualTo(premises.startDate)
        assertThat(updatedPremises.endDate).isEqualTo(archiveDate)
        assertThat(updatedPremises.status).isEqualTo(Cas3PremisesStatus.archived)

        updatedPremises.bedspaces.forEach { bedspace ->
          val archivedBedspace = allBedspaces.firstOrNull { it.id == bedspace.id }
          assertThat(archivedBedspace).isNotNull
          assertThat(bedspace.id).isEqualTo(archivedBedspace?.id)
          assertThat(bedspace.startDate).isEqualTo(archivedBedspace?.startDate)
          assertThat(bedspace.endDate).isEqualTo(archiveDate)
        }
      }

      verify(exactly = 1) {
        cas3DomainEventServiceMock.savePremisesArchiveEvent(
          match<Cas3PremisesEntity> { it.id == premises.id },
          archiveDate,
          any(),
        )
      }

      verify(exactly = 2) {
        cas3DomainEventServiceMock.saveBedspaceArchiveEvent(
          match { it.id == bedspaceOne.id || it.id == bedspaceTwo.id },
          premises.id,
          null,
          any(),
        )
      }
    }

    @Test
    fun `When archive a premises with archived bedspaces returns Success with correct result`() {
      val (premises, bedspaceOne) = createPremisesAndBedspace()
      // archived bedspace
      val bedspaceTwo = createBedspace(premises, startDate = LocalDate.now().minusDays(40), endDate = LocalDate.now().minusDays(5))
      val bedspaceThree = createBedspace(premises, startDate = LocalDate.now().minusDays(90))
      val archiveDate = LocalDate.now().plusDays(3)

      every { cas3DomainEventServiceMock.getPremisesActiveDomainEvents(premises.id, listOf(CAS3_PREMISES_ARCHIVED)) } returns emptyList()
      every { cas3BedspaceRepositoryMock.findByPremisesId(premises.id) } returns listOf(bedspaceOne, bedspaceThree)
      every { cas3v2BookingRepositoryMock.findActiveOverlappingBookingByPremisesId(premises.id, LocalDate.now()) } returns emptyList()
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDateByPremisesId(premises.id, archiveDate) } returns emptyList()
      every { cas3BedspaceRepositoryMock.save(any()) } returns bedspaceOne
      every { cas3PremisesRepositoryMock.save(match { it.id == premises.id }) } returns premises
      every {
        cas3DomainEventServiceMock.saveBedspaceArchiveEvent(
          match { it.id == bedspaceOne.id || it.id == bedspaceTwo.id },
          premises.id,
          null,
          any(),
        )
      } returns Unit
      every { cas3DomainEventServiceMock.savePremisesArchiveEvent(premises, archiveDate, any()) } returns Unit

      val result = premisesService.archivePremises(premises, archiveDate)

      val expectedBedspaceOne = bedspaceOne.copy(
        endDate = archiveDate,
      )
      val expectedBedspaceThree = bedspaceThree.copy(
        endDate = archiveDate,
      )

      val allBedspaces = listOf(expectedBedspaceOne, bedspaceTwo, expectedBedspaceThree)

      assertThatCasResult(result).isSuccess().with { updatedPremises ->
        assertThat(updatedPremises.id).isEqualTo(premises.id)
        assertThat(updatedPremises.startDate).isEqualTo(premises.startDate)
        assertThat(updatedPremises.endDate).isEqualTo(archiveDate)
        assertThat(updatedPremises.status).isEqualTo(Cas3PremisesStatus.archived)

        updatedPremises.bedspaces.forEach { bedspace ->
          val archivedBedspace = allBedspaces.firstOrNull { it.id == bedspace.id }
          assertThat(archivedBedspace).isNotNull
          assertThat(bedspace.id).isEqualTo(archivedBedspace?.id)
          assertThat(bedspace.startDate).isEqualTo(archivedBedspace?.startDate)
          assertThat(bedspace.endDate).isEqualTo(archivedBedspace?.endDate)
        }
      }
    }

    @Test
    fun `When archive a premises with an end date before seven days returns FieldValidationError with the correct message`() {
      val (premises, _) = createPremisesAndBedspace()

      val result = premisesService.archivePremises(premises, LocalDate.now().minusDays(8))

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.endDate", "invalidEndDateInThePast")
    }

    @Test
    fun `When archive a premises with an end date in the future more than three months returns FieldValidationError with the correct message`() {
      val (premises, _) = createPremisesAndBedspace()

      val result = premisesService.archivePremises(premises, LocalDate.now().plusMonths(3).plusDays(1))

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.endDate", "invalidEndDateInTheFuture")
    }

    @Test
    fun `When archive a premises with an end date before premises start date returns FieldValidationError with the correct message`() {
      val premises = createPremisesEntity(startDate = LocalDate.now().minusDays(5))

      every { cas3DomainEventServiceMock.getPremisesActiveDomainEvents(premises.id, listOf(CAS3_PREMISES_ARCHIVED)) } returns emptyList()

      val result = premisesService.archivePremises(premises, premises.startDate.minusDays(1))

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", premises.id.toString(), "endDateBeforePremisesStartDate", premises.startDate.toString())
    }

    @Test
    fun `When archive a premises with an end date that clashes with previous premises archive date returns FieldValidationError with the correct message`() {
      val premises = createPremisesEntity(startDate = LocalDate.now().minusDays(5))
      val previousPremisesArchiveDate = LocalDate.now().minusDays(3)
      val dataPremisesDomainEvent = createPremisesArchiveEvent(premisesId = premises.id, userId = UUID.randomUUID(), endDate = previousPremisesArchiveDate)
      val premisesDomainEvent = createPremisesArchiveDomainEvent(dataPremisesDomainEvent)

      every {
        cas3DomainEventServiceMock.getPremisesActiveDomainEvents(premises.id, listOf(CAS3_PREMISES_ARCHIVED))
      } returns listOf(premisesDomainEvent)

      val result = premisesService.archivePremises(premises, previousPremisesArchiveDate.minusDays(2))

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", premises.id.toString(), "endDateOverlapPreviousPremisesArchiveEndDate", previousPremisesArchiveDate.toString())
    }

    @Test
    fun `When archive a premises with an upcoming bedspace returns Cas3FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(5)
      val bedspaceTwo = createBedspace(premises, startDate = archiveDate.plusDays(2))

      every { cas3DomainEventServiceMock.getPremisesActiveDomainEvents(premises.id, listOf(CAS3_PREMISES_ARCHIVED)) } returns emptyList()
      every { cas3DomainEventServiceMock.getBedspaceActiveDomainEvents(bedspaceTwo.id, listOf(CAS3_BEDSPACE_UNARCHIVED)) } returns emptyList()
      every { cas3BedspaceRepositoryMock.findByPremisesId(premises.id) } returns listOf(bedspace, bedspaceTwo)

      val result = premisesService.archivePremises(premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspaceTwo.id.toString(), "existingUpcomingBedspace", bedspaceTwo.startDate!!.plusDays(1).toString())
    }

    @Test
    fun `When archive a premises with a bedspace that has a void end date after premises archive date returns Cas3FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(3)
      val voidBedspace = createVoidBedspace(bedspace, archiveDate.minusDays(3), archiveDate.plusDays(1))

      every { cas3DomainEventServiceMock.getPremisesActiveDomainEvents(premises.id, listOf(CAS3_PREMISES_ARCHIVED)) } returns emptyList()
      every { cas3BedspaceRepositoryMock.findByPremisesId(premises.id) } returns listOf(bedspace)
      every { cas3v2BookingRepositoryMock.findActiveOverlappingBookingByPremisesId(premises.id, LocalDate.now()) } returns listOf()
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDateByPremisesId(premises.id, archiveDate) } returns listOf(voidBedspace)

      val result = premisesService.archivePremises(premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspace.id.toString(), "existingVoid", voidBedspace.endDate.plusDays(1).toString())
    }

    @Test
    fun `When archive a premises with multiple bedspaces that have void and booking end date after premises archive date returns Cas3FieldValidationError with the correct message`() {
      val (premises, bedspaceOne) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(5)
      val bedspaceTwo = createBedspace(premises, startDate = LocalDate.now().minusDays(180))
      val voidBedspace = createVoidBedspace(bedspaceOne, archiveDate.minusDays(3), archiveDate.plusDays(5))
      val booking = createBooking(premises, bedspaceTwo, archiveDate.minusDays(3), archiveDate.plusDays(1), Cas3BookingStatus.arrived)

      every { cas3DomainEventServiceMock.getPremisesActiveDomainEvents(premises.id, listOf(CAS3_PREMISES_ARCHIVED)) } returns emptyList()
      every { cas3BedspaceRepositoryMock.findByPremisesId(premises.id) } returns listOf(bedspaceOne, bedspaceTwo)
      every { cas3v2BookingRepositoryMock.findActiveOverlappingBookingByPremisesId(premises.id, LocalDate.now()) } returns listOf(booking)
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDateByPremisesId(premises.id, archiveDate) } returns listOf(voidBedspace)
      every { workingDayServiceMock.addWorkingDays(booking.departureDate, any()) } returns booking.departureDate

      val result = premisesService.archivePremises(premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspaceOne.id.toString(), "existingVoid", voidBedspace.endDate.plusDays(1).toString())
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "provisional",
        "confirmed",
        "arrived",
      ],
    )
    fun `Archive premises returns FieldValidationError if there a booking in provisional, confirmed or arrived status`(bookingStatus: Cas3BookingStatus) {
      val (premises, bedspace) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(7)
      val booking = createBooking(premises, bedspace, LocalDate.now().plusDays(3), LocalDate.now().plusDays(10), bookingStatus)

      every { cas3DomainEventServiceMock.getPremisesActiveDomainEvents(premises.id, listOf(CAS3_PREMISES_ARCHIVED)) } returns emptyList()
      every { cas3BedspaceRepositoryMock.findByPremisesId(premises.id) } returns listOf(bedspace)
      every { cas3v2BookingRepositoryMock.findActiveOverlappingBookingByPremisesId(premises.id, LocalDate.now()) } returns listOf(booking)
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDateByPremisesId(premises.id, archiveDate) } returns listOf()
      every { workingDayServiceMock.addWorkingDays(booking.departureDate, any()) } returns booking.departureDate

      val result = premisesService.archivePremises(premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspace.id.toString(), "existingBookings", booking.departureDate.plusDays(1).toString())
    }
  }

  @Nested
  inner class CancelScheduledUnarchiveBedspace {
    @Test
    fun `cancelScheduledUnarchiveBedspace returns Success when scheduled unarchive is cancelled`() {
      val premises = createPremisesEntity()
      val scheduledToUnarchiveBedspace = createBedspace(premises, startDate = LocalDate.now().plusDays(10))
      val originalStartDate = LocalDate.now().minusDays(5)
      val originalEndDate = LocalDate.now().minusDays(4)

      val updatedBedspace = scheduledToUnarchiveBedspace.copy(startDate = originalStartDate, endDate = originalEndDate)

      val bedspaceUnarchiveEvent = createBedspaceUnarchiveEvent(
        bedspaceId = scheduledToUnarchiveBedspace.id,
        premisesId = premises.id,
        userId = UUID.randomUUID(),
        currentStartDate = originalStartDate,
        currentEndDate = originalEndDate,
        newStartDate = scheduledToUnarchiveBedspace.startDate,
      )

      val bedspaceDomainEvent = createBedspaceUnarchiveDomainEvent(bedspaceUnarchiveEvent)

      every { cas3BedspaceRepositoryMock.findById(scheduledToUnarchiveBedspace.id) } returns Optional.of(scheduledToUnarchiveBedspace)
      every { cas3BedspaceRepositoryMock.save(match { it.id == scheduledToUnarchiveBedspace.id }) } returns updatedBedspace
      every { domainEventRepositoryMock.findLastCas3BedspaceActiveDomainEventByBedspaceIdAndType(scheduledToUnarchiveBedspace.id, CAS3_BEDSPACE_UNARCHIVED) } returns bedspaceDomainEvent

      val updatedBedspaceDomainEvent = bedspaceDomainEvent.copy(
        cas3CancelledAt = OffsetDateTime.now(),
      )
      every { domainEventRepositoryMock.save(match { it.id == bedspaceDomainEvent.id }) } returns updatedBedspaceDomainEvent

      val result = premisesService.cancelScheduledUnarchiveBedspace(scheduledToUnarchiveBedspace.id)

      assertThatCasResult(result).isSuccess().with { bed ->
        assertThat(bed.id).isEqualTo(scheduledToUnarchiveBedspace.id)
        assertThat(bed.startDate).isEqualTo(originalStartDate)
        assertThat(bed.endDate).isEqualTo(originalEndDate)
      }

      verify(exactly = 1) {
        cas3BedspaceRepositoryMock.save(
          match<Cas3BedspacesEntity> {
            it.id == scheduledToUnarchiveBedspace.id && it.startDate == originalStartDate && it.endDate == originalEndDate
          },
        )

        domainEventRepositoryMock.save(
          match<DomainEventEntity> {
            it.id == updatedBedspaceDomainEvent.id && it.cas3CancelledAt != null
          },
        )
      }
    }

    @Test
    fun `cancelScheduledUnarchiveBedspace returns FieldValidationError when bedspace unarchive event does not exist`() {
      val premises = createPremisesEntity()
      val scheduledToUnarchiveBedspace = createBedspace(premises, startDate = LocalDate.now().plusDays(10))

      every { domainEventRepositoryMock.findLastCas3BedspaceActiveDomainEventByBedspaceIdAndType(scheduledToUnarchiveBedspace.id, CAS3_BEDSPACE_UNARCHIVED) } returns null
      every { cas3BedspaceRepositoryMock.findById(scheduledToUnarchiveBedspace.id) } returns Optional.of(scheduledToUnarchiveBedspace)

      val result = premisesService.cancelScheduledUnarchiveBedspace(scheduledToUnarchiveBedspace.id)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bedspaceId", "bedspaceNotScheduledToUnarchive")
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.Cas3PremisesServiceTest#startDateProvider")
    fun `cancelScheduledUnarchiveBedspace returns FieldValidationError when bedspace startDate (already online)`(startDate: LocalDate) {
      val premises = createPremisesEntity()
      val scheduledToUnarchiveBedspace = createBedspace(premises, startDate = startDate)

      every { cas3BedspaceRepositoryMock.findById(scheduledToUnarchiveBedspace.id) } returns Optional.of(scheduledToUnarchiveBedspace)

      val result = premisesService.cancelScheduledUnarchiveBedspace(scheduledToUnarchiveBedspace.id)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bedspaceId", "bedspaceAlreadyOnline")
    }

    @Test
    fun `cancelScheduledUnarchiveBedspace returns FieldValidationError when bedspace does not exist`() {
      val (_, scheduledToUnarchiveBedspace) = createPremisesAndBedspace()

      every { cas3BedspaceRepositoryMock.findById(scheduledToUnarchiveBedspace.id) } returns Optional.empty()

      val result = premisesService.cancelScheduledUnarchiveBedspace(scheduledToUnarchiveBedspace.id)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bedspaceId", "doesNotExist")
    }
  }

  private fun createPremisesAndBedspace(): Pair<Cas3PremisesEntity, Cas3BedspacesEntity> {
    val premises = createPremisesEntity()
    val bedspace = createBedspace(premises)

    return Pair(premises, bedspace)
  }

  private fun createPremisesEntity(
    id: UUID = UUID.randomUUID(),
    startDate: LocalDate = LocalDate.now().minusDays(180),
    endDate: LocalDate? = null,
    status: Cas3PremisesStatus = Cas3PremisesStatus.online,
  ): Cas3PremisesEntity {
    val probationRegion = ProbationRegionEntityFactory()
      .withApArea(ApAreaEntityFactory().produce())
      .produce()

    val localAuthority = LocalAuthorityEntityFactory()
      .produce()

    val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    return Cas3PremisesEntityFactory()
      .withId(id)
      .withService(ServiceName.temporaryAccommodation.value)
      .withProbationDeliveryUnit(probationDeliveryUnit)
      .withLocalAuthorityArea(localAuthority)
      .withStartDate(startDate)
      .withEndDate(endDate)
      .withStatus(status)
      .produce()
  }

  private fun createBedspace(
    premises: Cas3PremisesEntity,
    reference: String = randomStringMultiCaseWithNumbers(10),
    startDate: LocalDate = LocalDate.now().minusDays(180),
    endDate: LocalDate? = null,
  ) = Cas3BedspaceEntityFactory()
    .withPremises(premises)
    .withReference(reference)
    .withCreatedDate(startDate)
    .withStartDate(startDate)
    .withEndDate(endDate)
    .produce()

  @SuppressWarnings("LongParameterList")
  private fun createBedspaceArchiveEvent(
    premisesId: UUID,
    bedspaceId: UUID,
    userId: UUID,
    currentEndDate: LocalDate?,
    endDate: LocalDate,
    transactionId: UUID = UUID.randomUUID(),
  ): CAS3BedspaceArchiveEvent {
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
    data.eventDetails.transactionId!!,
  )

  @SuppressWarnings("LongParameterList")
  private fun createDomainEvent(id: UUID, premisesId: UUID, bedspaceId: UUID? = null, occurredAt: OffsetDateTime, data: String, type: DomainEventType, transactionId: UUID) = DomainEventEntityFactory()
    .withId(id)
    .withCas3PremisesId(premisesId)
    .withCas3BedspaceId(bedspaceId)
    .withCas3TransactionId(transactionId)
    .withType(type)
    .withData(data)
    .withOccurredAt(occurredAt)
    .produce()

  private fun createVoidBedspace(bedspace: Cas3BedspacesEntity, startDate: LocalDate, endDate: LocalDate) = Cas3VoidBedspaceEntityFactory()
    .withStartDate(startDate)
    .withEndDate(endDate)
    .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
    .withBedspace(bedspace)
    .produce()

  private fun createBooking(premises: Cas3PremisesEntity, bedspace: Cas3BedspacesEntity, arrivalDate: LocalDate, departureDate: LocalDate, status: Cas3BookingStatus) = Cas3BookingEntityFactory()
    .withPremises(premises)
    .withBedspace(bedspace)
    .withArrivalDate(arrivalDate)
    .withDepartureDate(departureDate)
    .withStatus(status)
    .produce()

  private fun createBookingTurnaround(booking: Cas3BookingEntity, turnaroundDays: Int) = Cas3v2TurnaroundEntityFactory()
    .withBooking(booking)
    .withWorkingDayCount(turnaroundDays)
    .produce()

  private fun createPremisesArchiveEvent(premisesId: UUID, userId: UUID, endDate: LocalDate, transactionId: UUID = UUID.randomUUID()): CAS3PremisesArchiveEvent {
    val eventId = UUID.randomUUID()
    val occurredAt = OffsetDateTime.now()
    return CAS3PremisesArchiveEvent(
      id = eventId,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.premisesArchived,
      eventDetails = CAS3PremisesArchiveEventDetails(
        premisesId = premisesId,
        endDate = endDate,
        userId = userId,
        transactionId = transactionId,
      ),
    )
  }

  private fun createPremisesArchiveDomainEvent(data: CAS3PremisesArchiveEvent) = createDomainEvent(
    id = data.id,
    occurredAt = data.timestamp.atOffset(ZoneOffset.UTC),
    data = objectMapper.writeValueAsString(data),
    type = CAS3_PREMISES_ARCHIVED,
    premisesId = data.eventDetails.premisesId,
    transactionId = data.eventDetails.transactionId!!,
  )

  @SuppressWarnings("LongParameterList")
  private fun createBedspaceUnarchiveEvent(
    premisesId: UUID,
    bedspaceId: UUID,
    userId: UUID,
    newStartDate: LocalDate,
    currentStartDate: LocalDate,
    currentEndDate: LocalDate,
    transactionId: UUID = UUID.randomUUID(),
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
    data.eventDetails.transactionId!!,
  )
}
