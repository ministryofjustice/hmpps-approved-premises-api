package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.v2

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3v2VoidBedspaceService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas3v2VoidBedspaceTest {
  @MockK
  lateinit var cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository

  @MockK
  lateinit var cas3VoidBedspaceReasonRepository: Cas3VoidBedspaceReasonRepository

  @InjectMockKs
  lateinit var cas3v2VoidBedspaceService: Cas3v2VoidBedspaceService

  lateinit var voidBedspaceReason: Cas3VoidBedspaceReasonEntity

  @BeforeEach
  fun setup() {
    voidBedspaceReason = Cas3VoidBedspaceReasonEntityFactory().produce()
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
    fun `createVoidBedspaces returns Success with correct result when validation passed`() {
      val voidBedspaceStartDate = bedspaceStartDate.plusDays(1)
      val voidBedspaceEndDate = bedspaceEndDate.minusDays(1)

      val result = cas3v2VoidBedspaceService.createVoidBedspace(
        startDate = voidBedspaceStartDate,
        endDate = voidBedspaceEndDate,
        reasonId = voidBedspaceReason.id,
        referenceNumber = "12345",
        notes = "notes",
        bedspace = bedspace,
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
      verify(exactly = 1) { cas3VoidBedspacesRepository.save(match { it.bedspace?.id == bedspace.id }) }
    }

    @Test
    fun `createVoidBedspaces returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
      // with an invalid reason and a void bedspace end date that is before the start date
      val invalidReasonId = UUID.randomUUID()
      every { cas3VoidBedspaceReasonRepository.findByIdOrNull(invalidReasonId) } returns null

      val result = cas3v2VoidBedspaceService.createVoidBedspace(
        startDate = bedspaceEndDate.minusDays(1),
        endDate = bedspaceEndDate.minusDays(2),
        reasonId = invalidReasonId,
        referenceNumber = "12345",
        notes = "notes",
        bedspace = bedspace,
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.reason", "doesNotExist")
        .hasMessage("$.endDate", "beforeStartDate")
    }

    @Test
    fun `createVoidBedspaces returns error when void start date is after bed end date`() {
      // when void bedspace dates are after the bedspace end date
      val voidBedspaceStartDate = bedspaceEndDate.plusDays(1)
      val voidBedspaceEndDate = bedspaceEndDate.plusDays(2)

      val result = cas3v2VoidBedspaceService.createVoidBedspace(
        startDate = voidBedspaceStartDate,
        endDate = voidBedspaceEndDate,
        reasonId = voidBedspaceReason.id,
        referenceNumber = "12345",
        notes = "notes",
        bedspace = bedspace,
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.startDate", "voidStartDateAfterBedspaceEndDate")
        .hasMessage("$.endDate", "voidEndDateAfterBedspaceEndDate")
    }

    @Test
    fun `createVoidBedspaces returns validation error when void start date is before bedspace start date`() {
      // when void bedspace dates are before bedspace start date
      val voidBedspaceStartDate = bedspaceStartDate.minusDays(2)
      val voidBedspaceEndDate = bedspaceStartDate.minusDays(1)
      val result = cas3v2VoidBedspaceService.createVoidBedspace(
        startDate = voidBedspaceStartDate,
        endDate = voidBedspaceEndDate,
        reasonId = voidBedspaceReason.id,
        referenceNumber = "12345",
        notes = "notes",
        bedspace = bedspace,
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.startDate", "voidStartDateBeforeBedspaceStartDate")
    }
  }
}
