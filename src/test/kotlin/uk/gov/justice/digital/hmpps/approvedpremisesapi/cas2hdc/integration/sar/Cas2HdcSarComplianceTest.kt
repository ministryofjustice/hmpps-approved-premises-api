package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.integration.sar

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2EventCohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.sar.Cas1SarComplianceTest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.sar.Cas2SarTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.events.Cas2ApplicationStatusUpdatedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.events.Cas2ApplicationSubmittedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.events.Cas2StatusFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.events.ExternalUserFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.CasSarFixtureAsserter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Per-service SAR compliance test for CAS2.
 *
 * Cross-service SAR infrastructure (Flyway schema, JPA entity snapshot, template
 * endpoint smoke tests) lives in `SarIntegrationTest` — they only need to run
 * once for the whole app, since all four CAS services share one DB and one
 * template file.
 *
 * This class verifies CAS2 HDC's slice end-to-end against CAS2 HDC-specific fixtures
 * via [CasSarFixtureAsserter].
 */
class Cas2HdcSarComplianceTest : Cas2SarTestBase() {

  companion object {
    const val TEST_CRN = "X320742"
    const val TEST_NOMS_NUMBER = "A1234BD"
    const val TEST_NOMIS_USER_NAME = "SAR-TEST-NOMIS-USER"
    const val TEST_EXTERNAL_USER_NAME = "SAR-TEST-EXTERNAL-USER"
    const val TEST_ASSESSOR_NAME = "SAR-TEST-ASSESSOR"
    const val TEST_NACRO_REFERRAL_ID = "0000000001"
    const val TEST_REFERRING_PRISON_CODE = "ABC"
    const val TEST_TELEPHONE_NUMBER = "0123456"
    val TEST_FROM_DATE: LocalDate = LocalDate.of(2019, 1, 1)
    val TEST_TO_DATE: LocalDate = LocalDate.of(2024, 12, 31)

    const val EXPECTED_API_RESPONSE_PATH = "/sar/cas2-hdc-expected-api-response.json"
    const val EXPECTED_REPORT_PATH = "/sar/cas2-hdc-expected-report.html"
    const val GENERATED_API_RESPONSE_FILENAME = "cas2-hdc-sar-api-response.json.log"
    const val GENERATED_REPORT_FILENAME = "cas2-hdc-sar-report.html.log"
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
    cas2NoteRepository.deleteAll()
    cas2StatusUpdateDetailRepository.deleteAll()
    cas2StatusUpdateRepository.deleteAll()
    cas2AssessmentRepository.deleteAll()
    cas2ApplicationRepository.deleteAll()
  }

  private fun setupTestData() {
    val (offenderDetails, _) = givenAnOffender(
      offenderDetailsConfigBlock = {
        withCrn(TEST_CRN)
        withNomsNumber(TEST_NOMS_NUMBER)
      },
    )
    val user = cas2NomisUserEntity(Cas2ServiceOrigin.HDC, name = TEST_NOMIS_USER_NAME)
    val assessor = cas2ExternalUserEntity(name = TEST_EXTERNAL_USER_NAME)
    val application = cas2ApplicationEntity(
      offenderDetails,
      user,
      Cas2ServiceOrigin.HDC,
      referringPrisonCode = TEST_REFERRING_PRISON_CODE,
      telephoneNumber = TEST_TELEPHONE_NUMBER,
      data = CAS2_APPLICATION_DATA,
      document = "null",
    )
    cas2ApplicationEntity(
      offenderDetails,
      user,
      Cas2ServiceOrigin.HDC,
      referringPrisonCode = TEST_REFERRING_PRISON_CODE,
      telephoneNumber = TEST_TELEPHONE_NUMBER,
      data = CAS2_APPLICATION_DATA,
      document = CAS2_APPLICATION_DOCUMENT,
    )
    val assessment = cas2AssessmentEntity(
      application,
      Cas2ServiceOrigin.HDC,
      assessorName = TEST_ASSESSOR_NAME,
      nacroReferralId = TEST_NACRO_REFERRAL_ID,
    )

    cas2ApplicationNoteEntity(application, assessment, user)
    val statusUpdate = cas2StatusUpdateEntity(application, assessment, assessor)
    cas2StatusUpdateDetailEntity(statusUpdate)

    val domainEventParams = DomainEventBuilderParams(
      offenderDetails,
      application.id,
      assessment.id,
      null,
      ServiceName.cas2,
    )

    domainEventEntity(
      domainEventParams,
      DomainEventType.CAS2_APPLICATION_SUBMITTED,
      data = Cas2ApplicationSubmittedEvent(
        id = UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"),
        timestamp = Instant.parse("2021-07-31T00:00:00.00Z"),
        eventType = EventType.applicationSubmitted,
        eventDetails = Cas2ApplicationSubmittedEventDetailsFactory()
          .withApplicationId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withApplicationUrl("url")
          .withPersonReference(PersonReference(Cas1SarComplianceTest.TEST_CRN, Cas1SarComplianceTest.TEST_NOMS_NUMBER))
          .withReferringPrisonCode("ref")
          .withPreferredAreas("preferred_areas")
          .withHdcEligibilityDate(LocalDate.of(2024, 1, 1))
          .withConditionalReleaseDate(LocalDate.of(2024, 1, 2))
          .withSubmittedAt(Instant.parse("2025-04-01T10:15:30.00Z"))
          .withSubmittedByStaffMember(staticStaffMember())
          .withCohort(Cas2EventCohort("code", "name"))
          .produce(),
      ),
    )

    domainEventEntity(
      domainEventParams,
      DomainEventType.CAS2_APPLICATION_STATUS_UPDATED,
      data = Cas2ApplicationStatusUpdatedEvent(
        id = UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"),
        timestamp = Instant.parse("2021-07-31T00:00:00.00Z"),
        eventType = EventType.applicationStatusUpdated,
        eventDetails = Cas2ApplicationStatusUpdatedEventDetailsFactory()
          .withApplicationId(UUID.fromString("72f972cc-9e74-4a8c-b398-becb4c14b4c4"))
          .withApplicationUrl("url")
          .withPersonReference(PersonReference(Cas1SarComplianceTest.TEST_CRN, Cas1SarComplianceTest.TEST_NOMS_NUMBER))
          .withUpdatedAt(Instant.parse("2025-04-01T10:15:30.00Z"))
          .withNewStatus(Cas2StatusFactory().produce())
          .withStatus(Cas2StatusFactory().produce())
          .withUpdatedBy(ExternalUserFactory().withName("name").withUsername("username").produce())
          .withCohort(Cas2EventCohort("code", "name"))
          .produce(),
      ),
    )
  }

  private fun staticStaffMember() = Cas2StaffMember(
    staffIdentifier = 1L,
    name = "the name",
    username = "the username",
    cas2StaffIdentifier = "id",
    usertype = Cas2StaffMember.Usertype.delius,
  )

  @Test
  fun `CAS2 HDC SAR API should return expected data`() {
    setupTestData()
    asserter.assertApiDataMatchesFixture(
      crn = TEST_CRN,
      fromDate = TEST_FROM_DATE,
      toDate = TEST_TO_DATE,
    )
  }

  @Test
  fun `CAS2 HDC SAR report should render as expected`() {
    setupTestData()
    asserter.assertReportMatchesFixture(
      crn = TEST_CRN,
      fromDate = TEST_FROM_DATE,
      toDate = TEST_TO_DATE,
    )
  }
}
