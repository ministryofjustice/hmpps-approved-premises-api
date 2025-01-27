package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ManagementInfoSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas1SpaceBookingTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1CreateMissingReferralsSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class SeedCas1BackfillActiveSpaceBookingsCreatedInDeliusTest : SeedTestBase() {

  @Autowired
  lateinit var cas1SpaceBookingTestRepository: Cas1SpaceBookingTestRepository

  @Autowired
  lateinit var deliusBookingImportRepository: Cas1DeliusBookingImportRepository

  lateinit var premises: ApprovedPremisesEntity
  lateinit var otherPremise: ApprovedPremisesEntity
  lateinit var otherUser: UserEntity

  lateinit var departureReasonActive: DepartureReasonEntity
  lateinit var moveOnCategory: MoveOnCategoryEntity
  lateinit var nonArrivalReasonCode: NonArrivalReasonEntity

  @BeforeEach
  fun setupReferenceData() {
    premises = givenAnApprovedPremises(
      name = "Premises 1",
      supportsSpaceBookings = true,
    )
    otherPremise = givenAnApprovedPremises(
      name = "Premises 2",
      supportsSpaceBookings = true,
    )
    otherUser = givenAUser().first

    departureReasonEntityFactory.produceAndPersist {
      withLegacyDeliusCategoryCode("dr1inactive")
      withServiceScope(ServiceName.approvedPremises.value)
    }
    departureReasonActive = departureReasonEntityFactory.produceAndPersist {
      withLegacyDeliusCategoryCode("dr1")
      withServiceScope(ServiceName.approvedPremises.value)
    }
    moveOnCategory = moveOnCategoryEntityFactory.produceAndPersist {
      withLegacyDeliusCategoryCode("moc1")
      withServiceScope(ServiceName.approvedPremises.value)
    }
    nonArrivalReasonCode = nonArrivalReasonEntityFactory.produceAndPersist {
      withLegacyDeliusReasonCode("narc1")
    }
  }

  @SuppressWarnings("LongMethod")
  @Test
  fun `Backfill Space Bookings from Delius Import`() {
    apDeliusContextAddListCaseSummaryToBulkResponse(
      listOf(
        CaseSummaryFactory()
          .withCrn("CRN1")
          .withName(
            NameFactory()
              .withForename("Max")
              .withSurname("Power")
              .produce(),
          )
          .produce(),
      ),
    )

    val deliusBooking = Cas1DeliusBookingImportEntity(
      id = UUID.randomUUID(),
      bookingId = null,
      approvedPremisesReferralId = "Delius Referral Id",
      crn = "CRN1",
      eventNumber = "67",
      keyWorkerStaffCode = "kw001",
      keyWorkerForename = "kay",
      keyWorkerMiddleName = "m",
      keyWorkerSurname = "werker",
      departureReasonCode = "dr1",
      moveOnCategoryCode = "moc1",
      moveOnCategoryDescription = null,
      expectedArrivalDate = LocalDate.of(2024, 5, 9),
      arrivalDate = LocalDate.of(2024, 5, 2),
      expectedDepartureDate = LocalDate.of(2025, 6, 2),
      departureDate = null,
      nonArrivalDate = LocalDate.of(3000, 1, 1),
      nonArrivalContactDatetime = OffsetDateTime.of(LocalDateTime.of(2024, 2, 1, 9, 58, 23), ZoneOffset.UTC),
      nonArrivalReasonCode = null,
      nonArrivalReasonDescription = null,
      nonArrivalNotes = "the non arrival notes",
      premisesQcode = premises.qCode,
      createdAt = OffsetDateTime.now(),
    )

    deliusBookingImportRepository.save(
      deliusBooking.copy(
        id = UUID.randomUUID(),
      ),
    )

    // booking with booking id (e.g. created in CAS1) is ignored
    deliusBookingImportRepository.save(
      deliusBooking.copy(
        id = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
      ),
    )

    // booking for other premise is ignored
    deliusBookingImportRepository.save(
      deliusBooking.copy(
        id = UUID.randomUUID(),
        premisesQcode = otherPremise.qCode,
      ),
    )

    // booking with non arrival recorded is ignored
    deliusBookingImportRepository.save(
      deliusBooking.copy(
        id = UUID.randomUUID(),
        nonArrivalReasonCode = "narc1",
      ),
    )

    // booking with departure recorded is ignore
    deliusBookingImportRepository.save(
      deliusBooking.copy(
        id = UUID.randomUUID(),
        departureDate = LocalDate.of(2025, 5, 4),
      ),
    )

    // booking with departure before 2025-1-1 is ignored
    deliusBookingImportRepository.save(
      deliusBooking.copy(
        id = UUID.randomUUID(),
        departureDate = LocalDate.of(2025, 1, 1).minusDays(1),
      ),
    )

    // booking with departure after 2035-1-1 is ignored
    deliusBookingImportRepository.save(
      deliusBooking.copy(
        id = UUID.randomUUID(),
        departureDate = LocalDate.of(2035, 1, 1).plusDays(1),
      ),
    )

    withCsv(
      "valid-csv",
      rowsToCsv(listOf(Cas1CreateMissingReferralsSeedCsvRow(premises.qCode))),
    )

    seedService.seedData(SeedFileType.approvedPremisesBackfillActiveSpaceBookingsCreatedInDelius, "valid-csv.csv")

    val premiseSpaceBookings = cas1SpaceBookingTestRepository.findByPremisesId(premises.id)
    assertThat(premiseSpaceBookings).hasSize(1)

    val migratedBooking1 = premiseSpaceBookings[0]

    assertThat(migratedBooking1.crn).isEqualTo("CRN1")
    assertThat(migratedBooking1.deliusEventNumber).isEqualTo("67")
    assertThat(migratedBooking1.premises.id).isEqualTo(premises.id)
    assertThat(migratedBooking1.placementRequest).isNull()
    assertThat(migratedBooking1.createdBy).isNull()
    assertThat(migratedBooking1.expectedArrivalDate).isEqualTo(LocalDate.of(2024, 5, 9))
    assertThat(migratedBooking1.expectedDepartureDate).isEqualTo(LocalDate.of(2025, 6, 2))
    assertThat(migratedBooking1.actualArrivalDate).isEqualTo(LocalDate.parse("2024-05-02"))
    assertThat(migratedBooking1.actualArrivalTime).isNull()
    assertThat(migratedBooking1.actualDepartureDate).isNull()
    assertThat(migratedBooking1.actualDepartureTime).isNull()
    assertThat(migratedBooking1.canonicalArrivalDate).isEqualTo(LocalDate.of(2024, 5, 2))
    assertThat(migratedBooking1.canonicalDepartureDate).isEqualTo(LocalDate.of(2025, 6, 2))
    assertThat(migratedBooking1.keyWorkerName).isEqualTo("kay werker")
    assertThat(migratedBooking1.keyWorkerStaffCode).isEqualTo("kw001")
    assertThat(migratedBooking1.keyWorkerAssignedAt).isNull()
    assertThat(migratedBooking1.application).isNull()
    assertThat(migratedBooking1.cancellationReason).isNull()
    assertThat(migratedBooking1.cancellationOccurredAt).isNull()
    assertThat(migratedBooking1.cancellationRecordedAt).isNull()
    assertThat(migratedBooking1.cancellationReasonNotes).isNull()
    assertThat(migratedBooking1.departureReason).isNull()
    assertThat(migratedBooking1.departureMoveOnCategory).isNull()
    assertThat(migratedBooking1.departureNotes).isNull()
    assertThat(migratedBooking1.criteria).isEmpty()
    assertThat(migratedBooking1.nonArrivalReason).isNull()
    assertThat(migratedBooking1.nonArrivalConfirmedAt).isNull()
    assertThat(migratedBooking1.nonArrivalNotes).isNull()
    assertThat(migratedBooking1.migratedManagementInfoFrom).isEqualTo(ManagementInfoSource.DELIUS)

    val offlineApplication1 = migratedBooking1.offlineApplication!!
    assertThat(offlineApplication1.crn).isEqualTo("CRN1")
    assertThat(offlineApplication1.service).isEqualTo(ServiceName.approvedPremises.value)
    assertThat(offlineApplication1.createdAt).isWithinTheLastMinute()
    assertThat(offlineApplication1.eventNumber).isEqualTo("67")
    assertThat(offlineApplication1.name).isEqualTo("Max Power")
  }

  private fun rowsToCsv(rows: List<Cas1CreateMissingReferralsSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "q_code",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(premises.qCode)
        .newRow()
    }

    return builder.build()
  }
}
