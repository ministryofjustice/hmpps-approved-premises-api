package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RoomService

class RoomServiceTest {
  private val roomRepository = mockk<RoomRepository>()
  private val bedRepository = mockk<BedRepository>()
  private val bookingRepository = mockk<BookingRepository>()
  private val lostBedsRepository = mockk<LostBedsRepository>()
  private val characteristicService = mockk<CharacteristicService>()

  private val roomService = RoomService(roomRepository, bedRepository, bookingRepository, lostBedsRepository, characteristicService)

  @Test
  fun `An empty room name results in a validation error`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val result = roomService.createRoom(premises, "", "test-notes", mutableListOf())

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.name", "empty"),
    )
  }

  @Test
  fun `An empty bed name results in a validation error`() {
    val room = RoomEntityFactory()
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .produce()

    val result = roomService.createBed(room, "")

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.name", "empty"),
    )
  }

  @Test
  fun `An Approved Premises does not automatically create a bed for the new room`() {
    val premises = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    every { roomRepository.save(any()) } answers { it.invocation.args[0] as RoomEntity }

    val result = roomService.createRoom(premises, "test-room", "test-notes", mutableListOf())

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.premises).isEqualTo(premises)
    assertThat(result.entity.name).isEqualTo("test-room")
    assertThat(result.entity.notes).isEqualTo("test-notes")
    assertThat(result.entity.beds).isEmpty()
  }

  @Test
  fun `A Temporary Accommodation Premises automatically creates a bed for the new room`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    every { roomRepository.save(any()) } answers { it.invocation.args[0] as RoomEntity }
    every { bedRepository.save(any()) } answers { it.invocation.args[0] as BedEntity }

    val result = roomService.createRoom(premises, "test-room", "test-notes", mutableListOf())

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.premises).isEqualTo(premises)
    assertThat(result.entity.name).isEqualTo("test-room")
    assertThat(result.entity.notes).isEqualTo("test-notes")
    assertThat(result.entity.beds).hasSize(1)
    assertThat(result.entity.beds[0].room).isEqualTo(result.entity)
    assertThat(result.entity.beds[0].name).isEqualTo("default-bed")
  }
}
