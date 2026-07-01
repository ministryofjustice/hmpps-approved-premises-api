package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.migration

import com.ninjasquad.springmockk.MockkSpyBean
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity

class MigrateBookingStatusTest : MigrationJobTestBase() {
  @MockkSpyBean
  lateinit var realBookingRepository: Cas3v2BookingRepository

  @Test
  fun `Should not migrate CAS3 booking with existing status 'arrived' with existing departure entity and returns 202 response`() {
    givenAUser { userEntity, _ ->
      givenAnOffender { _, _ ->
        val booking = createCas3Booking(userEntity, Cas3BookingStatus.arrived)
        booking.departures = cas3DepartureEntityFactory.produceAndPersistMultiple(1) {
          withBooking(booking)
          withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
          withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
          withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
        }.toMutableList()

        assertBookingStatus(booking, Cas3BookingStatus.arrived, ServiceName.temporaryAccommodation)

        migrationJobService.runMigrationJob(MigrationJobType.updateBookingStatus, 1)

        assertBookingStatus(booking, Cas3BookingStatus.arrived, ServiceName.temporaryAccommodation)
      }
    }
  }

  @Test
  fun `Successfully migrate CAS3 cancelled booking with 'cancelled' status and returns 202 response`() {
    givenAUser { userEntity, _ ->
      givenAnOffender { _, _ ->
        val booking = createCAS3CancelledBooking(userEntity)

        assertBookingStatusIsNull(booking, ServiceName.temporaryAccommodation)

        migrationJobService.runMigrationJob(MigrationJobType.updateBookingStatus, 1)

        assertBookingStatus(booking, Cas3BookingStatus.cancelled, ServiceName.temporaryAccommodation)
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

        migrationJobService.runMigrationJob(MigrationJobType.updateBookingStatus, 1)

        assertBookingStatus(booking1, Cas3BookingStatus.cancelled, ServiceName.temporaryAccommodation)
        assertBookingStatus(booking2, Cas3BookingStatus.cancelled, ServiceName.temporaryAccommodation)
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

        migrationJobService.runMigrationJob(MigrationJobType.updateBookingStatus, 1)

        assertBookingStatus(booking1, Cas3BookingStatus.departed, ServiceName.temporaryAccommodation)
        assertBookingStatus(booking2, Cas3BookingStatus.departed, ServiceName.temporaryAccommodation)
      }
    }
  }

  @Test
  fun `Successfully migrate multiple CAS3 provisional booking and returns 202 response`() {
    givenAUser { userEntity, _ ->
      givenAnOffender { _, _ ->
        val booking1 = createCas3Booking(userEntity, null)
        val booking2 = createCas3Booking(userEntity, null)

        assertBookingStatusIsNull(booking1, ServiceName.temporaryAccommodation)
        assertBookingStatusIsNull(booking2, ServiceName.temporaryAccommodation)

        migrationJobService.runMigrationJob(MigrationJobType.updateBookingStatus, 1)

        assertBookingStatus(booking1, Cas3BookingStatus.provisional, ServiceName.temporaryAccommodation)
        assertBookingStatus(booking2, Cas3BookingStatus.provisional, ServiceName.temporaryAccommodation)
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

        migrationJobService.runMigrationJob(MigrationJobType.updateBookingStatus, 1)

        assertBookingStatus(booking1, Cas3BookingStatus.arrived, ServiceName.temporaryAccommodation)
        assertBookingStatus(booking2, Cas3BookingStatus.arrived, ServiceName.temporaryAccommodation)
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

        migrationJobService.runMigrationJob(MigrationJobType.updateBookingStatus, 1)

        assertBookingStatus(booking1, Cas3BookingStatus.confirmed, ServiceName.temporaryAccommodation)
        assertBookingStatus(booking2, Cas3BookingStatus.confirmed, ServiceName.temporaryAccommodation)
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
        val provisionalBooking = createCas3Booking(userEntity, null)

        assertBookingStatusIsNull(departedBooking1, ServiceName.temporaryAccommodation)
        assertBookingStatusIsNull(departedBooking2, ServiceName.temporaryAccommodation)
        assertBookingStatusIsNull(arrivedBooking, ServiceName.temporaryAccommodation)
        assertBookingStatusIsNull(confirmedBooking, ServiceName.temporaryAccommodation)
        assertBookingStatusIsNull(provisionalBooking, ServiceName.temporaryAccommodation)

        migrationJobService.runMigrationJob(MigrationJobType.updateBookingStatus, 1)

        assertBookingStatus(departedBooking1, Cas3BookingStatus.departed, ServiceName.temporaryAccommodation)
        assertBookingStatus(departedBooking2, Cas3BookingStatus.departed, ServiceName.temporaryAccommodation)
        assertBookingStatus(arrivedBooking, Cas3BookingStatus.arrived, ServiceName.temporaryAccommodation)
        assertBookingStatus(confirmedBooking, Cas3BookingStatus.confirmed, ServiceName.temporaryAccommodation)
        assertBookingStatus(provisionalBooking, Cas3BookingStatus.provisional, ServiceName.temporaryAccommodation)
      }
    }
  }

