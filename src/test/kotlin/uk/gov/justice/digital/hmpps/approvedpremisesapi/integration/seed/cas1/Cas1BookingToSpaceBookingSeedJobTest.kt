package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Booking`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Booking for an Offline Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS1 Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS1 Space Booking`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Request`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Approved Premises`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offline Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulGetReferralDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas1SpaceBookingTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1BookingToSpaceBookingSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class Cas1BookingToSpaceBookingSeedJobTest : SeedTestBase() {

  @Autowired
  lateinit var cas1SpaceBookingTestRepository: Cas1SpaceBookingTestRepository

  @Autowired
  lateinit var cas1BookingDomainEventSet: Cas1BookingDomainEventService

  @SuppressWarnings("LongMethod")
  @Test
  fun `Migrate bookings, removing existing`() {
    val premises = `Given an Approved Premises`("Premises 1")
    val otherUser = `Given a User`().first
    val roomCriteria1 = characteristicEntityFactory.produceAndPersist { withModelScope("*") }
    val roomCriteria2 = characteristicEntityFactory.produceAndPersist { withModelScope("room") }
    val premisesCriteria = characteristicEntityFactory.produceAndPersist { withModelScope("premises") }

    val application1 = `Given a CAS1 Application`(createdByUser = otherUser, eventNumber = "25")
    val booking1 = `Given a Booking`(
      crn = "CRN1",
      premises = premises,
      application = application1,
      arrivalDate = LocalDate.of(2024, 5, 1),
      departureDate = LocalDate.of(2024, 5, 5),
    )
    val placementRequest1 = `Given a Placement Request`(
      application = application1,
      placementRequestAllocatedTo = otherUser,
      assessmentAllocatedTo = otherUser,
      createdByUser = otherUser,
      booking = booking1,
      essentialCriteria = listOf(premisesCriteria, roomCriteria1, roomCriteria2),
    ).first
    val (booking1CreatedByUser) = `Given a User`()
    cas1BookingDomainEventSet.bookingMade(
      application1,
      booking1,
      booking1CreatedByUser,
      placementRequest1,
    )
    APDeliusContext_mockSuccessfulGetReferralDetails(
      crn = "CRN1",
      bookingId = booking1.id.toString(),
      arrivedAt = ZonedDateTime.of(LocalDateTime.of(2024, 5, 2, 10, 15, 30, 0), ZoneOffset.systemDefault()),
      departedAt = ZonedDateTime.of(LocalDateTime.of(2024, 5, 4, 18, 45, 20, 0), ZoneOffset.systemDefault()),
    )

    val application2 = `Given a CAS1 Application`(createdByUser = otherUser, eventNumber = "50")
    val booking2 = `Given a Booking`(
      crn = "CRN2",
      premises = premises,
      application = application2,
      arrivalDate = LocalDate.of(2024, 6, 1),
      departureDate = LocalDate.of(2024, 6, 5),
    )
    val placementRequest2 = `Given a Placement Request`(
      application = application2,
      placementRequestAllocatedTo = otherUser,
      assessmentAllocatedTo = otherUser,
      createdByUser = otherUser,
      booking = booking2,
      essentialCriteria = listOf(),
    ).first
    val (booking2CreatedByUser) = `Given a User`()
    cas1BookingDomainEventSet.bookingMade(
      application2,
      booking2,
      booking2CreatedByUser,
      placementRequest2,
    )
    val cancellationReason = cancellationReasonEntityFactory.produceAndPersist()
    cancellationEntityFactory.produceAndPersist {
      withBooking(booking2)
      withCreatedAt(OffsetDateTime.of(LocalDateTime.of(2025, 1, 2, 3, 4, 5), ZoneOffset.UTC))
      withDate(LocalDate.of(2024, 1, 1))
      withReason(cancellationReason)
      withOtherReason("cancellation other reason")
    }

    val offlineApplication = `Given an Offline Application`(
      crn = "CRN3-offline",
      eventNumber = "75",
    )
    val booking3OfflineApplication = `Given a Booking for an Offline Application`(
      crn = "CRN3",
      premises = premises,
      offlineApplication = offlineApplication,
      arrivalDate = LocalDate.of(2024, 7, 1),
      departureDate = LocalDate.of(2024, 7, 5),
    )
    val (booking3CreatedByUser) = `Given a User`()
    cas1BookingDomainEventSet.adhocBookingMade(
      onlineApplication = null,
      offlineApplication = offlineApplication,
      eventNumber = "75",
      booking = booking3OfflineApplication,
      user = booking3CreatedByUser,
    )

    val booking4OfflineNoDomainEvent = `Given a Booking for an Offline Application`(
      crn = "CRN4",
      premises = premises,
      offlineApplication = offlineApplication,
      arrivalDate = LocalDate.of(2024, 8, 1),
      departureDate = LocalDate.of(2024, 8, 5),
    )

    val existingMigratedSpaceBooking1ToRemove = `Given a CAS1 Space Booking`(
      crn = "CRN1",
      premises = premises,
      migratedFromBooking = booking1,
    )

    withCsv(
      "valid-csv",
      rowsToCsv(listOf(Cas1BookingToSpaceBookingSeedCsvRow(premises.id))),
    )

    seedService.seedData(SeedFileType.approvedPremisesBookingToSpaceBooking, "valid-csv.csv")

    assertThat(cas1SpaceBookingRepository.findByIdOrNull(existingMigratedSpaceBooking1ToRemove.id)).isNull()

    val premiseSpaceBookings = cas1SpaceBookingTestRepository.findByPremisesId(premises.id)
    assertThat(premiseSpaceBookings).hasSize(4)

    val migratedBooking1 = premiseSpaceBookings[0]
    assertThat(migratedBooking1.premises.id).isEqualTo(premises.id)
    assertThat(migratedBooking1.placementRequest!!.id).isEqualTo(placementRequest1.id)
    assertThat(migratedBooking1.createdBy!!.id).isEqualTo(booking1CreatedByUser.id)
    assertThat(migratedBooking1.expectedArrivalDate).isEqualTo(LocalDate.of(2024, 5, 1))
    assertThat(migratedBooking1.expectedDepartureDate).isEqualTo(LocalDate.of(2024, 5, 5))
    assertThat(migratedBooking1.actualArrivalDateTime).isEqualTo(Instant.parse("2024-05-02T10:15:30Z"))
    assertThat(migratedBooking1.actualDepartureDateTime).isEqualTo(Instant.parse("2024-05-04T18:45:20Z"))
    assertThat(migratedBooking1.canonicalArrivalDate).isEqualTo(LocalDate.of(2024, 5, 2))
    assertThat(migratedBooking1.canonicalDepartureDate).isEqualTo(LocalDate.of(2024, 5, 4))
    assertThat(migratedBooking1.crn).isEqualTo("CRN1")
    assertThat(migratedBooking1.keyWorkerName).isNull()
    assertThat(migratedBooking1.keyWorkerStaffCode).isNull()
    assertThat(migratedBooking1.keyWorkerAssignedAt).isNull()
    assertThat(migratedBooking1.application!!.id).isEqualTo(application1.id)
    assertThat(migratedBooking1.offlineApplication).isNull()
    assertThat(migratedBooking1.cancellationReason).isNull()
    assertThat(migratedBooking1.cancellationOccurredAt).isNull()
    assertThat(migratedBooking1.cancellationRecordedAt).isNull()
    assertThat(migratedBooking1.cancellationReasonNotes).isNull()
    assertThat(migratedBooking1.departureReason).isNull()
    assertThat(migratedBooking1.departureMoveOnCategory).isNull()
    assertThat(migratedBooking1.migratedFromBooking!!.id).isEqualTo(booking1.id)
    assertThat(migratedBooking1.criteria).containsOnly(roomCriteria1, roomCriteria2)
    assertThat(migratedBooking1.deliusEventNumber).isEqualTo("25")

    val migratedBooking2 = premiseSpaceBookings[1]
    assertThat(migratedBooking2.premises.id).isEqualTo(premises.id)
    assertThat(migratedBooking2.placementRequest!!.id).isEqualTo(placementRequest2.id)
    assertThat(migratedBooking2.createdBy!!.id).isEqualTo(booking2CreatedByUser.id)
    assertThat(migratedBooking2.expectedArrivalDate).isEqualTo(LocalDate.of(2024, 6, 1))
    assertThat(migratedBooking2.expectedDepartureDate).isEqualTo(LocalDate.of(2024, 6, 5))
    assertThat(migratedBooking2.actualArrivalDateTime).isNull()
    assertThat(migratedBooking2.actualDepartureDateTime).isNull()
    assertThat(migratedBooking2.canonicalArrivalDate).isEqualTo(LocalDate.of(2024, 6, 1))
    assertThat(migratedBooking2.canonicalDepartureDate).isEqualTo(LocalDate.of(2024, 6, 5))
    assertThat(migratedBooking2.crn).isEqualTo("CRN2")
    assertThat(migratedBooking2.keyWorkerName).isNull()
    assertThat(migratedBooking2.keyWorkerStaffCode).isNull()
    assertThat(migratedBooking2.keyWorkerAssignedAt).isNull()
    assertThat(migratedBooking2.application!!.id).isEqualTo(application2.id)
    assertThat(migratedBooking2.offlineApplication).isNull()
    assertThat(migratedBooking2.cancellationReason).isEqualTo(cancellationReason)
    assertThat(migratedBooking2.cancellationOccurredAt).isEqualTo(LocalDate.of(2024, 1, 1))
    assertThat(migratedBooking2.cancellationRecordedAt).isEqualTo(LocalDateTime.of(2025, 1, 2, 3, 4, 5).toInstant(ZoneOffset.UTC))
    assertThat(migratedBooking2.cancellationReasonNotes).isEqualTo("cancellation other reason")
    assertThat(migratedBooking2.departureReason).isNull()
    assertThat(migratedBooking2.departureMoveOnCategory).isNull()
    assertThat(migratedBooking2.migratedFromBooking!!.id).isEqualTo(booking2.id)
    assertThat(migratedBooking2.criteria).isEmpty()
    assertThat(migratedBooking2.deliusEventNumber).isEqualTo("50")

    val migratedBooking3 = premiseSpaceBookings[2]
    assertThat(migratedBooking3.premises.id).isEqualTo(premises.id)
    assertThat(migratedBooking3.placementRequest).isNull()
    assertThat(migratedBooking3.createdBy!!.id).isEqualTo(booking3CreatedByUser.id)
    assertThat(migratedBooking3.expectedArrivalDate).isEqualTo(LocalDate.of(2024, 7, 1))
    assertThat(migratedBooking3.expectedDepartureDate).isEqualTo(LocalDate.of(2024, 7, 5))
    assertThat(migratedBooking3.actualArrivalDateTime).isNull()
    assertThat(migratedBooking3.actualDepartureDateTime).isNull()
    assertThat(migratedBooking3.canonicalArrivalDate).isEqualTo(LocalDate.of(2024, 7, 1))
    assertThat(migratedBooking3.canonicalDepartureDate).isEqualTo(LocalDate.of(2024, 7, 5))
    assertThat(migratedBooking3.crn).isEqualTo("CRN3")
    assertThat(migratedBooking3.keyWorkerName).isNull()
    assertThat(migratedBooking3.keyWorkerStaffCode).isNull()
    assertThat(migratedBooking3.keyWorkerAssignedAt).isNull()
    assertThat(migratedBooking3.application).isNull()
    assertThat(migratedBooking3.offlineApplication!!.id).isEqualTo(offlineApplication.id)
    assertThat(migratedBooking3.cancellationReason).isNull()
    assertThat(migratedBooking3.cancellationOccurredAt).isNull()
    assertThat(migratedBooking3.cancellationRecordedAt).isNull()
    assertThat(migratedBooking3.cancellationReasonNotes).isNull()
    assertThat(migratedBooking3.departureReason).isNull()
    assertThat(migratedBooking3.departureMoveOnCategory).isNull()
    assertThat(migratedBooking3.migratedFromBooking!!.id).isEqualTo(booking3OfflineApplication.id)
    assertThat(migratedBooking3.criteria).isEmpty()
    assertThat(migratedBooking3.deliusEventNumber).isEqualTo("75")

    val migratedBooking4 = premiseSpaceBookings[3]
    assertThat(migratedBooking4.premises.id).isEqualTo(premises.id)
    assertThat(migratedBooking4.placementRequest).isNull()
    assertThat(migratedBooking4.createdBy).isNull()
    assertThat(migratedBooking4.expectedArrivalDate).isEqualTo(LocalDate.of(2024, 8, 1))
    assertThat(migratedBooking4.expectedDepartureDate).isEqualTo(LocalDate.of(2024, 8, 5))
    assertThat(migratedBooking4.actualArrivalDateTime).isNull()
    assertThat(migratedBooking4.actualDepartureDateTime).isNull()
    assertThat(migratedBooking4.canonicalArrivalDate).isEqualTo(LocalDate.of(2024, 8, 1))
    assertThat(migratedBooking4.canonicalDepartureDate).isEqualTo(LocalDate.of(2024, 8, 5))
    assertThat(migratedBooking4.crn).isEqualTo("CRN4")
    assertThat(migratedBooking4.keyWorkerName).isNull()
    assertThat(migratedBooking4.keyWorkerStaffCode).isNull()
    assertThat(migratedBooking4.keyWorkerAssignedAt).isNull()
    assertThat(migratedBooking4.application).isNull()
    assertThat(migratedBooking4.offlineApplication!!.id).isEqualTo(offlineApplication.id)
    assertThat(migratedBooking4.cancellationReason).isNull()
    assertThat(migratedBooking4.cancellationOccurredAt).isNull()
    assertThat(migratedBooking4.cancellationRecordedAt).isNull()
    assertThat(migratedBooking4.cancellationReasonNotes).isNull()
    assertThat(migratedBooking4.departureReason).isNull()
    assertThat(migratedBooking4.departureMoveOnCategory).isNull()
    assertThat(migratedBooking4.migratedFromBooking!!.id).isEqualTo(booking4OfflineNoDomainEvent.id)
    assertThat(migratedBooking4.criteria).isEmpty()
    assertThat(migratedBooking4.deliusEventNumber).isNull()
  }

  private fun rowsToCsv(rows: List<Cas1BookingToSpaceBookingSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "premises_id",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.premisesId)
        .newRow()
    }

    return builder.build()
  }
}
