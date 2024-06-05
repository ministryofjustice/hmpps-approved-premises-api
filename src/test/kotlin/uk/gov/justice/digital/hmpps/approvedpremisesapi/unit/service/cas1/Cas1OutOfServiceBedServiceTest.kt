package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedCancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import java.time.LocalDate
import java.util.UUID

class Cas1OutOfServiceBedServiceTest {
  private val outOfServiceBedRepository = mockk<Cas1OutOfServiceBedRepository>()
  private val outOfServiceBedReasonRepository = mockk<Cas1OutOfServiceBedReasonRepository>()
  private val outOfServiceBedCancellationRepository = mockk<Cas1OutOfServiceBedCancellationRepository>()

  private val approvedPremisesFactory = ApprovedPremisesEntityFactory()
    .withDefaults()

  private val outOfServiceBedService = Cas1OutOfServiceBedService(
    outOfServiceBedRepository,
    outOfServiceBedReasonRepository,
    outOfServiceBedCancellationRepository,
  )

  @Nested
  inner class CreateOutOfServiceBeds {

    @Test
    fun `Returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
      val premisesEntity = approvedPremisesFactory.produce()

      val reasonId = UUID.randomUUID()

      every { outOfServiceBedReasonRepository.findByIdOrNull(reasonId) } returns null

      val result = outOfServiceBedService.createOutOfServiceBed(
        premises = premisesEntity,
        startDate = LocalDate.parse("2022-08-28"),
        endDate = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        referenceNumber = "12345",
        notes = "notes",
        bedId = UUID.randomUUID(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.endDate", "beforeStartDate"),
        entry("$.reason", "doesNotExist"),
      )
    }

    @Test
    fun `Returns Success with correct result when validation passed`() {
      val premisesEntity = approvedPremisesFactory.produce()

      val room = RoomEntityFactory()
        .withPremises(premisesEntity)
        .produce()

      val bed = BedEntityFactory()
        .withYieldedRoom { room }
        .produce()

      premisesEntity.rooms += room
      room.beds += bed

      val outOfServiceBedReason = Cas1OutOfServiceBedReasonEntityFactory()
        .produce()

      every { outOfServiceBedReasonRepository.findByIdOrNull(outOfServiceBedReason.id) } returns outOfServiceBedReason

      every { outOfServiceBedRepository.save(any()) } returnsArgument 0

      val result = outOfServiceBedService.createOutOfServiceBed(
        premises = premisesEntity,
        startDate = LocalDate.parse("2022-08-25"),
        endDate = LocalDate.parse("2022-08-28"),
        reasonId = outOfServiceBedReason.id,
        referenceNumber = "12345",
        notes = "notes",
        bedId = bed.id,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.premises).isEqualTo(premisesEntity)
      assertThat(result.entity.reason).isEqualTo(outOfServiceBedReason)
      assertThat(result.entity.startDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.endDate).isEqualTo(LocalDate.parse("2022-08-28"))
      assertThat(result.entity.referenceNumber).isEqualTo("12345")
      assertThat(result.entity.notes).isEqualTo("notes")
    }
  }

  @Nested
  inner class UpdateOutOfServiceBeds {
    @Test
    fun `Returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
      val premisesEntity = approvedPremisesFactory.produce()

      val reasonId = UUID.randomUUID()

      val outOfServiceBed = Cas1OutOfServiceBedEntityFactory()
        .withBed {
          withRoom {
            withPremises(premisesEntity)
          }
        }
        .produce()

      every { outOfServiceBedRepository.findByIdOrNull(outOfServiceBed.id) } returns outOfServiceBed
      every { outOfServiceBedReasonRepository.findByIdOrNull(reasonId) } returns null

      val result = outOfServiceBedService.updateOutOfServiceBed(
        outOfServiceBedId = outOfServiceBed.id,
        startDate = LocalDate.parse("2022-08-28"),
        endDate = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        referenceNumber = "12345",
        notes = "notes",
      )

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      val resultEntity = (result as AuthorisableActionResult.Success).entity
      assertThat(resultEntity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((resultEntity as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.endDate", "beforeStartDate"),
        entry("$.reason", "doesNotExist"),
      )
    }

    @Test
    fun `Returns Success with correct result when validation passed`() {
      val premisesEntity = approvedPremisesFactory.produce()

      val outOfServiceBedReason = Cas1OutOfServiceBedReasonEntityFactory()
        .produce()

      val outOfServiceBed = Cas1OutOfServiceBedEntityFactory()
        .withBed {
          withRoom {
            withPremises(premisesEntity)
          }
        }
        .produce()

      every { outOfServiceBedRepository.findByIdOrNull(outOfServiceBed.id) } returns outOfServiceBed
      every { outOfServiceBedReasonRepository.findByIdOrNull(outOfServiceBedReason.id) } returns outOfServiceBedReason

      every { outOfServiceBedRepository.save(any()) } returnsArgument 0

      val result = outOfServiceBedService.updateOutOfServiceBed(
        outOfServiceBedId = outOfServiceBed.id,
        startDate = LocalDate.parse("2022-08-25"),
        endDate = LocalDate.parse("2022-08-28"),
        reasonId = outOfServiceBedReason.id,
        referenceNumber = "12345",
        notes = "notes",
      )
      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      val resultEntity = (result as AuthorisableActionResult.Success).entity
      assertThat(resultEntity).isInstanceOf(ValidatableActionResult.Success::class.java)
      resultEntity as ValidatableActionResult.Success
      assertThat(resultEntity.entity.premises).isEqualTo(premisesEntity)
      assertThat(resultEntity.entity.reason).isEqualTo(outOfServiceBedReason)
      assertThat(resultEntity.entity.startDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(resultEntity.entity.endDate).isEqualTo(LocalDate.parse("2022-08-28"))
      assertThat(resultEntity.entity.referenceNumber).isEqualTo("12345")
      assertThat(resultEntity.entity.notes).isEqualTo("notes")
    }
  }

  @Nested
  inner class CancelOutOfServiceBed {
    @Test
    fun `Returns GeneralValidationError when the out-of-service bed already has a cancellation`() {
      val premisesEntity = approvedPremisesFactory.produce()

      val outOfServiceBed = Cas1OutOfServiceBedEntityFactory()
        .withBed {
          withRoom {
            withPremises(premisesEntity)
          }
        }
        .produce()
        .apply {
          cancellation = Cas1OutOfServiceBedCancellationEntityFactory()
            .withOutOfServiceBed(this)
            .produce()
        }

      val result = outOfServiceBedService.cancelOutOfServiceBed(outOfServiceBed, notes = null)

      assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
      result as ValidatableActionResult.GeneralValidationError
      assertThat(result.message).isEqualTo("This out-of-service bed has already been cancelled.")
    }

    @Test
    fun `Returns Success with correct result when validation passed`() {
      val premisesEntity = approvedPremisesFactory.produce()

      val outOfServiceBed = Cas1OutOfServiceBedEntityFactory()
        .withBed {
          withRoom {
            withPremises(premisesEntity)
          }
        }
        .produce()

      val notes = "Some notes"

      every { outOfServiceBedCancellationRepository.save(any()) } returnsArgument 0

      val result = outOfServiceBedService.cancelOutOfServiceBed(outOfServiceBed, notes)

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.outOfServiceBed).isEqualTo(outOfServiceBed)
      assertThat(result.entity.notes).isEqualTo(notes)
    }
  }

  @Nested
  inner class GetOutOfServiceBeds {
    @Test
    fun `Delegates to repository method`() {
      val expectedList = mockk<List<Cas1OutOfServiceBedEntity>>()
      every { outOfServiceBedRepository.findOutOfServiceBeds() } returns expectedList

      val result = outOfServiceBedService.getOutOfServiceBeds()

      assertThat(result).isEqualTo(expectedList)
      verify(exactly = 1) { outOfServiceBedRepository.findOutOfServiceBeds() }
    }
  }

  @Nested
  inner class GetActiveOutOfServiceBedsForPremisesId {
    @Test
    fun `Delegates to repository method`() {
      val expectedList = mockk<List<Cas1OutOfServiceBedEntity>>()
      every { outOfServiceBedRepository.findAllActiveForPremisesId(any()) } returns expectedList

      val premisesId = UUID.randomUUID()

      val result = outOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premisesId)

      assertThat(result).isEqualTo(expectedList)
      verify(exactly = 1) { outOfServiceBedRepository.findAllActiveForPremisesId(premisesId) }
    }
  }