  @Test
  fun `Successfully migrate CAS3 bookings when multiple different booking exists in DB`() {
    givenAUser { userEntity, _ ->
      givenAnOffender { _, _ ->
        val cancelledBooking = createCAS3CancelledBooking(userEntity)
        val provisionalBooking = createCas3Booking(userEntity, null)

        assertBookingStatusIsNull(cancelledBooking, ServiceName.temporaryAccommodation)
        assertBookingStatusIsNull(provisionalBooking, ServiceName.temporaryAccommodation)

        migrationJobService.runMigrationJob(MigrationJobType.updateBookingStatus, 1)

        assertBookingStatus(provisionalBooking, Cas3BookingStatus.provisional, ServiceName.temporaryAccommodation)
        assertBookingStatus(cancelledBooking, Cas3BookingStatus.cancelled, ServiceName.temporaryAccommodation)
      }
    }
  }

  private fun assertBookingStatus(
    givenBooking: Cas3BookingEntity,
    expectedStatus: Cas3BookingStatus,
    serviceName: ServiceName,
  ) {
    val booking = realBookingRepository.findById(givenBooking.id)
    Assertions.assertThat(booking).isNotNull()
    Assertions.assertThat(booking.get().status).isEqualTo(expectedStatus)
    Assertions.assertThat(booking.get().service).isEqualTo(serviceName.value)
  }

  private fun assertBookingStatusIsNull(
    booking: Cas3BookingEntity,
    serviceName: ServiceName,
  ) {
    val updatedBooking = realBookingRepository.findById(booking.id)
    Assertions.assertThat(updatedBooking).isNotNull()
    Assertions.assertThat(updatedBooking.get().status).isNull()
    Assertions.assertThat(updatedBooking.get().service).isEqualTo(serviceName.value)
  }

  private fun createCAS3DepartedBooking(userEntity: UserEntity): Cas3BookingEntity {
    val booking = createCas3Booking(userEntity, null)
    booking.departures = cas3DepartureEntityFactory.produceAndPersistMultiple(1) {
      withBooking(booking)
      withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
      withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
      withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
    }.toMutableList()

    return booking
  }

  private fun createCAS3CancelledBooking(userEntity: UserEntity): Cas3BookingEntity {
    val booking = createCas3Booking(userEntity, null)
    booking.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
      withBooking(booking)
      withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
    }.toMutableList()

    return booking
  }

  private fun createCAS3ArrivedBooking(userEntity: UserEntity): Cas3BookingEntity {
    val booking = createCas3Booking(userEntity, null)
    booking.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) {
      withBooking(booking)
    }.toMutableList()

    return booking
  }

  private fun createCAS3ConfirmedBooking(userEntity: UserEntity): Cas3BookingEntity {
    val booking = createCas3Booking(userEntity, null)
    booking.confirmation = cas3v2ConfirmationEntityFactory.produceAndPersist {
      withBooking(booking)
    }
    return booking
  }

  private fun createCas3Booking(userEntity: UserEntity, bookingStatus: Cas3BookingStatus?): Cas3BookingEntity {
    val premises = cas3PremisesEntityFactory.produceAndPersist {
      withProbationDeliveryUnit(
        probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(userEntity.probationRegion)
        },
      )
      withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
    }
    val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
      withPremises(premises)
    }
    return cas3BookingEntityFactory.produceAndPersist {
      withPremises(premises)
      withBedspace(bedspace)
      withCrn("X320741")
      withServiceName(ServiceName.temporaryAccommodation)
      bookingStatus?.let { withStatus(bookingStatus) }
    }
  }
}
