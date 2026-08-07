package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.sar

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationExpiredFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingChangedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeBookedByFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.Cas1DomainEventEnvelopeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.CruFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonReferenceFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.StaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.CasSarFixtureAsserter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Per-service SAR compliance test for CAS1.
 *
 * Cross-service SAR infrastructure (Flyway schema, JPA entity snapshot, template
 * endpoint smoke tests) lives in `SarIntegrationTest` — they only need to run
 * once for the whole app, since all four CAS services share one DB and one
 * template file.
 *
 * This class verifies CAS1's slice end-to-end against CAS1-specific fixtures
 * via [CasSarFixtureAsserter].
 */
class Cas1SarComplianceTest : Cas1SarTestBase() {

  companion object {
    const val TEST_CRN = "X320741"
    const val TEST_NOMS_NUMBER = "A1234BC"
    const val TEST_OFFENDER_FIRST_NAME = "SAR-FIRST"
    const val TEST_OFFENDER_SURNAME = "SAR-LAST"
    const val TEST_APPLICANT_NAME = "SAR-TEST-APPLICANT"
    const val TEST_APPLICATION_CREATED_BY_USER_NAME = "SAR-TEST-CREATED-BY-USER"
    const val TEST_ASSESSOR_NAME = "SAR-TEST-ASSESSOR"
    const val TEST_ASSESSOR_USERNAME = "SAR-TEST-ASSESSOR-USERNAME"
    const val TEST_CASE_MANAGER_NAME = "SAR-TEST-CASE-MANAGER"
    const val TEST_SPACE_BOOKING_CREATED_BY_USER_NAME = "SAR-TEST-BOOKING-CREATED-BY"
    const val TEST_PREMISES_NAME = "SAR-TEST-PREMISES"
    const val TEST_CHARACTERISTIC_NAME = "SAR-TEST-CHARACTERISTIC"
    const val TEST_CHARACTERISTIC_PROPERTY_NAME = "SAR-CHAR-PROP"
    const val TEST_DESIRABLE_CRITERIA_NAME = "SAR-DESIRABLE"
    const val TEST_DESIRABLE_CRITERIA_PROPERTY_NAME = "SAR-DESIRABLE-PROP"
    const val TEST_ESSENTIAL_CRITERIA_NAME = "SAR-ESSENTIAL"
    const val TEST_ESSENTIAL_CRITERIA_PROPERTY_NAME = "SAR-ESSENTIAL-PROP"
    const val TEST_POSTCODE_OUTCODE = "SAR1"
    const val TEST_NON_ARRIVAL_REASON_NAME = "SAR-NON-ARR"
    const val TEST_NON_ARRIVAL_REASON_ID = "11111111-1111-1111-1111-111111111111"
    const val TEST_DEPARTURE_REASON_NAME = "SAR-DEP"
    const val TEST_MOVE_ON_CATEGORY_NAME = "SAR-MOC"
    const val TEST_CANCELLATION_REASON_NAME = "SAR-CXL"
    val TEST_FROM_DATE: LocalDate = LocalDate.of(2019, 1, 1)
    val TEST_TO_DATE: LocalDate = LocalDate.of(2024, 12, 31)

    const val EXPECTED_API_RESPONSE_PATH = "/sar/cas1-expected-api-response.json"
    const val EXPECTED_REPORT_PATH = "/sar/cas1-expected-report.html"
    const val GENERATED_API_RESPONSE_FILENAME = "cas1-sar-api-response.json.log"
    const val GENERATED_REPORT_FILENAME = "cas1-sar-report.html.log"
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
    clearTestData()
  }

  private fun clearTestData() {
    bookingNotMadeRepository.deleteAll()
    placementRequestRepository.deleteAll()
    placementApplicationRepository.deleteAll()
    cas1SpaceBookingRepository.deleteAll()
    assessmentClarificationNoteRepository.deleteAll()
    applicationTimelineNoteRepository.deleteAll()
    appealTestRepository.deleteAll()
    approvedPremisesAssessmentRepository.deleteAll()
    approvedPremisesApplicationRepository.deleteAll()
    offlineApplicationRepository.deleteAll()
    domainEventRepository.deleteAll()
    bedRepository.deleteAll()
    roomRepository.deleteAll()
    approvedPremisesRepository.deleteAll()
    cas1CharacteristicRepository.deleteAll()
    nonArrivalReasonRepository.deleteAll()
    moveOnCategoryRepository.deleteAll()
    cancellationReasonRepository.deleteAll()
  }

