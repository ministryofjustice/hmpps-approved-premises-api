package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.sar

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.CasSarFixtureAsserter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.time.LocalDate

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
    const val TEST_PROBATION_DELIVERY_UNIT = "SAR-TEST-PDU"
    const val TEST_PREMISES_NAME = "SAR-TEST-PREMISES"
    val TEST_FROM_DATE: LocalDate = LocalDate.of(2019, 1, 1)
    val TEST_TO_DATE: LocalDate = LocalDate.of(2024, 12, 31)

    const val EXPECTED_API_RESPONSE_PATH = "/sar/cas3-expected-api-response.json"
    const val EXPECTED_REPORT_PATH = "/sar/cas3-expected-report.html"
    const val GENERATED_API_RESPONSE_FILENAME = "cas3-sar-api-response.json.log"
    const val GENERATED_REPORT_FILENAME = "cas3-sar-report.html.log"
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
    }
    val assessor = userEntityFactory.produceAndPersist {
      withName(TEST_ASSESSOR_NAME)
      withProbationRegion(givenAProbationRegion())
    }
    val temporaryAccommodationApplication = temporaryAccommodationApplicationEntity(
      offenderDetails,
      user,
      dutyToReferLocalAuthorityAreaName = TEST_DUTY_TO_REFER_AREA,
      probationRegionName = TEST_PROBATION_REGION,
      probationDeliveryUnitName = "SAR-TEST-PDU-1",
      data = CAS3_APPLICATION_DATA,
      document = "null",
    )
    temporaryAccommodationApplicationEntity(
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
    domainEventEntity(offenderDetails, temporaryAccommodationApplication.id, temporaryAccommodationAssessment.id, user.id, DomainEventType.CAS3_REFERRAL_SUBMITTED, ServiceName.temporaryAccommodation)
  }

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
