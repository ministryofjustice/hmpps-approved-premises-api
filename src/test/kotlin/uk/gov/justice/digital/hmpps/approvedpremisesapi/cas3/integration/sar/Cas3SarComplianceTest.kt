package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.sar

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.CAS3BookingCancelledEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.CAS3BookingConfirmedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.CAS3BookingProvisionallyMadeEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.CAS3PersonArrivedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.CAS3PersonDepartedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.PersonReferenceFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.PremisesFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.StaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingConfirmedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingProvisionallyMadeEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonArrivedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonDepartedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.CasSarFixtureAsserter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Per-service SAR compliance test for CAS3.
 *
 * Cross-service SAR infrastructure (Flyway schema, JPA entity snapshot, template
 * endpoint smoke tests) lives in `SarIntegrationTest` — they only need to run
 * once for the whole app, since all four CAS services share one DB and one
 * template file.
 *
 * This class verifies CAS3's slice end-to-end against CAS3-specific fixtures
 * via [CasSarFixtureAsserter].
 */
@SuppressWarnings("LongMethod")
class Cas3SarComplianceTest : Cas3SarTestBase() {

  companion object {
    const val TEST_CRN = "X320744"
    const val TEST_NOMS_NUMBER = "A1234BF"
    const val TEST_OFFENDER_FIRST_NAME = "SAR-FIRST"
    const val TEST_OFFENDER_SURNAME = "SAR-LAST"
    const val TEST_CREATED_BY_USER_NAME = "SAR-TEST-CREATED-BY-USER"
    const val TEST_ASSESSOR_NAME = "SAR-TEST-ASSESSOR"
    const val TEST_DUTY_TO_REFER_AREA = "SAR-TEST-LAA"
    const val TEST_REJECTION_REASON_NAME = "SAR-REJ"
    const val TEST_PROBATION_REGION = "SAR-TEST-REGION"
    val TEST_FROM_DATE: LocalDate = LocalDate.of(2019, 1, 1)
    val TEST_TO_DATE: LocalDate = LocalDate.of(2024, 12, 31)

    const val EXPECTED_API_RESPONSE_PATH = "/sar/cas3-expected-api-response.json"
    const val EXPECTED_REPORT_PATH = "/sar/cas3-expected-report.html"
    const val GENERATED_API_RESPONSE_FILENAME = "cas3-expected-api-response.json.log"
    const val GENERATED_REPORT_FILENAME = "cas3-expected-report.html.log"
  }

  private val asserter by lazy {
    CasSarFixtureAsserter(
      sarHelper = sarIntegrationTestHelper,
      webTestClient = webTestClient,
      expectedApiResponseResourcePath = EXPECTED_API_RESPONSE_PATH,
      expectedReportResourcePath = EXPECTED_REPORT_PATH,
      generatedApiResponseFilename = GENERATED_API_RESPONSE_FILENAME,
      generatedReportFilename = GENERATED_REPORT_FILENAME,
    )
  }

  @BeforeEach
  fun clear() {
    assessmentReferralSystemNoteRepository.deleteAll()
    assessmentReferralUserNoteRepository.deleteAll()
    temporaryAccommodationAssessmentRepository.deleteAll()
    temporaryAccommodationApplicationRepository.deleteAll()
    domainEventRepository.deleteAll()
  }

  private fun setupTestData() {
    val (offenderDetails, _) = givenAnOffender(
      offenderDetailsConfigBlock = {
        withCrn(TEST_CRN)
        withNomsNumber(TEST_NOMS_NUMBER)
        withFirstName(TEST_OFFENDER_FIRST_NAME)
        withLastName(TEST_OFFENDER_SURNAME)
      },
    )
    val user = userEntityFactory.produceAndPersist {
      withName(TEST_CREATED_BY_USER_NAME)
      withProbationRegion(givenAProbationRegion())
      withDeliusUsername(TEST_CREATED_BY_USER_NAME)
    }
    val assessor = userEntityFactory.produceAndPersist {
      withName(TEST_ASSESSOR_NAME)
      withProbationRegion(givenAProbationRegion())
      withDeliusUsername("ASSESOR_USERNAME")
    }
    val temporaryAccommodationApplication = temporaryAccommodationApplicationEntity(
      offenderDetails,
      user,
      dutyToReferLocalAuthorityAreaName = TEST_DUTY_TO_REFER_AREA,
      probationRegionName = TEST_PROBATION_REGION,
      probationDeliveryUnitName = "SAR-TEST-PDU-2",
      data = CAS3_APPLICATION_DATA,
      document = CAS3_APPLICATION_DOCUMENT,
    )
    val temporaryAccommodationAssessment = temporaryAccommodationAssessmentEntity(
      temporaryAccommodationApplication,
      allocatedToUser = assessor,
      referralRejectionReasonName = TEST_REJECTION_REASON_NAME,
    )
    assessmentReferralHistorySystemNoteEntity(temporaryAccommodationAssessment, user)
    assessmentReferralHistoryUserNoteEntity(temporaryAccommodationAssessment, user)
    val booking = bookingEntity(
      offenderDetails,
      temporaryAccommodationApplication,
    )
    bookingExtensionEntity(booking)
    cancellationEntity(booking)
    setupDomainEvents(offenderDetails, temporaryAccommodationApplication, temporaryAccommodationAssessment, assessor, booking)
  }