  @SuppressWarnings("LongMethod")
  private fun setupTestData() {
    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(TEST_CRN)
      .withNomsNumber(TEST_NOMS_NUMBER)
      .withFirstName(TEST_OFFENDER_FIRST_NAME)
      .withLastName(TEST_OFFENDER_SURNAME)
      .produce()
    val createdByUser = userEntityFactory.produceAndPersist {
      withName(TEST_APPLICATION_CREATED_BY_USER_NAME)
      withDeliusUsername(TEST_APPLICATION_CREATED_BY_USER_NAME)
      withProbationRegion(givenAProbationRegion())
    }
    val assessor = userEntityFactory.produceAndPersist {
      withName(TEST_ASSESSOR_NAME)
      withProbationRegion(givenAProbationRegion())
      withDeliusUsername(TEST_ASSESSOR_USERNAME)
    }
    val spaceBookingCreatedByUser = userEntityFactory.produceAndPersist {
      withName(TEST_SPACE_BOOKING_CREATED_BY_USER_NAME)
      withProbationRegion(givenAProbationRegion())
    }

    val application = approvedPremisesApplicationEntity(
      offenderDetails,
      caseManagerName = TEST_CASE_MANAGER_NAME,
      createdByUser = createdByUser,
      applicantUserName = TEST_APPLICANT_NAME,
      data = CAS1_APPLICATION_DATA,
      document = "null",
    )
    approvedPremisesApplicationEntity(
      offenderDetails,
      caseManagerName = TEST_CASE_MANAGER_NAME,
      createdByUser = createdByUser,
      applicantUserName = TEST_APPLICANT_NAME,
      data = CAS1_APPLICATION_DATA,
      document = CAS1_APPLICATION_DOCUMENT,
    )
    val assessment = approvedPremisesAssessmentEntity(application, assessor, CAS1_ASSESSMENT_DATA, "null")
    approvedPremisesAssessmentEntity(application, assessor, CAS1_ASSESSMENT_DATA, CAS1_ASSESSMENT_DOCUMENT)

    applicationTimelineNoteEntity(application)
    approvedPremisesAssessmentClarificationNoteEntity(assessment)
    appealEntity(application, assessment)

    val placementApplication = placementApplicationEntity(application, "null", CAS1_PLACEMENT_APPLICATION_DATA)
    placementApplicationEntity(application, CAS1_PLACEMENT_APPLICATION_DOCUMENT, CAS1_PLACEMENT_APPLICATION_DATA)
    val placementRequirements = placementRequirementEntity(
      application,
      assessment,
      desirableCriteria = listOf(
        characteristicEntity(
          name = TEST_DESIRABLE_CRITERIA_NAME,
          propertyName = TEST_DESIRABLE_CRITERIA_PROPERTY_NAME,
        ),
      ),
      essentialCriteria = listOf(
        characteristicEntity(
          name = TEST_ESSENTIAL_CRITERIA_NAME,
          propertyName = TEST_ESSENTIAL_CRITERIA_PROPERTY_NAME,
        ),
      ),
      postcodeOutcode = TEST_POSTCODE_OUTCODE,
    )
    val placementRequest = placementRequestEntity(
      assessment,
      application,
      placementApplication,
      placementRequirements = placementRequirements,
    )
    bookingNotMadeEntity(placementRequest)

    val nonArrivalReason = nonArrivalReasonEntityFactory.produceAndPersist {
      withId(UUID.fromString(TEST_NON_ARRIVAL_REASON_ID))
      withName(TEST_NON_ARRIVAL_REASON_NAME)
    }
    val departureReason = departureReasonEntityFactory.produceAndPersist {
      withName(TEST_DEPARTURE_REASON_NAME)
    }
    val moveOnCategory = moveOnCategoryEntityFactory.produceAndPersist {
      withName(TEST_MOVE_ON_CATEGORY_NAME)
    }
    val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
      withName(TEST_CANCELLATION_REASON_NAME)
    }
    spaceBookingEntity(
      offenderDetails = offenderDetails,
      application = application,
      nonArrivalReason = nonArrivalReason,
      departureReason = departureReason,
      moveOnCategory = moveOnCategory,
      cancellationReason = cancellationReason,
      transferType = TRANSFER_TYPE,
      additionalInformation = ADDITIONAL_INFORMATION,
      transferReason = TRANSFER_REASON,
      createdByUser = spaceBookingCreatedByUser,
      premisesName = TEST_PREMISES_NAME,
      characteristicName = TEST_CHARACTERISTIC_NAME,
      characteristicPropertyName = TEST_CHARACTERISTIC_PROPERTY_NAME,
    )

