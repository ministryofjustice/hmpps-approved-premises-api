package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.v2

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CostCentre
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3v2VoidBedspaceService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas3v2VoidBedspaceTest {
  @MockK
  lateinit var cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository

  @MockK
  lateinit var cas3VoidBedspaceReasonRepository: Cas3VoidBedspaceReasonRepository

  @MockK
  lateinit var cas3BookingService: Cas3v2BookingService

  @InjectMockKs
  lateinit var cas3v2VoidBedspaceService: Cas3v2VoidBedspaceService

  private val voidBedspaceReason = Cas3VoidBedspaceReasonEntityFactory().produce()

  @BeforeEach
  fun setup() {
    every { cas3BookingService.throwIfBookingDatesConflict(any(), any(), any(), any()) } just runs
    every { cas3BookingService.throwIfVoidBedspaceDatesConflict(any(), any(), any(), any()) } just runs
    every { cas3VoidBedspaceReasonRepository.findByIdOrNull(voidBedspaceReason.id) } returns voidBedspaceReason
    every { cas3VoidBedspacesRepository.save(any()) } answers { it.invocation.args[0] as Cas3VoidBedspaceEntity }
  }

  @Nested
  inner class CreateVoidBedspace {
    private val bedspaceStartDate = LocalDate.now().plusDays(2)
    private val bedspaceEndDate = LocalDate.now().plusDays(10)
    private val bedspace =
      Cas3BedspaceEntityFactory().withStartDate(bedspaceStartDate).withEndDate(bedspaceEndDate).produce()

    @Test
    fun `createVoidBedspace returns Success with correct result when validation passed`() {
      val voidBedspaceStartDate = bedspaceStartDate.plusDays(1)
      val voidBedspaceEndDate = bedspaceEndDate.minusDays(1)

      val result = cas3v2VoidBedspaceService.createVoidBedspace(
        voidBedspaceStartDate = voidBedspaceStartDate,
        voidBedspaceEndDate = voidBedspaceEndDate,
        reasonId = voidBedspaceReason.id,
        referenceNumber = "12345",
        notes = "notes",
        bedspace = bedspace,
        costCentre = Cas3CostCentre.HMPPS,
      )

      assertThatCasResult(result).isSuccess().with {
        assertAll({
          assertThat(it.reason).isEqualTo(voidBedspaceReason)
          assertThat(it.startDate).isEqualTo(voidBedspaceStartDate)
          assertThat(it.endDate).isEqualTo(voidBedspaceEndDate)
          assertThat(it.referenceNumber).isEqualTo("12345")
          assertThat(it.notes).isEqualTo("notes")
        })
      }
      verify(exactly = 1) {
        cas3BookingService.throwIfVoidBedspaceDatesConflict(
          voidBedspaceStartDate,
          voidBedspaceEndDate,
          null,
          bedspace.id,
        )
      }
      verify(exactly = 1) {
        cas3BookingService.throwIfBookingDatesConflict(
          voidBedspaceStartDate,
          voidBedspaceEndDate,
          null,
          bedspace.id,
        )
      }
      verify(exactly = 1) { cas3VoidBedspacesRepository.save(match { it.bedspace?.id == bedspace.id }) }
    }

    @Test
    fun `createVoidBedspace returns FieldValidationError when reason does not exist and end date is before start date`() {
      // with an invalid reason and a void bedspace end date that is before the start date
      val invalidReasonId = UUID.randomUUID()
      every { cas3VoidBedspaceReasonRepository.findByIdOrNull(invalidReasonId) } returns null

      val result = cas3v2VoidBedspaceService.createVoidBedspace(
        voidBedspaceStartDate = bedspaceEndDate.minusDays(1),
        voidBedspaceEndDate = bedspaceEndDate.minusDays(2),
        reasonId = invalidReasonId,
        referenceNumber = "12345",
        notes = "notes",
        bedspace = bedspace,
        costCentre = Cas3CostCentre.HMPPS,
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.reason", "doesNotExist")
        .hasMessage("$.endDate", "beforeStartDate")
    }

    @Test
    fun `createVoidBedspace returns FieldValidationError when void bedspace start date is after bedspace end date`() {
      // when void bedspace dates are after the bedspace end date
      val voidBedspaceStartDate = bedspaceEndDate.plusDays(1)
      val voidBedspaceEndDate = bedspaceEndDate.plusDays(2)

      val result = cas3v2VoidBedspaceService.createVoidBedspace(
        voidBedspaceStartDate = voidBedspaceStartDate,
        voidBedspaceEndDate = voidBedspaceEndDate,
        reasonId = voidBedspaceReason.id,
        referenceNumber = "12345",
        notes = "notes",
        bedspace = bedspace,
        costCentre = Cas3CostCentre.HMPPS,
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.startDate", "voidStartDateAfterBedspaceEndDate")
        .hasMessage("$.endDate", "voidEndDateAfterBedspaceEndDate")
    }

    @Test
    fun `createVoidBedspace returns FieldValidationError when void bedspace start date is before bedspace start date`() {
      // when void bedspace dates are before bedspace start date
      val voidBedspaceStartDate = bedspaceStartDate.minusDays(2)
      val voidBedspaceEndDate = bedspaceStartDate.minusDays(1)
      val result = cas3v2VoidBedspaceService.createVoidBedspace(
        voidBedspaceStartDate = voidBedspaceStartDate,
        voidBedspaceEndDate = voidBedspaceEndDate,
        reasonId = voidBedspaceReason.id,
        referenceNumber = "12345",
        notes = "notes",
        bedspace = bedspace,
        costCentre = Cas3CostCentre.HMPPS,
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.startDate", "voidStartDateBeforeBedspaceStartDate")
    }
  }

  @Nested
  inner class UpdateVoidBedspace {

    @BeforeEach
    fun setup() {
      every { cas3VoidBedspacesRepository.findVoidBedspace(any(), any(), any()) } returns voidBedspaceEntity
    }

    private val bedspaceStartDate = LocalDate.now()
    private val bedspaceEndDate = LocalDate.now().plusDays(100)
    private val voidBedspaceStartDate = bedspaceStartDate.plusDays(2)
    private val voidBedspaceEndDate = bedspaceEndDate.minusDays(1)
    private val bedspace = Cas3BedspaceEntityFactory()
      .withStartDate(bedspaceStartDate)
      .withEndDate(bedspaceEndDate)
      .produce()

    private val voidBedspaceEntity = Cas3VoidBedspaceEntityFactory()
      .withBedspace(bedspace)
      .withYieldedReason { voidBedspaceReason }
      .withStartDate(voidBedspaceStartDate)
      .withEndDate(voidBedspaceEndDate)
      .produceV2()

    @Test
    fun `updateVoidBedspace returns Success with correct result when validation passed`() {
      val result = cas3v2VoidBedspaceService.updateVoidBedspace(
        voidBedspaceEntity,
        voidBedspaceStartDate = voidBedspaceStartDate,
        voidBedspaceEndDate = voidBedspaceEndDate,
        reasonId = voidBedspaceReason.id,
        referenceNumber = "new ref number",
        notes = "some new notes",
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.premises).isNull()
        assertThat(it.id).isEqualTo(voidBedspaceEntity.id)
        assertThat(it.reason).isEqualTo(voidBedspaceReason)
        assertThat(it.startDate).isEqualTo(voidBedspaceStartDate)
        assertThat(it.endDate).isEqualTo(voidBedspaceEndDate)
        assertThat(it.referenceNumber).isEqualTo("new ref number")
        assertThat(it.notes).isEqualTo("some new notes")
      }

      verify(exactly = 1) {
        cas3BookingService.throwIfVoidBedspaceDatesConflict(
          voidBedspaceStartDate,
          voidBedspaceEndDate,
          null,
          bedspace.id,
        )
      }
      verify(exactly = 1) {
        cas3BookingService.throwIfBookingDatesConflict(
          voidBedspaceStartDate,
          voidBedspaceEndDate,
          null,
          bedspace.id,
        )
      }
      verify(exactly = 1) { cas3VoidBedspacesRepository.save(match { it.bedspace?.id == bedspace.id }) }
    }

    @Test
    fun `updateVoidBedspace returns FieldValidationError when reason does not exist and end date is before start date`() {
      every { cas3VoidBedspaceReasonRepository.findByIdOrNull(any()) } returns null

      val result = cas3v2VoidBedspaceService.updateVoidBedspace(
        voidBedspaceEntity,
        voidBedspaceStartDate = LocalDate.now().plusDays(10),
        voidBedspaceEndDate = LocalDate.now().plusDays(2),
        reasonId = voidBedspaceReason.id,
        referenceNumber = "new ref number",
        notes = "some new notes",
      )

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.endDate", "beforeStartDate")
        .hasMessage("$.reason", "doesNotExist")
    }

    @Test
    fun `updateVoidBedspace returns FieldValidationErrors when void start and end dates are before bedspace start date`() {
      val result = cas3v2VoidBedspaceService.updateVoidBedspace(
        voidBedspaceEntity,
        voidBedspaceStartDate = bedspaceStartDate.minusDays(1),
        voidBedspaceEndDate = bedspaceEndDate.plusDays(1),
        reasonId = voidBedspaceReason.id,
        referenceNumber = "new ref number",
        notes = "some new notes",
      )

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.startDate", "voidStartDateBeforeBedspaceStartDate")
        .hasMessage("$.endDate", "voidEndDateAfterBedspaceEndDate")
    }

    @Test
    fun `updateVoidBedspace returns FieldValidationErrors when trying to update a cancelled bedspace`() {
      voidBedspaceEntity.cancellationDate = OffsetDateTime.now()

      val result = cas3v2VoidBedspaceService.updateVoidBedspace(
        voidBedspaceEntity,
        voidBedspaceStartDate = bedspaceStartDate.minusDays(1),
        voidBedspaceEndDate = bedspaceEndDate.plusDays(1),
        reasonId = voidBedspaceReason.id,
        referenceNumber = "new ref number",
        notes = "some new notes",
      )

      assertThatCasResult(result).isGeneralValidationError("This Void Bedspace has been cancelled")
    }

    @Test
    fun `updateVoidBedspaces returns FieldValidationErrors when void start date is after bedspace end date`() {
      val result = cas3v2VoidBedspaceService.updateVoidBedspace(
        voidBedspaceEntity,
        voidBedspaceStartDate = bedspaceEndDate.plusDays(1),
        voidBedspaceEndDate = bedspaceEndDate.plusDays(10),
        reasonId = voidBedspaceReason.id,
        referenceNumber = "new ref number",
        notes = "some new notes",
      )

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.startDate", "voidStartDateAfterBedspaceEndDate")
        .hasMessage("$.endDate", "voidEndDateAfterBedspaceEndDate")
    }
  }

  @Nested
  inner class CancelVoidBedspace {
    @Test
    fun `Previously cancelled void bedspace returns General Validation Error`() {
      val voidBedspace = Cas3VoidBedspaceEntityFactory()
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .withCancellationDate(OffsetDateTime.now())
        .withCancellationNotes("cancelled notes")
        .produceV2()

      val result = cas3v2VoidBedspaceService.cancelVoidBedspace(voidBedspace, "some notes")
      assertThatCasResult(result).isGeneralValidationError("This Void Bedspace already has a cancellation set")
    }

    @Test
    fun `Cancelled void bedspace with no validation issues returns Success`() {
      val voidBedspace = Cas3VoidBedspaceEntityFactory()
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .produceV2()

      val result = cas3v2VoidBedspaceService.cancelVoidBedspace(voidBedspace, "some notes")
      verify { cas3VoidBedspacesRepository.save(any()) }
      assertThatCasResult(result).isSuccess().with {
        assertAll({
          assertThat(it.cancellationDate).isNotNull
          assertThat(it.cancellationNotes).isEqualTo("some notes")
        })
      }
    }
  }
}
