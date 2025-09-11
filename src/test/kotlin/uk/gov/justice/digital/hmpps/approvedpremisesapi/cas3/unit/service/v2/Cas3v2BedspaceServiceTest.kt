package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.v2

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BedspacesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class Cas3v2BedspaceServiceTest {
  private val mockCharacteristicServiceMock = mockk<CharacteristicService>()
  private val mockCas3BedspacesRepository = mockk<Cas3BedspacesRepository>()
  private val mockCas3v2PremisesService = mockk<Cas3v2PremisesService>()

  private val cas3v2BedspacesService = Cas3v2BedspacesService(
    mockCharacteristicServiceMock,
    mockCas3BedspacesRepository,
    mockCas3v2PremisesService,
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
        startDate = bedspace.startDate!!,
        notes = null,
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.reference).isEqualTo(bedspace.reference)
        assertThat(it.notes).isEqualTo(bedspace.notes)
        assertThat(it.startDate).isEqualTo(bedspace.startDate)
        assertThat(it.endDate).isEqualTo(bedspace.endDate)
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
      every { mockCas3v2PremisesService.unarchivePremisesAndSaveDomainEvent(any(), any()) } returns Unit

      val result = cas3v2BedspacesService.createBedspace(
        premises,
        bedspaceReference = bedspace.reference,
        startDate = bedspace.startDate!!,
        notes = null,
        characteristicIds = emptyList(),
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.reference).isEqualTo(bedspace.reference)
        assertThat(it.notes).isEqualTo(bedspace.notes)
        assertThat(it.startDate).isEqualTo(bedspace.startDate)
        assertThat(it.endDate).isEqualTo(bedspace.endDate)
      }

      verify { mockCas3v2PremisesService.unarchivePremisesAndSaveDomainEvent(eq(premises), eq(bedspaceStartDate)) }
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

      every { mockCharacteristicServiceMock.getCas3BedspaceCharacteristic(nonExistCharacteristicId) } returns null

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
}
