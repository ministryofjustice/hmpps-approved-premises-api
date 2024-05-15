package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1ApplicationUserDetailsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1ApplicationEmailServiceTest.TestConstants.CASE_MANAGER_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockCas1EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime

class Cas1ApplicationEmailServiceTest {

  private object TestConstants {
    const val APPLICANT_EMAIL = "applicantEmail@test.com"
    const val APPLICANT_NAME = "the applicant name"
    const val CASE_MANAGER_EMAIL = "caseManagerEmail@test.com"
    const val CRN = "CRN123"
  }

  private val notifyConfig = NotifyConfig()
  private val mockEmailNotificationService = MockCas1EmailNotificationService()

  private val service = Cas1ApplicationEmailService(
    emailNotifier = mockEmailNotificationService,
    notifyConfig = notifyConfig,
    applicationUrlTemplate = UrlTemplate("http://frontend/applications/#id"),
    applicationTimelineUrlTemplate = UrlTemplate("http://frontend/applications/#applicationId?tab=timeline"),
  )

  @Nested
  inner class ApplicationSubmitted {

    @Test
    fun `applicationSubmitted doesnt send email to applicant if no email addresses defined`() {
      val applicant = createUser(emailAddress = null)

      val application = createApplicationForApplicant(applicant)

      service.applicationSubmitted(application)

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @Test
    fun `applicationSubmitted sends an email to applicant if email addresses defined`() {
      val applicant = createUser(
        name = TestConstants.APPLICANT_NAME,
        emailAddress = TestConstants.APPLICANT_EMAIL,
      )

      val application = createApplicationForApplicant(applicant)

      service.applicationSubmitted(application)

      mockEmailNotificationService.assertEmailRequestCount(1)

      val personalisation = mapOf(
        "name" to TestConstants.APPLICANT_NAME,
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "crn" to TestConstants.CRN,
      )

      mockEmailNotificationService.assertEmailRequested(
        TestConstants.APPLICANT_EMAIL,
        notifyConfig.templates.applicationSubmitted,
        personalisation,
        application,
      )
    }
  }

  @Nested
  inner class ApplicationWithdrawn {

    @Test
    fun `applicationWithdrawn doesnt send email to applicant if no email addresses defined`() {
      val applicant = createUser(emailAddress = null)
      val withdrawingUser = createUser(emailAddress = null)

      val application = createApplicationForApplicant(applicant)

      service.applicationWithdrawn(application, withdrawingUser)

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @Test
    fun `applicationWithdrawn doesnt send email to applicant if application not submitted`() {
      val applicant = createUser(emailAddress = TestConstants.APPLICANT_EMAIL)
      val withdrawingUser = createUser(emailAddress = null)

      val application = createApplicationForApplicant(applicant, submittedAt = null)

      service.applicationWithdrawn(application, withdrawingUser)

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @Test
    fun `applicationWithdrawn sends email to applicant if email addresses defined`() {
      val applicant = createUser(emailAddress = TestConstants.APPLICANT_EMAIL)
      val withdrawingUser = createUser(name = "the withdrawing user")

      val application = createApplicationForApplicant(applicant)

      service.applicationWithdrawn(application, withdrawingUser)

      mockEmailNotificationService.assertEmailRequestCount(1)

      val personalisation = mapOf(
        "crn" to TestConstants.CRN,
        "applicationTimelineUrl" to "http://frontend/applications/${application.id}?tab=timeline",
        "withdrawnBy" to "the withdrawing user",
      )

      mockEmailNotificationService.assertEmailRequested(
        TestConstants.APPLICANT_EMAIL,
        notifyConfig.templates.applicationWithdrawnV2,
        personalisation,
        application,
      )
    }

    @Test
    fun `applicationWithdrawn sends a email to applicant and case manager if case manager not applicant`() {
      val applicant = createUser(emailAddress = TestConstants.APPLICANT_EMAIL)
      val withdrawingUser = createUser(name = "the withdrawing user")

      val application = createApplicationForApplicant(applicant, caseManagerNotApplicant = true)

      service.applicationWithdrawn(application, withdrawingUser)

      mockEmailNotificationService.assertEmailRequestCount(2)

      val personalisation = mapOf(
        "crn" to TestConstants.CRN,
        "applicationTimelineUrl" to "http://frontend/applications/${application.id}?tab=timeline",
        "withdrawnBy" to "the withdrawing user",
      )

      mockEmailNotificationService.assertEmailRequested(
        TestConstants.APPLICANT_EMAIL,
        notifyConfig.templates.applicationWithdrawnV2,
        personalisation,
        application,
      )
      mockEmailNotificationService.assertEmailRequested(
        CASE_MANAGER_EMAIL,
        notifyConfig.templates.applicationWithdrawnV2,
        personalisation,
        application,
      )
    }
  }

  private fun createUser(
    emailAddress: String? = null,
    name: String = "default name",
  ) = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .withEmail(emailAddress)
    .withName(name)
    .produce()

  private fun createApplicationForApplicant(
    applicant: UserEntity,
    caseManagerNotApplicant: Boolean = false,
    submittedAt: OffsetDateTime? = OffsetDateTime.now(),
  ) = ApprovedPremisesApplicationEntityFactory()
    .withCrn(TestConstants.CRN)
    .withCreatedByUser(applicant)
    .withSubmittedAt(submittedAt)
    .withCaseManagerIsNotApplicant(caseManagerNotApplicant)
    .withCaseManagerUserDetails(
      if (caseManagerNotApplicant) {
        Cas1ApplicationUserDetailsEntityFactory().withEmailAddress(CASE_MANAGER_EMAIL).produce()
      } else {
        null
      },
    )
    .withApArea(ApAreaEntityFactory().withName("test area").produce())
    .produce()
}
