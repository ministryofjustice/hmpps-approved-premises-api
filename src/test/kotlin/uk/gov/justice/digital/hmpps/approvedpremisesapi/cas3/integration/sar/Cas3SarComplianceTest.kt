package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.sar

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.CasSarFixtureAsserter
import java.time.LocalDate

/**
 * For general information on SARs and testing, see docs/how-to/subject_access_requests.md
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
    const val GENERATED_PDF_FILENAME = "cas3-sar-generated-report.pdf"
  }

  private val asserter by lazy {
    CasSarFixtureAsserter(
      sarHelper = sarIntegrationTestHelper,
      webTestClient = webTestClient,
      expectedApiResponseResourcePath = EXPECTED_API_RESPONSE_PATH,
      expectedReportResourcePath = EXPECTED_REPORT_PATH,
      generatedApiResponseFilename = GENERATED_API_RESPONSE_FILENAME,
      generatedReportFilename = GENERATED_REPORT_FILENAME,
      generatedPdfFilename = GENERATED_PDF_FILENAME,
    )
  }

  @BeforeEach
  fun clear() {
    assessmentReferralSystemNoteRepository.deleteAll()
    assessmentReferralUserNoteRepository.deleteAll()
    temporaryAccommodationAssessmentRepository.deleteAll()
    temporaryAccommodationApplicationRepository.deleteAll()
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
  }

  /**
   * These test will fail when the template or test data changes
   *
   * To regenerate the expected outputs run `./gradlew updateSarFixtures` and validate the updated files
   */
  @Test
  fun `CAS3 SAR API should return expected data`() {
    setupTestData()
    asserter.assertApiDataMatchesFixture(
      crn = TEST_CRN,
      fromDate = TEST_FROM_DATE,
      toDate = TEST_TO_DATE,
    )
  }

  /**
   * These test will fail when the template or test data changes
   *
   * To regenerate the expected outputs run `./gradlew updateSarFixtures` and validate the updated files
   */
  @Test
  fun `CAS3 SAR report should render as expected`() {
    setupTestData()
    asserter.assertReportMatchesFixture(
      crn = TEST_CRN,
      fromDate = TEST_FROM_DATE,
    )
  }
}
