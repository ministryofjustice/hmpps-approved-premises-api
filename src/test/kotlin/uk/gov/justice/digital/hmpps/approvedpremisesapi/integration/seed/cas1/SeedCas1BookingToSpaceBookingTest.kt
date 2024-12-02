package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenABooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenABookingForAnOfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ManagementInfoSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas1SpaceBookingTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1BookingToSpaceBookingSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedCas1BookingToSpaceBookingTest : SeedTestBase() {

  @Autowired
  lateinit var cas1SpaceBookingTestRepository: Cas1SpaceBookingTestRepository

  @Autowired
  lateinit var cas1BookingDomainEventSet: Cas1BookingDomainEventService

  @Autowired
  lateinit var deliusBookingImportRepository: Cas1DeliusBookingImportRepository

  @SuppressWarnings("LongMethod")
  @Test
  fun `Migrate bookings, removing existing`() {
    val premises = givenAnApprovedPremises(
      name = "Premises 1",
      supportsSpaceBookings = true,
    )
    val otherPremise = givenAnApprovedPremises(
      name = "Premises 2",
      supportsSpaceBookings = true,
    )
    val otherUser = givenAUser().first
    val roomCriteria1 = characteristicEntityFactory.produceAndPersist { withModelScope("*") }
    val roomCriteria2 = characteristicEntityFactory.produceAndPersist { withModelScope("room") }
    val premisesCriteria = characteristicEntityFactory.produceAndPersist { withModelScope("premises") }

    val application1 = givenACas1Application(createdByUser = otherUser, eventNumber = "25")
    val booking1DeliusManagementInfo = givenABooking(
      crn = "CRN1",
      premises = premises,
      application = application1,
      arrivalDate = LocalDate.of(2024, 5, 1),
      departureDate = LocalDate.of(2024, 5, 5),
    )
    val placementRequest1 = givenAPlacementRequest(
      application = application1,
      placementRequestAllocatedTo = otherUser,
      assessmentAllocatedTo = otherUser,
      createdByUser = otherUser,
      booking = booking1DeliusManagementInfo,
      essentialCriteria = listOf(premisesCriteria, roomCriteria1, roomCriteria2),
    ).first
    val (booking1CreatedByUser) = givenAUser()
    cas1BookingDomainEventSet.bookingMade(
      application1,
      booking1DeliusManagementInfo,
      booking1CreatedByUser,
      placementRequest1,
    )

    departureReasonEntityFactory.produceAndPersist {
      withLegacyDeliusCategoryCode("dr1inactive")
      withServiceScope(ServiceName.approvedPremises.value)
    }
    val departureReason1Active = departureReasonEntityFactory.produceAndPersist {
      withLegacyDeliusCategoryCode("dr1")
      withServiceScope(ServiceName.approvedPremises.value)
    }

    val moveOnCategory1 = moveOnCategoryEntityFactory.produceAndPersist {
      withLegacyDeliusCategoryCode("moc1")
      withServiceScope(ServiceName.approvedPremises.value)
    }

    val nonArrivalReasonCode1 = nonArrivalReasonEntityFactory.produceAndPersist {
      withLegacyDeliusReasonCode("narc1")
    }

    deliusBookingImportRepository.save(
      Cas1DeliusBookingImportEntity(
        bookingId = booking1DeliusManagementInfo.id,
        crn = "irrelevant",
        eventNumber = "irrelevant",
        keyWorkerStaffCode = "kw001",
        keyWorkerForename = "kay",
        keyWorkerMiddleName = "m",
        keyWorkerSurname = "werker",
        departureReasonCode = "dr1",
        moveOnCategoryCode = "moc1",
        moveOnCategoryDescription = null,
        expectedArrivalDate = LocalDate.of(3000, 1, 1),
        arrivalDate = LocalDate.of(2024, 5, 2),
        expectedDepartureDate = LocalDate.of(3001, 2, 2),
        departureDate = LocalDate.of(2024, 5, 4),
        nonArrivalDate = LocalDate.of(3000, 1, 1),
        nonArrivalContactDatetime = OffsetDateTime.of(LocalDateTime.of(2024, 2, 1, 9, 58, 23), ZoneOffset.UTC),
        nonArrivalReasonCode = "narc1",
        nonArrivalReasonDescription = null,
        nonArrivalNotes = "the non arrival notes",
      ),
    )

    val application2 = givenACas1Application(createdByUser = otherUser, eventNumber = "50")
    val booking2LegacyCas1ManagementInfo = givenABooking(
      crn = "CRN2",
      premises = premises,
      application = application2,
      arrivalDate = LocalDate.of(2024, 6, 1),
      departureDate = LocalDate.of(2024, 6, 5),
      departureReason = departureReasonEntityFactory.produceAndPersist(),
      departureNotes = "the legacy departure notes",
      departureMoveOnCategory = moveOnCategoryEntityFactory.produceAndPersist(),
      nonArrivalReason = nonArrivalReasonEntityFactory.produceAndPersist(),
      nonArrivalConfirmedAt = OffsetDateTime.of(LocalDateTime.of(2024, 5, 1, 2, 3, 4), ZoneOffset.UTC),
      nonArrivalNotes = "the legacy non arrival notes",
      actualArrivalDate = LocalDateTime.of(2024, 6, 3, 20, 0, 5),
      actualDepartureDate = LocalDateTime.of(2024, 6, 6, 9, 48, 0),
    )
    val placementRequest2 = givenAPlacementRequest(
      application = application2,
      placementRequestAllocatedTo = otherUser,
      assessmentAllocatedTo = otherUser,
      createdByUser = otherUser,
      booking = booking2LegacyCas1ManagementInfo,
      essentialCriteria = listOf(),
    ).first
    val (booking2CreatedByUser) = givenAUser()
    cas1BookingDomainEventSet.bookingMade(
      application2,
      booking2LegacyCas1ManagementInfo,
      booking2CreatedByUser,
      placementRequest2,
    )
    val cancellationReason = cancellationReasonEntityFactory.produceAndPersist()
    cancellationEntityFactory.produceAndPersist {
      withBooking(booking2LegacyCas1ManagementInfo)
      withCreatedAt(OffsetDateTime.of(LocalDateTime.of(2025, 1, 2, 3, 4, 5), ZoneOffset.UTC))
      withDate(LocalDate.of(2024, 1, 1))
      withReason(cancellationReason)
      withOtherReason("cancellation other reason")
    }
    dateChangeEntityFactory.produceAndPersist {
      withBooking(booking2LegacyCas1ManagementInfo)
      withChangedByUser(booking2CreatedByUser)
    }

    val offlineApplication = givenAnOfflineApplication(
      crn = "CRN3",
      eventNumber = "75",
    )
    val booking3OfflineApplication = givenABookingForAnOfflineApplication(
      crn = "CRN3",
      premises = premises,
      offlineApplication = offlineApplication,
      arrivalDate = LocalDate.of(2024, 7, 1),
      departureDate = LocalDate.of(2024, 7, 5),
    )
    val (booking3CreatedByUser) = givenAUser()
    cas1BookingDomainEventSet.adhocBookingMade(
      onlineApplication = null,
      offlineApplication = offlineApplication,
      eventNumber = "75",
      booking = booking3OfflineApplication,
      user = booking3CreatedByUser,
    )

    val booking4OfflineNoDomainEvent = givenABookingForAnOfflineApplication(
      crn = "CRN4",
      premises = premises,
      offlineApplication = offlineApplication,
      arrivalDate = LocalDate.of(2024, 8, 1),
      departureDate = LocalDate.of(2024, 8, 5),
    )

    val booking5DifferentPremiseNotDeleted = givenABookingForAnOfflineApplication(
      crn = "CRN5",
      premises = otherPremise,
      offlineApplication = offlineApplication,
      arrivalDate = LocalDate.of(2025, 8, 1),
      departureDate = LocalDate.of(2025, 8, 5),
    )

    withCsv(
      "valid-csv",
      rowsToCsv(listOf(Cas1BookingToSpaceBookingSeedCsvRow(premises.id))),
    )

    seedService.seedData(SeedFileType.approvedPremisesBookingToSpaceBooking, "valid-csv.csv")

    val premiseSpaceBookings = cas1SpaceBookingTestRepository.findByPremisesId(premises.id)
    assertThat(premiseSpaceBookings).hasSize(4)

    val migratedBooking1 = premiseSpaceBookings[0]
    assertBookingIsDeleted(booking1DeliusManagementInfo.id)
    assertThat(migratedBooking1.id).isEqualTo(booking1DeliusManagementInfo.id)
    assertThat(migratedBooking1.premises.id).isEqualTo(premises.id)
    assertThat(migratedBooking1.placementRequest!!.id).isEqualTo(placementRequest1.id)
    assertThat(migratedBooking1.createdBy!!.id).isEqualTo(booking1CreatedByUser.id)
    assertThat(migratedBooking1.expectedArrivalDate).isEqualTo(LocalDate.of(2024, 5, 1))
    assertThat(migratedBooking1.expectedDepartureDate).isEqualTo(LocalDate.of(2024, 5, 5))
    assertThat(migratedBooking1.actualArrivalDate).isEqualTo(LocalDate.parse("2024-05-02"))
    assertThat(migratedBooking1.actualArrivalTime).isNull()
    assertThat(migratedBooking1.actualDepartureDate).isEqualTo(LocalDate.parse("2024-05-04"))
    assertThat(migratedBooking1.actualDepartureTime).isNull()
    assertThat(migratedBooking1.canonicalArrivalDate).isEqualTo(LocalDate.of(2024, 5, 2))
    assertThat(migratedBooking1.canonicalDepartureDate).isEqualTo(LocalDate.of(2024, 5, 4))
    assertThat(migratedBooking1.crn).isEqualTo("CRN1")
    assertThat(migratedBooking1.keyWorkerName).isEqualTo("kay werker")
    assertThat(migratedBooking1.keyWorkerStaffCode).isEqualTo("kw001")
    assertThat(migratedBooking1.keyWorkerAssignedAt).isNull()
    assertThat(migratedBooking1.application!!.id).isEqualTo(application1.id)
    assertThat(migratedBooking1.offlineApplication).isNull()
    assertThat(migratedBooking1.cancellationReason).isNull()
    assertThat(migratedBooking1.cancellationOccurredAt).isNull()
    assertThat(migratedBooking1.cancellationRecordedAt).isNull()
    assertThat(migratedBooking1.cancellationReasonNotes).isNull()
    assertThat(migratedBooking1.departureReason).isEqualTo(departureReason1Active)
    assertThat(migratedBooking1.departureMoveOnCategory).isEqualTo(moveOnCategory1)
    assertThat(migratedBooking1.criteria).containsOnly(roomCriteria1, roomCriteria2)
    assertThat(migratedBooking1.nonArrivalReason).isEqualTo(nonArrivalReasonCode1)
    assertThat(migratedBooking1.nonArrivalConfirmedAt).isEqualTo(Instant.parse("2024-02-01T09:58:23.00Z"))
    assertThat(migratedBooking1.nonArrivalNotes).isEqualTo("the non arrival notes")
    assertThat(migratedBooking1.deliusEventNumber).isEqualTo("25")
    assertThat(migratedBooking1.migratedManagementInfoFrom).isEqualTo(ManagementInfoSource.DELIUS)

    val migratedBooking2 = premiseSpaceBookings[1]
    assertBookingIsDeleted(booking2LegacyCas1ManagementInfo.id)
    assertThat(migratedBooking2.id).isEqualTo(booking2LegacyCas1ManagementInfo.id)
    assertThat(migratedBooking2.premises.id).isEqualTo(premises.id)
    assertThat(migratedBooking2.placementRequest!!.id).isEqualTo(placementRequest2.id)
    assertThat(migratedBooking2.createdBy!!.id).isEqualTo(booking2CreatedByUser.id)
    assertThat(migratedBooking2.expectedArrivalDate).isEqualTo(LocalDate.of(2024, 6, 1))
    assertThat(migratedBooking2.expectedDepartureDate).isEqualTo(LocalDate.of(2024, 6, 5))
    assertThat(migratedBooking2.actualArrivalDate).isEqualTo(LocalDate.parse("2024-06-03"))
    assertThat(migratedBooking2.actualArrivalTime).isEqualTo(LocalTime.parse("20:00:05"))
    assertThat(migratedBooking2.actualDepartureDate).isEqualTo(LocalDate.parse("2024-06-06"))
    assertThat(migratedBooking2.actualDepartureTime).isEqualTo(LocalTime.parse("09:48:00"))
    assertThat(migratedBooking2.canonicalArrivalDate).isEqualTo(LocalDate.of(2024, 6, 3))
    assertThat(migratedBooking2.canonicalDepartureDate).isEqualTo(LocalDate.of(2024, 6, 6))
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
    assertThat(migratedBooking2.departureReason).isEqualTo(booking2LegacyCas1ManagementInfo.departure!!.reason)
    assertThat(migratedBooking2.departureMoveOnCategory).isEqualTo(booking2LegacyCas1ManagementInfo.departure!!.moveOnCategory)
    assertThat(migratedBooking2.departureNotes).isEqualTo("the legacy departure notes")
    assertThat(migratedBooking2.criteria).isEmpty()
    assertThat(migratedBooking2.nonArrivalReason).isEqualTo(booking2LegacyCas1ManagementInfo.nonArrival!!.reason)
    assertThat(migratedBooking2.nonArrivalConfirmedAt).isEqualTo(Instant.parse("2024-05-01T02:03:04Z"))
    assertThat(migratedBooking2.nonArrivalNotes).isEqualTo("the legacy non arrival notes")
    assertThat(migratedBooking2.deliusEventNumber).isEqualTo("50")
    assertThat(migratedBooking2.migratedManagementInfoFrom).isEqualTo(ManagementInfoSource.LEGACY_CAS_1)

    val migratedBooking3 = premiseSpaceBookings[2]
    assertBookingIsDeleted(booking3OfflineApplication.id)
    assertThat(migratedBooking3.id).isEqualTo(booking3OfflineApplication.id)
    assertThat(migratedBooking3.premises.id).isEqualTo(premises.id)
    assertThat(migratedBooking3.placementRequest).isNull()
    assertThat(migratedBooking3.createdBy!!.id).isEqualTo(booking3CreatedByUser.id)
    assertThat(migratedBooking3.expectedArrivalDate).isEqualTo(LocalDate.of(2024, 7, 1))
    assertThat(migratedBooking3.expectedDepartureDate).isEqualTo(LocalDate.of(2024, 7, 5))
    assertThat(migratedBooking3.actualArrivalDate).isNull()
    assertThat(migratedBooking3.actualArrivalTime).isNull()
    assertThat(migratedBooking3.actualDepartureDate).isNull()
    assertThat(migratedBooking3.actualDepartureTime).isNull()
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
    assertThat(migratedBooking3.nonArrivalReason).isNull()
    assertThat(migratedBooking3.nonArrivalConfirmedAt).isNull()
    assertThat(migratedBooking3.nonArrivalNotes).isNull()
    assertThat(migratedBooking3.criteria).isEmpty()
    assertThat(migratedBooking3.deliusEventNumber).isEqualTo("75")
    assertThat(migratedBooking3.migratedManagementInfoFrom).isEqualTo(ManagementInfoSource.LEGACY_CAS_1)

    val migratedBooking4 = premiseSpaceBookings[3]
    assertBookingIsDeleted(booking4OfflineNoDomainEvent.id)
    assertThat(migratedBooking4.id).isEqualTo(booking4OfflineNoDomainEvent.id)
    assertThat(migratedBooking4.premises.id).isEqualTo(premises.id)
    assertThat(migratedBooking4.placementRequest).isNull()
    assertThat(migratedBooking4.createdBy).isNull()
    assertThat(migratedBooking4.expectedArrivalDate).isEqualTo(LocalDate.of(2024, 8, 1))
    assertThat(migratedBooking4.expectedDepartureDate).isEqualTo(LocalDate.of(2024, 8, 5))
    assertThat(migratedBooking4.actualArrivalDate).isNull()
    assertThat(migratedBooking4.actualArrivalTime).isNull()
    assertThat(migratedBooking4.actualDepartureDate).isNull()
    assertThat(migratedBooking4.actualDepartureTime).isNull()
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
    assertThat(migratedBooking4.nonArrivalReason).isNull()
    assertThat(migratedBooking4.nonArrivalConfirmedAt).isNull()
    assertThat(migratedBooking4.nonArrivalNotes).isNull()
    assertThat(migratedBooking4.criteria).isEmpty()
    assertThat(migratedBooking4.deliusEventNumber).isNull()
    assertThat(migratedBooking4.migratedManagementInfoFrom).isEqualTo(ManagementInfoSource.LEGACY_CAS_1)

    assertBookingIsNotDeleted(booking5DifferentPremiseNotDeleted.id)
  }

  @Test
  fun `Update domain event links from booking to space booking when migrating`() {
    val premises1 = givenAnApprovedPremises(
      name = "Premises 1",
      supportsSpaceBookings = true,
    )
    val premises2 = givenAnApprovedPremises(
      name = "Premises 2",
      supportsSpaceBookings = true,
    )
    val user = givenAUser().first

    val application1 = givenACas1Application(createdByUser = user, eventNumber = "25")
    val booking1InPremises1 = givenABooking(
      crn = "CRN1",
      premises = premises1,
      application = application1,
      arrivalDate = LocalDate.of(2024, 5, 1),
      departureDate = LocalDate.of(2024, 5, 5),
    )
    val placementRequest1 = givenAPlacementRequest(
      application = application1,
      placementRequestAllocatedTo = user,
      assessmentAllocatedTo = user,
      createdByUser = user,
      booking = booking1InPremises1,
    ).first
    val (booking1CreatedByUser) = givenAUser()
    cas1BookingDomainEventSet.bookingMade(
      application1,
      booking1InPremises1,
      booking1CreatedByUser,
      placementRequest1,
    )

    val booking2InPremises2 = givenABooking(
      crn = "CRN1",
      premises = premises2,
      application = application1,
      arrivalDate = LocalDate.of(2024, 5, 1),
      departureDate = LocalDate.of(2024, 5, 5),
    )
    val placementRequest2 = givenAPlacementRequest(
      application = application1,
      placementRequestAllocatedTo = user,
      assessmentAllocatedTo = user,
      createdByUser = user,
      booking = booking2InPremises2,
    ).first
    val (booking2CreatedByUser) = givenAUser()
    cas1BookingDomainEventSet.bookingMade(
      application1,
      booking2InPremises2,
      booking2CreatedByUser,
      placementRequest2,
    )

    withCsv(
      "valid-csv",
      rowsToCsv(listOf(Cas1BookingToSpaceBookingSeedCsvRow(premises1.id))),
    )

    seedService.seedData(SeedFileType.approvedPremisesBookingToSpaceBooking, "valid-csv.csv")

    val bookingCreatedDomainEvents = domainEventRepository.findByApplicationId(application1.id)
    assertThat(bookingCreatedDomainEvents).hasSize(2)

    val premise1SpaceBooking = cas1SpaceBookingTestRepository.findByPremisesId(premises1.id)[0]
    val booking1CreatedDomainEvent = bookingCreatedDomainEvents.first { it.cas1SpaceBookingId == premise1SpaceBooking.id }
    assertThat(booking1CreatedDomainEvent.bookingId).isNull()
    assertThat(booking1CreatedDomainEvent.cas1SpaceBookingId).isEqualTo(premise1SpaceBooking.id)
    assertBookingIsDeleted(booking1InPremises1.id)

    assertThat(cas1SpaceBookingTestRepository.findByPremisesId(premises2.id)).isEmpty()
    val booking2CreatedDomainEvent = bookingCreatedDomainEvents.first { it.bookingId == booking2InPremises2.id }
    assertThat(booking2CreatedDomainEvent.bookingId).isEqualTo(booking2InPremises2.id)
    assertThat(booking2CreatedDomainEvent.cas1SpaceBookingId).isNull()
    assertBookingIsNotDeleted(booking2InPremises2.id)
  }

  private fun assertBookingIsDeleted(bookingId: UUID) = assertThat(bookingRepository.findByIdOrNull(bookingId)).isNull()

  private fun assertBookingIsNotDeleted(bookingId: UUID) = assertThat(bookingRepository.findByIdOrNull(bookingId)).isNotNull

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