  private fun setupDomainEvents(
    offenderDetails: OffenderDetailSummary,
    application: TemporaryAccommodationApplicationEntity,
    assessment: AssessmentEntity,
    assessor: UserEntity,
    booking: Cas3BookingEntity,
  ) {
    val domainEventCommon = DomainEventBuilderParams(
      offenderDetails,
      application.id,
      assessment.id,
      assessor.id,
      ServiceName.temporaryAccommodation,
      bookingId = booking.id,
    )

    domainEventEntity(domainEventCommon, DomainEventType.CAS3_REFERRAL_SUBMITTED)

    domainEventEntity(
      domainEventCommon,
      DomainEventType.CAS3_BOOKING_PROVISIONALLY_MADE,
      CAS3BookingProvisionallyMadeEvent(
        id = UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"),
        timestamp = Instant.parse("2021-07-31T00:00:00.00Z"),
        eventType = EventType.bookingProvisionallyMade,
        eventDetails =
        CAS3BookingProvisionallyMadeEventDetailsFactory()
          .withApplicationId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withBookingId(booking.id)
          .withPersonReference(
            PersonReferenceFactory().withCrn(TEST_CRN).withNoms(
              TEST_NOMS_NUMBER,
            ).produce(),
          )
          .withPremisesId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withBookedBy(staticStaffMember())
          .withExpectedArrivedAt(Instant.parse("2025-01-01T00:00:00Z"))
          .withNotes("")
          .produce(),
      ),
    )

    domainEventEntity(
      domainEventCommon,
      DomainEventType.CAS3_BOOKING_CONFIRMED,

      CAS3BookingConfirmedEvent(
        id = UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"),
        timestamp = Instant.parse("2021-07-31T00:00:00.00Z"),
        eventType = EventType.bookingConfirmed,
        eventDetails =
        CAS3BookingConfirmedEventDetailsFactory()
          .withApplicationId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withPersonReference(
            PersonReferenceFactory().withCrn(TEST_CRN).withNoms(
              TEST_NOMS_NUMBER,
            ).produce(),
          )
          .withPremisesId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withConfirmedBy(staticStaffMember())
          .withExpectedArrivedAt(Instant.parse("2025-01-01T00:00:00Z"))
          .withNotes("")
          .produce(),
      ),
    )

    domainEventEntity(
      domainEventCommon,
      DomainEventType.CAS3_BOOKING_CANCELLED,
      CAS3BookingCancelledEvent(
        id = UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"),
        timestamp = Instant.parse("2021-07-31T00:00:00.00Z"),
        eventType = EventType.bookingCancelled,
        eventDetails =
        CAS3BookingCancelledEventDetailsFactory()
          .withApplicationId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withPersonReference(
            PersonReferenceFactory().withCrn(TEST_CRN).withNoms(
              TEST_NOMS_NUMBER,
            ).produce(),
          )
          .withPremisesId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withCancellationReason("reason")
          .withCancelledAt(LocalDate.parse("2025-01-01"))
          .withCancelledBy(staticStaffMember())
          .withNotes("")
          .produce(),
      ),
    )

    domainEventEntity(
      domainEventCommon,
      DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED,
      CAS3BookingCancelledEvent(
        id = UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"),
        timestamp = Instant.parse("2021-07-31T00:00:00.00Z"),
        eventType = EventType.bookingCancelledUpdated,
        eventDetails =
        CAS3BookingCancelledEventDetailsFactory()
          .withApplicationId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withPersonReference(
            PersonReferenceFactory().withCrn(TEST_CRN).withNoms(
              TEST_NOMS_NUMBER,
            ).produce(),
          )
          .withPremisesId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withCancellationReason("reason")
          .withCancelledAt(LocalDate.parse("2025-01-01"))
          .withCancelledBy(staticStaffMember())
          .withNotes("")
          .produce(),
      ),
    )

    domainEventEntity(
      domainEventCommon,
      DomainEventType.CAS3_PERSON_ARRIVED,

      CAS3PersonArrivedEvent(
        id = UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"),
        timestamp = Instant.parse("2021-07-31T00:00:00.00Z"),
        eventType = EventType.personArrived,
        eventDetails =
        CAS3PersonArrivedEventDetailsFactory()
          .withApplicationId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withPersonReference(
            PersonReferenceFactory().withCrn(TEST_CRN).withNoms(
              TEST_NOMS_NUMBER,
            ).produce(),
          )
          .withPremisesId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withPremises(
            PremisesFactory()
              .withAddressLine1("address line1")
              .withPostcode("postcode")
              .withRegion("region")
              .produce(),
          )
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withArrivedAt(Instant.parse("2025-01-01T00:00:00Z"))
          .withExpectedDepartureOn(LocalDate.parse("2025-01-02"))
          .withDeliusEventNumber("1")
          .withNotes("notes")
          .withRecordedBy(staticStaffMember())
          .produce(),
      ),
    )

    domainEventEntity(
      domainEventCommon,
      DomainEventType.CAS3_PERSON_ARRIVED_UPDATED,

      CAS3PersonArrivedEvent(
        id = UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"),
        timestamp = Instant.parse("2021-07-31T00:00:00.00Z"),
        eventType = EventType.personArrivedUpdated,
        eventDetails =
        CAS3PersonArrivedEventDetailsFactory()
          .withApplicationId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withPersonReference(
            PersonReferenceFactory().withCrn(TEST_CRN).withNoms(
              TEST_NOMS_NUMBER,
            ).produce(),
          )
          .withPremisesId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withPremises(
            PremisesFactory()
              .withAddressLine1("address line1")
              .withPostcode("postcode")
              .withRegion("region")
              .produce(),
          )
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withArrivedAt(Instant.parse("2025-01-01T00:00:00Z"))
          .withExpectedDepartureOn(LocalDate.parse("2025-01-02"))
          .withDeliusEventNumber("1")
          .withNotes("notes")
          .withRecordedBy(staticStaffMember())
          .produce(),
      ),
    )

    domainEventEntity(
      domainEventCommon,
      DomainEventType.CAS3_PERSON_DEPARTED,

      CAS3PersonDepartedEvent(
        id = UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"),
        timestamp = Instant.parse("2021-07-31T00:00:00.00Z"),
        eventType = EventType.personDeparted,
        eventDetails =
        CAS3PersonDepartedEventDetailsFactory()
          .withApplicationId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withPersonReference(
            PersonReferenceFactory().withCrn(TEST_CRN).withNoms(
              TEST_NOMS_NUMBER,
            ).produce(),
          )
          .withPremisesId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withPremises(
            PremisesFactory()
              .withAddressLine1("address line1")
              .withPostcode("postcode")
              .withRegion("region")
              .produce(),
          )
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withDepartedAt(Instant.parse("2025-01-01T00:00:00Z"))
          .withReason("reason")
          .withReasonDetail("reason detail")
          .withDeliusEventNumber("1")
          .withNotes("notes")
          .withRecordedBy(staticStaffMember())
          .produce(),
      ),
    )

    domainEventEntity(
      domainEventCommon,
      DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED,

      CAS3PersonDepartedEvent(
        id = UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"),
        timestamp = Instant.parse("2021-07-31T00:00:00.00Z"),
        eventType = EventType.personDepartureUpdated,
        eventDetails =
        CAS3PersonDepartedEventDetailsFactory()
          .withApplicationId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withPersonReference(
            PersonReferenceFactory().withCrn(TEST_CRN).withNoms(
              TEST_NOMS_NUMBER,
            ).produce(),
          )
          .withPremisesId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withPremises(
            PremisesFactory()
              .withAddressLine1("address line1")
              .withPostcode("postcode")
              .withRegion("region")
              .produce(),
          )
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withDepartedAt(Instant.parse("2025-01-01T00:00:00Z"))
          .withReason("reason")
          .withReasonDetail("reason detail")
          .withDeliusEventNumber("1")
          .withNotes("notes")
          .withRecordedBy(staticStaffMember())
          .produce(),
      ),
    )
  }

  private fun staticStaffMember() = StaffMemberFactory()
    .withStaffCode("code")
    .withUsername("username")
    .withProbationRegionCode("region")
    .produce()

  @Test
  fun `CAS3 SAR API should return expected data`() {
    setupTestData()
    asserter.assertApiDataMatchesFixture(
      crn = TEST_CRN,
      fromDate = TEST_FROM_DATE,
      toDate = TEST_TO_DATE,
    )
  }

  @Test
  fun `CAS3 SAR report should render as expected`() {
    setupTestData()
    asserter.assertReportMatchesFixture(
      crn = TEST_CRN,
      fromDate = TEST_FROM_DATE,
    )
  }
}