  @Nested
  inner class GetOutOfServiceBedWithConflictingDates {
    @Test
    fun `Returns null when no overlapping out-of-service beds exist`() {
      val expectedList = listOf<Cas1OutOfServiceBedEntity>()
      every { outOfServiceBedRepository.findByBedIdAndOverlappingDate(any(), any(), any(), any()) } returns expectedList

      val id = UUID.randomUUID()
      val bedId = UUID.randomUUID()

      val result = outOfServiceBedService.getOutOfServiceBedWithConflictingDates(
        LocalDate.parse("2024-01-01"),
        LocalDate.parse("2024-02-01"),
        id,
        bedId,
      )

      assertThat(result).isEqualTo(null)
      verify(exactly = 1) {
        outOfServiceBedRepository.findByBedIdAndOverlappingDate(
          bedId,
          LocalDate.parse("2024-01-01"),
          LocalDate.parse("2024-02-01"),
          id,
        )
      }
    }

    @Test
    fun `Returns the first result when one or more out-of-service beds exist`() {
      val expectedList = listOf(
        Cas1OutOfServiceBedEntityFactory()
          .withBed {
            withRoom {
              withPremises(
                ApprovedPremisesEntityFactory()
                  .withDefaults()
                  .produce(),
              )
            }
          }
          .produce(),
      )
      every { outOfServiceBedRepository.findByBedIdAndOverlappingDate(any(), any(), any(), any()) } returns expectedList

      val id = UUID.randomUUID()
      val bedId = UUID.randomUUID()

      val result = outOfServiceBedService.getOutOfServiceBedWithConflictingDates(
        LocalDate.parse("2024-01-01"),
        LocalDate.parse("2024-02-01"),
        id,
        bedId,
      )

      assertThat(result).isEqualTo(expectedList[0])
      verify(exactly = 1) {
        outOfServiceBedRepository.findByBedIdAndOverlappingDate(
          bedId,
          LocalDate.parse("2024-01-01"),
          LocalDate.parse("2024-02-01"),
          id,
        )
      }
    }
  }
}