    val domainEventCommon = DomainEventBuilderParams(offenderDetails, application.id, assessment.id, assessor.id, ServiceName.approvedPremises)

    domainEventEntity(domainEventCommon, DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
    domainEventEntity(domainEventCommon, DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED)
    domainEventEntity(domainEventCommon, DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)
    domainEventEntity(
      params = domainEventCommon,
      type = DomainEventType.APPROVED_PREMISES_APPLICATION_EXPIRED,
      data = domainEventDetailsWrapper(
        ApplicationExpiredFactory()
          .withApplicationId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withPreviousStatus("ASSESSED")
          .withUpdatedStatus("EXPIRED")
          .produce(),
      ),
    )
    domainEventEntity(
      params = domainEventCommon,
      type = DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
      data = domainEventDetailsWrapper(
        BookingMadeFactory()
          .withApplicationId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withApplicationUrl("NA")
          .withPersonReference(PersonReferenceFactory().withCrn(TEST_CRN).withNoms(TEST_NOMS_NUMBER).produce())
          .withDeliusEventNumber("1")
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withCreatedAt(Instant.parse(CREATED_AT))
          .withBookedBy(
            BookingMadeBookedByFactory()
              .withStaffMember(staticStaffMember())
              .withCru(CruFactory().withName("cru").produce())
              .produce(),
          )
          .withPremises(staticPremises())
          .withArrivalOn(LocalDate.of(2025, 1, 1))
          .withDepartureOn(LocalDate.of(2025, 2, 1))
          .produce(),
      ),
    )

    domainEventEntity(
      params = domainEventCommon,
      type = DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED,
      data = domainEventDetailsWrapper(
        BookingChangedFactory()
          .withApplicationId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withApplicationUrl("NA")
          .withPersonReference(PersonReferenceFactory().withCrn(TEST_CRN).withNoms(TEST_NOMS_NUMBER).produce())
          .withDeliusEventNumber("1")
          .withBookingId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withChangedAt(Instant.parse(CREATED_AT))
          .withChangedBy(staticStaffMember())
          .withPremises(staticPremises())
          .withPreviousArrivalOn(LocalDate.of(2025, 1, 1))
          .withPreviousDepartureOn(LocalDate.of(2025, 2, 1))
          .withArrivalOn(LocalDate.of(2025, 3, 1))
          .withDepartureOn(LocalDate.of(2025, 4, 1))
          .produce(),
      ),
    )
  }

  private fun staticPremises() = Premises(
    id = UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"),
    name = TEST_PREMISES_NAME,
    apCode = "code",
    legacyApCode = "legacyApCode",
    localAuthorityAreaName = "localAuthName",
  )

  private fun staticStaffMember() = StaffMemberFactory()
    .withSurname("sur")
    .withForenames("for")
    .withStaffCode("code")
    .withUsername("username")
    .produce()

  private fun <T : Cas1DomainEventPayload> domainEventDetailsWrapper(details: T) = Cas1DomainEventEnvelopeFactory<T>()
    .withId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
    .withDetails(details)
    .withTimestamp(Instant.parse(CREATED_AT))
    .produce()

  @Test
  fun `CAS1 SAR API should return expected data`() {
    setupTestData()
    asserter.assertApiDataMatchesFixture(
      crn = TEST_CRN,
      fromDate = TEST_FROM_DATE,
      toDate = TEST_TO_DATE,
    )
  }

  @Test
  fun `CAS1 SAR report should render as expected`() {
    setupTestData()
    asserter.assertReportMatchesFixture(
      crn = TEST_CRN,
      fromDate = TEST_FROM_DATE,
      toDate = TEST_TO_DATE,
    )
  }
}
