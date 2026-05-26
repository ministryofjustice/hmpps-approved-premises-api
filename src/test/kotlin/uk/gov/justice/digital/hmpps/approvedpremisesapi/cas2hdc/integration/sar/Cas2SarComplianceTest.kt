package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.CasSarFixtureAsserter
import java.time.LocalDate

/**
 * Per-service SAR compliance test for CAS2.
 *
 * Cross-service SAR infrastructure (Flyway schema, JPA entity snapshot, template
 * endpoint smoke tests) lives in `SarIntegrationTest` — they only need to run
 * once for the whole app, since all four CAS services share one DB and one
 * template file.
 *
 * This class verifies CAS2's slice end-to-end against CAS2-specific fixtures
 * via [CasSarFixtureAsserter].
 */
class Cas2SarComplianceTest : Cas2SarTestBase() {

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

    const val EXPECTED_API_RESPONSE_PATH = "/sar/cas2-expected-api-response.json"
    const val EXPECTED_REPORT_PATH = "/sar/cas2-expected-report.html"
    const val GENERATED_API_RESPONSE_FILENAME = "cas2-sar-api-response.json.log"
    const val GENERATED_REPORT_FILENAME = "cas2-sar-report.html.log"
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
    domainEventEntity(offenderDetails, application.id, assessment.id, null, ServiceName.cas2)
  }

  @Test
  fun `CAS2 SAR API should return expected data`() {
    setupTestData()
    asserter.assertApiDataMatchesFixture(
      crn = TEST_CRN,
      fromDate = TEST_FROM_DATE,
      toDate = TEST_TO_DATE,
    )
  }

  @Test
  fun `CAS2 SAR report should render as expected`() {
    setupTestData()
    asserter.assertReportMatchesFixture(
      crn = TEST_CRN,
      fromDate = TEST_FROM_DATE,
      toDate = TEST_TO_DATE,
    )
  }
}
