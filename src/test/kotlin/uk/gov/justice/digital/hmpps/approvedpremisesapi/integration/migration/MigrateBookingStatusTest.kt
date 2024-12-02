package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration

import com.ninjasquad.springmockk.SpykBean
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity

class MigrateBookingStatusTest : MigrationJobTestBase() {
  @SpykBean
  lateinit var realBookingRepository: BookingRepository

  @Test
  fun `Should not migrate CAS1 booking with 'cancelled' status and returns 202 response`() {
    givenAUser { userEntity, _ ->
      givenAnOffender { _, _ ->
        val approvedPremises = ServiceName.approvedPremises
        val booking = createCAS1CancelledBooking(userEntity)

        assertBookingStatusIsNull(booking, approvedPremises)

        migrationJobService.runMigrationJob(MigrationJobType.BOOKING_STATUS, 1)

        assertBookingStatusIsNull(booking, approvedPremises)
      }
    }
  }

  @Test
  fun `Should not migrate CAS3 booking with existing status 'arrived' with existing departure entity and returns 202 response`() {
    givenAUser { userEntity, _ ->
      givenAnOffender { _, _ ->
        val booking = createTemporaryAccommodationBooking(userEntity, BookingStatus.ARRIVED)
        booking.departures = departureEntityFactory.produceAndPersistMultiple(1) {
          withBooking(booking)
          withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
          withMoveOnCategory(moveOnCategoryEntityFactory.produceAndPersist())
        }.toMutableList()

        assertBookingStatus(booking, BookingStatus.ARRIVED, ServiceName.temporaryAccommodation)

        migrationJobService.runMigrationJob(MigrationJobType.BOOKING_STATUS, 1)

        assertBookingStatus(booking, BookingStatus.ARRIVED, ServiceName.temporaryAccommodation)
      }
    }
  }

  @Test
  fun `Successfully migrate CAS3 cancelled booking with 'cancelled' status and returns 202 response`() {
    givenAUser { userEntity, _ ->
      givenAnOffender { _, _ ->
        val booking = createCAS3CancelledBooking(userEntity)

        assertBookingStatusIsNull(booking, ServiceName.temporaryAccommodation)

        migrationJobService.runMigrationJob(MigrationJobType.BOOKING_STATUS, 1)

        assertBookingStatus(booking, BookingStatus.CANCELLED, ServiceName.temporaryAccommodation)
      }
    }
  }

  @Test
  fun `Successfully migrate multiple CAS3 cancelled booking with 'cancelled' status and returns 202 response`() {
    givenAUser { userEntity, _ ->
      givenAnOffender { _, _ ->
        val booking1 = createCAS3CancelledBooking(userEntity)
        val booking2 = createCAS3CancelledBooking(userEntity)

        assertBookingStatusIsNull(booking1, ServiceName.temporaryAccommodation)
        assertBookingStatusIsNull(booking2, ServiceName.temporaryAccommodation)

        migrationJobService.runMigrationJob(MigrationJobType.BOOKING_STATUS, 1)

        assertBookingStatus(booking1, BookingStatus.CANCELLED, ServiceName.temporaryAccommodation)
        assertBookingStatus(booking2, BookingStatus.CANCELLED, ServiceName.temporaryAccommodation)
      }
    }
  }

  @Test
  fun `Successfully migrate multiple CAS3 departure booking with 'departed' status and returns 202 response`() {
    givenAUser { userEntity, _ ->
      givenAnOffender { _, _ ->
        val booking1 = createCAS3DepartedBooking(userEntity)
        val booking2 = createCAS3DepartedBooking(userEntity)

        assertBookingStatusIsNull(booking1, ServiceName.temporaryAccommodation)
        assertBookingStatusIsNull(booking2, ServiceName.temporaryAccommodation)

        migrationJobService.runMigrationJob(MigrationJobType.BOOKING_STATUS, 1)

        assertBookingStatus(booking1, BookingStatus.DEPARTED, ServiceName.temporaryAccommodation)
        assertBookingStatus(booking2, BookingStatus.DEPARTED, ServiceName.temporaryAccommodation)
      }
    }
  }

