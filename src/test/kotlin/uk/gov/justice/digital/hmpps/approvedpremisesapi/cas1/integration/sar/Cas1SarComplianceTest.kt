package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.sar

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import java.time.LocalDate
import javax.sql.DataSource

class Cas1SarComplianceTest : Cas1SarTestBase() {

  @Autowired
  lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  @Autowired
  lateinit var dataSource: DataSource

  @Autowired
  lateinit var entityManager: EntityManager

  companion object {
    const val TEST_CRN = "X320741"
    const val TEST_NOMS_NUMBER = "A1234BC"
    const val TEST_ASSESSOR_NAME = "SAR-TEST-ASSESSOR"
    const val TEST_CASE_MANAGER_NAME = "SAR-TEST-CASE-MANAGER"
    val TEST_FROM_DATE: LocalDate = LocalDate.of(2019, 1, 1)
    val TEST_TO_DATE: LocalDate = LocalDate.of(2024, 12, 31)
  }

  private fun clearTestData() {
    appealTestRepository.deleteAll()
    approvedPremisesAssessmentRepository.deleteAll()
    approvedPremisesApplicationRepository.deleteAll()
  }

  private fun setupTestData() {
    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(TEST_CRN)
      .withNomsNumber(TEST_NOMS_NUMBER)
      .produce()
    val assessor = userEntityFactory.produceAndPersist {
      withName(TEST_ASSESSOR_NAME)
      withProbationRegion(givenAProbationRegion())
    }
    val application = approvedPremisesApplicationEntity(offenderDetails, TEST_CASE_MANAGER_NAME)
    val assessment = approvedPremisesAssessmentEntity(application, assessor)
    appealEntity(application, assessment)
  }

  @Nested
  inner class ApiDataTest : SarApiDataTest {
    @BeforeEach
    fun clearTESTData() {
      clearTestData()
    }

    override fun setupTestData() = this@Cas1SarComplianceTest.setupTestData()
    override fun getCrn() = TEST_CRN
    override fun getFromDate() = TEST_FROM_DATE
    override fun getToDate() = TEST_TO_DATE
    override fun getWebTestClientInstance() = webTestClient
    override fun getSarHelper() = sarIntegrationTestHelper
  }

  @Nested
  inner class ReportTest : SarReportTest {
    @BeforeEach
    fun clearTESTData() {
      clearTestData()
    }

    override fun setupTestData() = this@Cas1SarComplianceTest.setupTestData()
    override fun getCrn() = TEST_CRN
    override fun getFromDate() = TEST_FROM_DATE
    override fun getToDate() = TEST_TO_DATE
    override fun getWebTestClientInstance() = webTestClient
    override fun getSarHelper() = sarIntegrationTestHelper
  }
}
