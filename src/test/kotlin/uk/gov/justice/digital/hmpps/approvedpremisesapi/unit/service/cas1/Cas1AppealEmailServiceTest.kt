package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AppealEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AppealEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockCas1EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime

object Cas1AppealEmailServiceTestConstants {
  const val APPLICANT_EMAIL = "applicant@test.com"
  const val APPEAL_ARBITRATOR_EMAIL = "arbitrator@test.com"
  const val CRN = "CRN123"
}

class Cas1AppealEmailServiceTest {
  private val notifyConfig = NotifyConfig()
  private val mockEmailNotificationService = MockCas1EmailNotificationService()

  val service = Cas1AppealEmailService(
    mockEmailNotificationService,
    notifyConfig,
    applicationUrlTemplate = UrlTemplate("http://frontend/applications/#id"),
  )

  @BeforeEach
  fun beforeEach() {
    mockEmailNotificationService.reset()
  }

  @Nested
  inner class AppealSuccess {
    @Test
    fun `appealSuccess doesn't send an email if the user does not have an email address`() {
      val (application, appeal) = createApplicationAndAppeal(null, null)

      service.appealSuccess(application, appeal)

      mockEmailNotificationService.assertEmailRequestCount(0)
    }

    @Test
    fun `appealSuccess sends an email to the applicant, but not the arbitrator if the applicant has an email address, but the arbitrator doesn't`() {
      val (application, appeal) = createApplicationAndAppeal(Cas1AppealEmailServiceTestConstants.APPLICANT_EMAIL, null)

      service.appealSuccess(application, appeal)

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        Cas1AppealEmailServiceTestConstants.APPLICANT_EMAIL,
        notifyConfig.templates.appealSuccess,
        expectedPersonalisationForAppealSuccess(application),
        application,
      )
    }

    @Test
    fun `appealSuccess sends an email to the arbitrator, but not the applicant if the arbitrator has an email address, but the applicant doesn't`() {
      val (application, appeal) = createApplicationAndAppeal(null, Cas1AppealEmailServiceTestConstants.APPEAL_ARBITRATOR_EMAIL)

      service.appealSuccess(application, appeal)

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        Cas1AppealEmailServiceTestConstants.APPEAL_ARBITRATOR_EMAIL,
        notifyConfig.templates.appealSuccess,
        expectedPersonalisationForAppealSuccess(application),
        application,
      )
    }

    @Test
    fun `appealSuccess sends an email to the arbitrator and applicant when both users have an email address`() {
      val (application, appeal) = createApplicationAndAppeal(
        Cas1AppealEmailServiceTestConstants.APPLICANT_EMAIL,
        Cas1AppealEmailServiceTestConstants.APPEAL_ARBITRATOR_EMAIL,
      )

      service.appealSuccess(application, appeal)

      mockEmailNotificationService.assertEmailRequestCount(2)
      mockEmailNotificationService.assertEmailRequested(
        Cas1AppealEmailServiceTestConstants.APPLICANT_EMAIL,
        notifyConfig.templates.appealSuccess,
        expectedPersonalisationForAppealSuccess(application),
        application,
      )
      mockEmailNotificationService.assertEmailRequested(
        Cas1AppealEmailServiceTestConstants.APPEAL_ARBITRATOR_EMAIL,
        notifyConfig.templates.appealSuccess,
        expectedPersonalisationForAppealSuccess(application),
        application,
      )
    }

    private fun expectedPersonalisationForAppealSuccess(application: ApprovedPremisesApplicationEntity) = mapOf(
      "crn" to application.crn,
      "applicationUrl" to "http://frontend/applications/${application.id}",
    )
  }

  @Nested
  inner class AppealFailed {
    @Test
    fun `appealFailed does not send an email if the applicant doesn't have an email address`() {
      val application = createApplication(null)

      service.appealFailed(application)

      mockEmailNotificationService.assertEmailRequestCount(0)
    }

    @Test
    fun `appealFailed sends an email when the applicant has an email address`() {
      val application = createApplication(Cas1AppealEmailServiceTestConstants.APPLICANT_EMAIL)

      service.appealFailed(application)

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        Cas1AppealEmailServiceTestConstants.APPLICANT_EMAIL,
        notifyConfig.templates.appealReject,
        expectedPersonalisationForAppealFailed(application),
        application,
      )
    }

    private fun expectedPersonalisationForAppealFailed(application: ApprovedPremisesApplicationEntity) = mapOf(
      "crn" to application.crn,
      "applicationUrl" to "http://frontend/applications/${application.id}",
    )
  }

  private fun createApplication(
    applicantEmail: String?,
  ): ApprovedPremisesApplicationEntity {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(applicantEmail)
      .produce()

    return ApprovedPremisesApplicationEntityFactory()
      .withCrn(Cas1AppealEmailServiceTestConstants.CRN)
      .withCreatedByUser(applicant)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()
  }

  private fun createApplicationAndAppeal(
    applicantEmail: String?,
    arbitratorEmail: String?,
  ): Pair<ApprovedPremisesApplicationEntity, AppealEntity> {
    val arbitrator = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(arbitratorEmail)
      .produce()

    val application = createApplication(applicantEmail)

    val appeal = AppealEntityFactory()
      .withCreatedBy(arbitrator)
      .produce()

    return Pair(application, appeal)
  }
}