  @Test
  fun `Successfully migrate multiple CAS3 provisional booking and returns 202 response`() {
    givenAUser { userEntity, _ ->
      givenAnOffender { _, _ ->
        val booking1 = createTemporaryAccommodationBooking(userEntity, null)
        val booking2 = createTemporaryAccommodationBooking(userEntity, null)

        assertBookingStatusIsNull(booking1, ServiceName.temporaryAccommodation)
        assertBookingStatusIsNull(booking2, ServiceName.temporaryAccommodation)

        migrationJobService.runMigrationJob(MigrationJobType.BOOKING_STATUS, 1)

        assertBookingStatus(booking1, BookingStatus.PROVISIONAL, ServiceName.temporaryAccommodation)
        assertBookingStatus(booking2, BookingStatus.PROVISIONAL, ServiceName.temporaryAccommodation)
      }
    }
  }

  @Test
  fun `Successfully migrate multiple CAS3 Arrived booking and returns 202 response`() {
    givenAUser { userEntity, _ ->
      givenAnOffender { _, _ ->
        val booking1 = createCAS3ArrivedBooking(userEntity)
        val booking2 = createCAS3ArrivedBooking(userEntity)

        assertBookingStatusIsNull(booking1, ServiceName.temporaryAccommodation)
        assertBookingStatusIsNull(booking2, ServiceName.temporaryAccommodation)

        migrationJobService.runMigrationJob(MigrationJobType.BOOKING_STATUS, 1)

        assertBookingStatus(booking1, BookingStatus.ARRIVED, ServiceName.temporaryAccommodation)
        assertBookingStatus(booking2, BookingStatus.ARRIVED, ServiceName.temporaryAccommodation)
      }
    }
  }

  @Test
  fun `Successfully migrate multiple CAS3 Confirmed booking and returns 202 response`() {
    givenAUser { userEntity, _ ->
      givenAnOffender { _, _ ->
        val booking1 = createCAS3ConfirmedBooking(userEntity)
        val booking2 = createCAS3ConfirmedBooking(userEntity)

        assertBookingStatusIsNull(booking1, ServiceName.temporaryAccommodation)
        assertBookingStatusIsNull(booking2, ServiceName.temporaryAccommodation)

        migrationJobService.runMigrationJob(MigrationJobType.BOOKING_STATUS, 1)

        assertBookingStatus(booking1, BookingStatus.CONFIRMED, ServiceName.temporaryAccommodation)
        assertBookingStatus(booking2, BookingStatus.CONFIRMED, ServiceName.temporaryAccommodation)
      }
    }
  }

  @Test
  fun `Successfully migrate CAS3 only bookings with multiple state exists in the DB`() {
    givenAUser { userEntity, _ ->
      givenAnOffender { _, _ ->
        val departedBooking1 = createCAS3DepartedBooking(userEntity)
        val departedBooking2 = createCAS3DepartedBooking(userEntity)
        val arrivedBooking = createCAS3ArrivedBooking(userEntity)
        val confirmedBooking = createCAS3ConfirmedBooking(userEntity)
        val provisionalBooking = createTemporaryAccommodationBooking(userEntity, null)

        assertBookingStatusIsNull(departedBooking1, ServiceName.temporaryAccommodation)
        assertBookingStatusIsNull(departedBooking2, ServiceName.temporaryAccommodation)
        assertBookingStatusIsNull(arrivedBooking, ServiceName.temporaryAccommodation)
        assertBookingStatusIsNull(confirmedBooking, ServiceName.temporaryAccommodation)
        assertBookingStatusIsNull(provisionalBooking, ServiceName.temporaryAccommodation)

        migrationJobService.runMigrationJob(MigrationJobType.BOOKING_STATUS, 1)

        assertBookingStatus(departedBooking1, BookingStatus.DEPARTED, ServiceName.temporaryAccommodation)
        assertBookingStatus(departedBooking2, BookingStatus.DEPARTED, ServiceName.temporaryAccommodation)
        assertBookingStatus(arrivedBooking, BookingStatus.ARRIVED, ServiceName.temporaryAccommodation)
        assertBookingStatus(confirmedBooking, BookingStatus.CONFIRMED, ServiceName.temporaryAccommodation)
        assertBookingStatus(provisionalBooking, BookingStatus.PROVISIONAL, ServiceName.temporaryAccommodation)
      }
    }
  }

