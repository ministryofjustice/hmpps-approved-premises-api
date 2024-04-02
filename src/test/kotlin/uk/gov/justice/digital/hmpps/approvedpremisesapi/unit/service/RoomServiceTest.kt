package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RoomService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

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

    val result = roomService.createRoom(premises, "", "test-notes", mutableListOf(), null)

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

    val result = roomService.createRoom(premises, "test-room", "test-notes", mutableListOf(), null)

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

    val result = roomService.createRoom(premises, "test-room", "test-notes", mutableListOf(), null)

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.premises).isEqualTo(premises)
    assertThat(result.entity.name).isEqualTo("test-room")
    assertThat(result.entity.notes).isEqualTo("test-notes")
    assertThat(result.entity.beds).hasSize(1)
    assertThat(result.entity.beds[0].room).isEqualTo(result.entity)
    assertThat(result.entity.beds[0].name).isEqualTo("default-bed")
  }

  @Test
  fun `A Temporary Accommodation Premises automatically creates a bed for the new room with bedspace end date`() {
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

    val result = roomService.createRoom(premises, "test-room", "test-notes", mutableListOf(), LocalDate.now())

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.premises).isEqualTo(premises)
    assertThat(result.entity.name).isEqualTo("test-room")
    assertThat(result.entity.notes).isEqualTo("test-notes")
    assertThat(result.entity.beds).hasSize(1)
    assertThat(result.entity.beds[0].room).isEqualTo(result.entity)
    assertThat(result.entity.beds[0].name).isEqualTo("default-bed")
    assertThat(result.entity.beds[0].endDate).isEqualTo(LocalDate.now())
  }

  @Test
  fun `renameRoom returns NotFound if the room does not exist`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    every { roomRepository.findByIdOrNull(any()) } returns null

    val result = roomService.renameRoom(premises, UUID.randomUUID(), "unknown-room")

    assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
  }

  @Test
  fun `renameRoom returns NotFound if the room does not belong to the given premises`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val anotherPremises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val room = RoomEntityFactory()
      .withPremises(anotherPremises)
      .produce()

    every { roomRepository.findByIdOrNull(any()) } returns room

    val result = roomService.renameRoom(premises, room.id, "wrong-premises-room")

    assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
  }

  @Test
  fun `renameRoom returns FieldValidationError if the new name is not unique for the premises`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    every { roomRepository.findByIdOrNull(any()) } returns room
    every { roomRepository.nameIsUniqueForPremises(any(), any()) } returns false

    val result = roomService.renameRoom(premises, room.id, "non-unique-name-room")

    assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
    result as AuthorisableActionResult.Success
    assertThat(result.entity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    val resultEntity = result.entity as ValidatableActionResult.FieldValidationError
    assertThat(resultEntity.validationMessages).contains(
      entry("$.name", "notUnique"),
    )
  }

  @Test
  fun `renameRoom returns Success containing updated room otherwise`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    every { roomRepository.findByIdOrNull(any()) } returns room
    every { roomRepository.nameIsUniqueForPremises(any(), any()) } returns true
    every { roomRepository.save(any()) } returnsArgument 0

    val result = roomService.renameRoom(premises, room.id, "renamed-room")

    assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
    result as AuthorisableActionResult.Success
    assertThat(result.entity).isInstanceOf(ValidatableActionResult.Success::class.java)
    val resultEntity = result.entity as ValidatableActionResult.Success
    assertThat(resultEntity.entity).matches {
      it.id == room.id &&
        it.name == "renamed-room"
    }

    verify(exactly = 1) {
      roomRepository.save(
        match {
          it.id == room.id &&
            it.name == "renamed-room"
        },
      )
    }
  }

  @Test
  fun `updateBedEndDate returns Success containing updated bed end-date`() {
    val bedEndDate = LocalDate.now()
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    val bed = BedEntityFactory().withRoom(room).produce()
    room.beds.add(bed)

    every { roomRepository.findByIdOrNull(any()) } returns room
    every { bookingRepository.findActiveOverlappingBookingByBed(any(), any()) } returns emptyList()
    every { bedRepository.save(any()) } returnsArgument 0

    val result = roomService.updateBedEndDate(premises, room.id, bedEndDate)

    assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
    result as AuthorisableActionResult.Success
    assertThat(result.entity).isInstanceOf(ValidatableActionResult.Success::class.java)
    val resultEntity = result.entity as ValidatableActionResult.Success
    assertThat(resultEntity.entity).matches {
      it.id == room.id &&
        it.beds.first().endDate == bedEndDate
    }

    verify(exactly = 1) {
      roomRepository.findByIdOrNull(any())
    }

    verify(exactly = 1) {
      bookingRepository.findActiveOverlappingBookingByBed(any(), any())
    }

    verify(exactly = 1) {
      bedRepository.save(
        match {
          it.endDate == bedEndDate
        },
      )
    }
  }

  @Test
  fun `updateBedEndDate returns Success containing updated bed end-date when start date is null`() {
    val bedEndDate = LocalDate.now()
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    val bed = BedEntityFactory()
      .withRoom(room)
      .withCreatedAt(null)
      .produce()

    room.beds.add(bed)

    every { roomRepository.findByIdOrNull(any()) } returns room
    every { bookingRepository.findActiveOverlappingBookingByBed(any(), any()) } returns emptyList()
    every { bedRepository.save(any()) } returnsArgument 0

    val result = roomService.updateBedEndDate(premises, room.id, bedEndDate)

    assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
    result as AuthorisableActionResult.Success
    assertThat(result.entity).isInstanceOf(ValidatableActionResult.Success::class.java)
    val resultEntity = result.entity as ValidatableActionResult.Success
    assertThat(resultEntity.entity).matches {
      it.id == room.id &&
        it.beds.first().endDate == bedEndDate
    }

    verify(exactly = 1) {
      roomRepository.findByIdOrNull(any())
    }

    verify(exactly = 1) {
      bookingRepository.findActiveOverlappingBookingByBed(any(), any())
    }

    verify(exactly = 1) {
      bedRepository.save(
        match {
          it.createdAt == null
          it.endDate == bedEndDate
        },
      )
    }
  }

  @Test
  fun `updateBedEndDate returns NotFound error when given rooms is not exists`() {
    val bedEndDate = LocalDate.now()
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    val bed = BedEntityFactory().withRoom(room).produce()
    room.beds.add(bed)

    every { roomRepository.findByIdOrNull(any()) } returns null

    val result = roomService.updateBedEndDate(premises, room.id, bedEndDate)

    assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
    verify(exactly = 1) {
      roomRepository.findByIdOrNull(any())
    }
    verify(exactly = 0) {
      bedRepository.save(any())
    }
  }

  @Test
  fun `updateBedEndDate returns NotFound error when given rooms is not exists in given premises`() {
    val bedEndDate = LocalDate.now()
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val anotherPremises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val room = RoomEntityFactory()
      .withPremises(anotherPremises)
      .produce()

    val bed = BedEntityFactory().withRoom(room).produce()
    room.beds.add(bed)

    every { roomRepository.findByIdOrNull(any()) } returns room

    val result = roomService.updateBedEndDate(premises, room.id, bedEndDate)

    assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
    verify(exactly = 1) {
      roomRepository.findByIdOrNull(any())
    }
    verify(exactly = 0) {
      bedRepository.save(any())
    }
  }

  @Test
  fun `updateBedEndDate returns NotFound error when given rooms don't have bed`() {
    val bedEndDate = LocalDate.now()
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    every { roomRepository.findByIdOrNull(any()) } returns room

    val result = roomService.updateBedEndDate(premises, room.id, bedEndDate)

    assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
    verify(exactly = 1) {
      roomRepository.findByIdOrNull(any())
    }
    verify(exactly = 0) {
      bedRepository.save(any())
    }
  }

  @Test
  fun `updateBedEndDate returns bedEndDateCantBeModified error when given rooms has already got end-date`() {
    val bedEndDate = LocalDate.now()
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()
    val bed = BedEntityFactory()
      .withRoom(room)
      .withEndDate { LocalDate.now() }
      .produce()
    room.beds.add(bed)

    every { roomRepository.findByIdOrNull(any()) } returns room
    every { bookingRepository.findActiveOverlappingBookingByBed(any(), any()) } returns emptyList()

    val result = roomService.updateBedEndDate(premises, room.id, bedEndDate)

    result as AuthorisableActionResult.Success
    assertThat(result.entity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    val resultEntity = result.entity as ValidatableActionResult.FieldValidationError
    assertThat(resultEntity.validationMessages).contains(
      entry("$.roomId", "bedEndDateCantBeModified"),
    )

    verify(exactly = 1) {
      roomRepository.findByIdOrNull(any())
    }
    verify(exactly = 1) {
      bookingRepository.findActiveOverlappingBookingByBed(any(), any())
    }
    verify(exactly = 0) {
      bedRepository.save(any())
    }
  }

  @Test
  fun `updateBedEndDate returns afterBedspaceEndDate error when given rooms creation date is before end-date`() {
    val bedEndDate = LocalDate.now()
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()
    val bed = BedEntityFactory()
      .withRoom(room)
      .withCreatedAt { OffsetDateTime.now().plusDays(2) }
      .produce()
    room.beds.add(bed)

    every { roomRepository.findByIdOrNull(any()) } returns room
    every { bookingRepository.findActiveOverlappingBookingByBed(any(), any()) } returns emptyList()

    val result = roomService.updateBedEndDate(premises, room.id, bedEndDate)

    result as AuthorisableActionResult.Success
    assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
    val validationError = result.entity as ValidatableActionResult.GeneralValidationError
    assertThat(validationError.message).isEqualTo("Bedspace end date cannot be prior to the Bedspace creation date: ${bed.createdAt!!.toLocalDate()}")

    verify(exactly = 1) {
      roomRepository.findByIdOrNull(any())
    }
    verify(exactly = 0) {
      bookingRepository.findActiveOverlappingBookingByBed(any(), any())
    }
    verify(exactly = 0) {
      bedRepository.save(any())
    }
  }

  @Test
  fun `updateBedEndDate throws Runtime exception when save bedEntity failed with DB exception`() {
    val bedEndDate = LocalDate.now()
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()
    val bed = BedEntityFactory()
      .withRoom(room)
      .produce()
    room.beds.add(bed)

    every { roomRepository.findByIdOrNull(any()) } returns room
    every { bookingRepository.findActiveOverlappingBookingByBed(any(), any()) } returns emptyList()
    every { bedRepository.save(any()) } throws RuntimeException("DB Exception")

    assertThrows<RuntimeException> { roomService.updateBedEndDate(premises, room.id, bedEndDate) }

    verify(exactly = 1) {
      roomRepository.findByIdOrNull(any())
    }
    verify(exactly = 1) {
      bookingRepository.findActiveOverlappingBookingByBed(any(), any())
    }
    verify(exactly = 1) {
      bedRepository.save(any())
    }
  }

  @Test
  fun `updateBedEndDate returns bookingExistsForBedEndDate error when given rooms has got overlapping bookings`() {
    val bedEndDate = LocalDate.now()
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val booking = BookingEntityFactory()
      .withPremises(premises)
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()
    val bed = BedEntityFactory()
      .withRoom(room)
      .produce()
    room.beds.add(bed)

    every { roomRepository.findByIdOrNull(any()) } returns room
    every { bookingRepository.findActiveOverlappingBookingByBed(any(), any()) } returns listOf(booking)

    val result = roomService.updateBedEndDate(premises, room.id, bedEndDate)

    result as AuthorisableActionResult.Success
    assertThat(result.entity is ValidatableActionResult.ConflictError).isTrue
    val validationError = result.entity as ValidatableActionResult.ConflictError
    assertThat(validationError.message).isEqualTo("Conflict booking exists for the room with end date $bedEndDate")
    assertThat(validationError.conflictingEntityId).isEqualTo(booking.id)
    verify(exactly = 1) {
      roomRepository.findByIdOrNull(any())
    }
    verify(exactly = 1) {
      bookingRepository.findActiveOverlappingBookingByBed(any(), any())
    }
    verify(exactly = 0) {
      bedRepository.save(any())
    }
  }
}
