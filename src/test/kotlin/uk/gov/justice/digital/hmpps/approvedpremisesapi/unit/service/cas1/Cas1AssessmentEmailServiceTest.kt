package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockEmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.util.UUID

object Cas1AssessmentEmailServiceTestConstants {
  const val ALLOCATED_USER_EMAIL = "applicant@test.com"
  const val DEALLOCATED_USER_EMAIL = "deallocated@test.com"
  const val CRN = "CRN123"
}

class Cas1AssessmentEmailServiceTest {
  private val notifyConfig = NotifyConfig()
  private val mockEmailNotificationService = MockEmailNotificationService()

  val service = Cas1AssessmentEmailService(
    mockEmailNotificationService,
    notifyConfig,
    assessmentUrlTemplate = UrlTemplate("http://frontend/assessments/#id"),
  )

  @BeforeEach
  fun beforeEach() {
    mockEmailNotificationService.reset()
  }

  @Test
  fun `assessmentAllocated sends an email to a user if they have an email address`() {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(Cas1AssessmentEmailServiceTestConstants.ALLOCATED_USER_EMAIL)
      .produce()
    val assessmentID = UUID.randomUUID()

    service.assessmentAllocated(applicant, assessmentID, Cas1AssessmentEmailServiceTestConstants.CRN)

    mockEmailNotificationService.assertEmailRequestCount(1)
    mockEmailNotificationService.assertEmailRequested(
      Cas1AssessmentEmailServiceTestConstants.ALLOCATED_USER_EMAIL,
      notifyConfig.templates.assessmentAllocated,
      mapOf(
        "name" to applicant.name,
        "crn" to Cas1AssessmentEmailServiceTestConstants.CRN,
        "assessmentUrl" to "http://frontend/assessments/$assessmentID",
      ),
    )
  }

  @Test
  fun `assessmentAllocated does not send an email to a user if they do not have an email address`() {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(null)
      .produce()
    val assessmentID = UUID.randomUUID()

    service.assessmentAllocated(applicant, assessmentID, Cas1AssessmentEmailServiceTestConstants.CRN)
    mockEmailNotificationService.assertEmailRequestCount(0)
  }

  @Test
  fun `assessmentDeallocated sends an email when the user has an email address`() {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(Cas1AssessmentEmailServiceTestConstants.DEALLOCATED_USER_EMAIL)
      .produce()
    val assessmentID = UUID.randomUUID()

    service.assessmentDeallocated(applicant, assessmentID, Cas1AssessmentEmailServiceTestConstants.CRN)

    mockEmailNotificationService.assertEmailRequestCount(1)
    mockEmailNotificationService.assertEmailRequested(
      Cas1AssessmentEmailServiceTestConstants.DEALLOCATED_USER_EMAIL,
      notifyConfig.templates.assessmentDeallocated,
      mapOf(
        "name" to applicant.name,
        "crn" to Cas1AssessmentEmailServiceTestConstants.CRN,
        "assessmentUrl" to "http://frontend/assessments/$assessmentID",
      ),
    )
  }

  @Test
  fun `assessmentDeallocated does not send an email to a user if they do not have an email address`() {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(null)
      .produce()
    val assessmentID = UUID.randomUUID()

    service.assessmentDeallocated(applicant, assessmentID, Cas1AssessmentEmailServiceTestConstants.CRN)
    mockEmailNotificationService.assertEmailRequestCount(0)
  }
}