  @Test
  fun `Successfully migrate CAS3 bookings when multiple different booking exists in DB`() {
    givenAUser { userEntity, _ ->
      givenAnOffender { _, _ ->
        val cancelledBooking = createCAS3CancelledBooking(userEntity)
        val provisionalBooking = createTemporaryAccommodationBooking(userEntity, null)

        assertBookingStatusIsNull(cancelledBooking, ServiceName.temporaryAccommodation)
        assertBookingStatusIsNull(provisionalBooking, ServiceName.temporaryAccommodation)

        migrationJobService.runMigrationJob(MigrationJobType.BOOKING_STATUS, 1)

        assertBookingStatus(provisionalBooking, BookingStatus.PROVISIONAL, ServiceName.temporaryAccommodation)
        assertBookingStatus(cancelledBooking, BookingStatus.CANCELLED, ServiceName.temporaryAccommodation)
      }
    }
  }

  private fun assertBookingStatus(
    givenBooking: BookingEntity,
    expectedStatus: BookingStatus,
    serviceName: ServiceName,
  ) {
    val booking = realBookingRepository.findById(givenBooking.id)
    assertThat(booking).isNotNull()
    assertThat(booking.get().status).isEqualTo(expectedStatus)
    assertThat(booking.get().service).isEqualTo(serviceName.value)
  }

  private fun assertBookingStatusIsNull(
    booking: BookingEntity,
    serviceName: ServiceName,
  ) {
    val updatedBooking = realBookingRepository.findById(booking.id)
    assertThat(updatedBooking).isNotNull()
    assertThat(updatedBooking.get().status).isNull()
    assertThat(updatedBooking.get().service).isEqualTo(serviceName.value)
  }

  private fun createCAS3DepartedBooking(userEntity: UserEntity): BookingEntity {
    val booking = createTemporaryAccommodationBooking(userEntity, null)
    booking.departures = departureEntityFactory.produceAndPersistMultiple(1) {
      withBooking(booking)
      withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
      withMoveOnCategory(moveOnCategoryEntityFactory.produceAndPersist())
    }.toMutableList()

    return booking
  }

  private fun createCAS3CancelledBooking(userEntity: UserEntity): BookingEntity {
    val booking = createTemporaryAccommodationBooking(userEntity, null)
    booking.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
      withBooking(booking)
      withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
    }.toMutableList()

    return booking
  }

  private fun createCAS3ArrivedBooking(userEntity: UserEntity): BookingEntity {
    val booking = createTemporaryAccommodationBooking(userEntity, null)
    booking.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) {
      withBooking(booking)
    }.toMutableList()

    return booking
  }

  private fun createCAS3ConfirmedBooking(userEntity: UserEntity): BookingEntity {
    val booking = createTemporaryAccommodationBooking(userEntity, null)
    booking.confirmation = confirmationEntityFactory.produceAndPersist {
      withBooking(booking)
    }

    return booking
  }

  private fun createCAS1CancelledBooking(userEntity: UserEntity): BookingEntity {
    val booking = createApprovedAccommodationBooking(userEntity)
    booking.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
      withBooking(booking)
      withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
    }.toMutableList()

    return booking
  }

  private fun createTemporaryAccommodationBooking(userEntity: UserEntity, bookingStatus: BookingStatus?): BookingEntity {
    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(userEntity.probationRegion)
      withYieldedLocalAuthorityArea {
        localAuthorityEntityFactory.produceAndPersist()
      }
    }

    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
    }
    val bed = bedEntityFactory.produceAndPersist {
      withRoom(room)
    }
    val booking = bookingEntityFactory.produceAndPersist {
      withPremises(bed.room.premises)
      withCrn("X320741")
      withBed(bed)
      withServiceName(ServiceName.temporaryAccommodation)
      bookingStatus?.let { withStatus(bookingStatus) }
    }
    return booking
  }

  private fun createApprovedAccommodationBooking(userEntity: UserEntity): BookingEntity {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(userEntity.probationRegion)
      withYieldedLocalAuthorityArea {
        localAuthorityEntityFactory.produceAndPersist()
      }
    }

    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
    }
    val bed = bedEntityFactory.produceAndPersist {
      withRoom(room)
    }
    val booking = bookingEntityFactory.produceAndPersist {
      withPremises(bed.room.premises)
      withCrn("X320741")
      withBed(bed)
      withServiceName(ServiceName.approvedPremises)
    }
    return booking
  }
}
