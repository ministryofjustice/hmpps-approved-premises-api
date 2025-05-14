package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.context.ApplicationEventPublisher
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyMode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SendEmailRequestedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.LoggerExtension
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException

class EmailNotificationServiceTest {
  private val mockNormalNotificationClient = mockk<NotificationClient>()
  private val mockGuestListNotificationClient = mockk<NotificationClient>()
  private val mockApplicationEventPublisher = mockk<ApplicationEventPublisher>()
  private val mockSentryService = mockk<SentryService>()

  @RegisterExtension
  var loggerExtension: LoggerExtension = LoggerExtension()

  @Nested
  inner class SendEmail {

    @Test
    fun `sendEmail NotifyMode DISABLED does not send an email`() {
      val emailNotificationService = createService(NotifyMode.DISABLED)

      every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit

      emailNotificationService.sendEmail(
        recipientEmailAddress = "test@here.com",
        templateId = "f3d78814-383f-4b5f-a681-9bd3ab912888",
        personalisation = mapOf(
          "name" to "Jim",
          "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444",
        ),
      )

      verify { mockGuestListNotificationClient wasNot Called }
      verify { mockNormalNotificationClient wasNot Called }

      loggerExtension.assertContains("Email sending is disabled")
    }

    @Test
    fun `sendEmail NotifyMode DISABLED still raises a Send Email Requested event`() {
      val emailNotificationService = createService(NotifyMode.DISABLED)

      every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit

      emailNotificationService.sendEmail(
        recipientEmailAddress = "test@here.com",
        templateId = "f3d78814-383f-4b5f-a681-9bd3ab912888",
        personalisation = mapOf(
          "name" to "Jim",
          "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444",
        ),
      )

      verify {
        mockApplicationEventPublisher.publishEvent(
          SendEmailRequestedEvent(
            EmailRequest(
              "test@here.com",
              "f3d78814-383f-4b5f-a681-9bd3ab912888",
              mapOf(
                "name" to "Jim",
                "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444",
              ),
            ),
          ),
        )
      }
    }

    @Test
    fun `sendEmail NotifyMode TEST_AND_GUEST_LIST sends email using the guest list`() {
      val emailNotificationService = createService(NotifyMode.TEST_AND_GUEST_LIST)

      every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit

      val templateId = "f3d78814-383f-4b5f-a681-9bd3ab912888"
      val personalisation = mapOf(
        "name" to "Jim",
        "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444",
      )

      every {
        mockGuestListNotificationClient.sendEmail(
          "f3d78814-383f-4b5f-a681-9bd3ab912888",
          "test@here.com",
          personalisation,
          null,
          null,
        )
      } returns mockk()

      emailNotificationService.sendEmail(
        recipientEmailAddress = "test@here.com",
        templateId = templateId,
        personalisation = personalisation,
      )

      verify(exactly = 1) {
        mockGuestListNotificationClient.sendEmail(
          "f3d78814-383f-4b5f-a681-9bd3ab912888",
          "test@here.com",
          personalisation,
          null,
          null,
        )
      }

      verify { mockNormalNotificationClient wasNot Called }
    }

    @Test
    fun `sendEmail NotifyMode ENABLED sends email using the normal client`() {
      val emailNotificationService = createService(NotifyMode.ENABLED)

      val templateId = "f3d78814-383f-4b5f-a681-9bd3ab912888"
      val personalisation = mapOf(
        "name" to "Jim",
        "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444",
      )

      every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit
      every {
        mockNormalNotificationClient.sendEmail(
          "f3d78814-383f-4b5f-a681-9bd3ab912888",
          "test@here.com",
          personalisation,
          null,
          null,
        )
      } returns mockk()

      emailNotificationService.sendEmail(
        recipientEmailAddress = "test@here.com",
        templateId = templateId,
        personalisation = personalisation,
      )

      verify(exactly = 1) {
        mockNormalNotificationClient.sendEmail(
          "f3d78814-383f-4b5f-a681-9bd3ab912888",
          "test@here.com",
          personalisation,
          null,
          null,
        )
      }

      verify { mockGuestListNotificationClient wasNot Called }
    }

    @Test
    fun `log the email if logging is enabled, CAS1`() {
      val emailNotificationService = createService(NotifyMode.ENABLED, logEmails = true)

      val templateId = Cas1NotifyTemplates.APPLICATION_SUBMITTED
      val personalisation = mapOf(
        "name" to "Jim",
        "assessmentUrl" to "someUrlValue",
      )

      every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit
      every { mockNormalNotificationClient.sendEmail(any(), any(), any(), any(), any()) } returns mockk()

      emailNotificationService.sendEmail(
        recipientEmailAddress = "test@here.com",
        templateId = templateId,
        personalisation = personalisation,
        replyToEmailId = "theReplyToId",
      )

      loggerExtension.assertContains(
        "Sending email with template APPLICATION_SUBMITTED (c9944bd8-63c4-473c-8dce-b3636e47d3dd) " +
          "to user test@here.com with replyToId theReplyToId. Personalisation is {name=Jim, assessmentUrl=someUrlValue}",
      )
    }

    @Test
    fun `log the email if logging is enabled, CAS2`() {
      val emailNotificationService = createService(NotifyMode.ENABLED, logEmails = true)

      val templateId = Cas2NotifyTemplates.cas2NoteAddedForAssessor
      val personalisation = mapOf(
        "name" to "Jeff",
        "assessmentUrl" to "aUrlValue",
      )

      every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit
      every { mockNormalNotificationClient.sendEmail(any(), any(), any(), any(), any()) } returns mockk()

      emailNotificationService.sendEmail(
        recipientEmailAddress = "test@here.com",
        templateId = templateId,
        personalisation = personalisation,
        replyToEmailId = "theReplyToId",
      )

      loggerExtension.assertContains(
        "Sending email with template cas2NoteAddedForAssessor (0d646bf0-d40f-4fe7-aa74-dd28b10d04f1) " +
          "to user test@here.com with replyToId theReplyToId. Personalisation is {name=Jeff, assessmentUrl=aUrlValue}",
      )
    }

    @Test
    fun `don't log the email if logging disabled`() {
      val emailNotificationService = createService(NotifyMode.ENABLED, logEmails = false)

      val templateId = Cas1NotifyTemplates.APPLICATION_SUBMITTED
      val personalisation = mapOf(
        "name" to "Jim",
        "assessmentUrl" to "someUrlValue",
      )

      every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit
      every { mockNormalNotificationClient.sendEmail(any(), any(), any(), any(), any()) } returns mockk()

      emailNotificationService.sendEmail(
        recipientEmailAddress = "test@here.com",
        templateId = templateId,
        personalisation = personalisation,
        replyToEmailId = "theReplyToId",
      )

      loggerExtension.assertNoLogs()
    }

    @Test
    fun `sendEmail logs an error if the notification fails`() {
      val exception = NotificationClientException("oh dear")
      val emailNotificationService = createService(NotifyMode.ENABLED)

      val templateId = Cas1NotifyTemplates.BOOKING_MADE
      val personalisation = mapOf(
        "name" to "Jim",
        "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444",
      )

      every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit
      every {
        mockNormalNotificationClient.sendEmail(
          Cas1NotifyTemplates.BOOKING_MADE,
          "test@here.com",
          personalisation,
          null,
          null,
        )
      } throws exception

      every { mockSentryService.captureException(any()) } returns Unit

      emailNotificationService.sendEmail(
        recipientEmailAddress = "test@here.com",
        templateId = templateId,
        personalisation = personalisation,
      )

      loggerExtension.assertError("Unable to send template BOOKING_MADE (1e3d2ee2-250e-4755-af38-80d24cdc3480) to user test@here.com", exception)
    }

    @ParameterizedTest
    @CsvSource(
      quoteCharacter = '\'',
      textBlock = """
    'Status code: 400 {"errors":[{"error":"BadRequestError","message":"Missing personalisation: applicationUrl"}],"status_code":400}', true
    'Status code: 400 {"errors":[{"error":"BadRequestError","message":"Missing personalisation: additionalDatesSet"}],"status_code":400}', true
    '"Unable to send template xyz to user test@here.com',true
    'Status code: 400 {"errors":[{"error":"BadRequestError","message":"Something unexpected"}],"status_code":400}', true
    'Status code: 400 {"errors":[{"error":"BadRequestError","message":"Can\\u2019t send to this recipient using a team-only API key"}],"status_code":400}', false
    'Status code: 400 {"errors":[{"error":"ValidationError","message":"email_address Not a valid email address"}],"status_code":400}',false""",
    )
    fun `sendEmail logs exception in sentry unless in ignore list`(
      message: String,
      shouldRaiseInSentry: Boolean,
    ) {
      val exception = NotificationClientException(message)
      val emailNotificationService = createService(NotifyMode.ENABLED)

      val templateId = "f3d78814-383f-4b5f-a681-9bd3ab912888"
      val personalisation = mapOf(
        "name" to "Jim",
        "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444",
      )

      every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit
      every {
        mockNormalNotificationClient.sendEmail(
          "f3d78814-383f-4b5f-a681-9bd3ab912888",
          "test@here.com",
          personalisation,
          null,
          null,
        )
      } throws exception

      every { mockSentryService.captureException(any()) } returns Unit

      emailNotificationService.sendEmail(
        recipientEmailAddress = "test@here.com",
        templateId = templateId,
        personalisation = personalisation,
      )

      if (shouldRaiseInSentry) {
        verify { mockSentryService.captureException(exception) }
      } else {
        verify { mockSentryService wasNot Called }
      }
    }
  }

  @Nested
  inner class SendEmails {

    @Test
    fun `sendEmails sends an email for each recipient, filtering out duplicates()`() {
      val emailNotificationService = createService(NotifyMode.ENABLED)

      val templateId = "f3d78814-383f-4b5f-a681-9bd3ab912888"
      val personalisation = mapOf(
        "name" to "Jim",
        "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444",
      )

      every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit
      every {
        mockNormalNotificationClient.sendEmail(any(), any(), any(), any(), any())
      } returns mockk()

      emailNotificationService.sendEmails(
        recipientEmailAddresses = setOf("test1@here.com", "test2@here.com"),
        templateId = templateId,
        personalisation = personalisation,
      )

      verify(exactly = 1) {
        mockNormalNotificationClient.sendEmail(
          "f3d78814-383f-4b5f-a681-9bd3ab912888",
          "test1@here.com",
          personalisation,
          null,
          null,
        )
      }

      verify(exactly = 1) {
        mockNormalNotificationClient.sendEmail(
          "f3d78814-383f-4b5f-a681-9bd3ab912888",
          "test2@here.com",
          personalisation,
          null,
          null,
        )
      }

      verify { mockGuestListNotificationClient wasNot Called }
    }
  }

  @Nested
  inner class WithReplyToEmailId {
    val templateId = "f3d78814-383f-4b5f-a681-9bd3ab912888"
    val replyToEmailId = "263a4a1e-a5b5-4287-9c09-64f0a236f3a9"
    val email = "test@example.com"
    val personalisation = mapOf(
      "name" to "Jim",
      "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444",
    )

    @Nested
    inner class FeatureFlagNotifyEnabled {
      @Test
      fun `sendEmail sends replyToEmailId using the normal client`() {
        val emailNotificationService = createService(NotifyMode.ENABLED)

        every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit
        every {
          mockNormalNotificationClient.sendEmail(
            "f3d78814-383f-4b5f-a681-9bd3ab912888",
            email,
            personalisation,
            null,
            replyToEmailId,
          )
        } returns mockk()

        emailNotificationService.sendEmail(
          recipientEmailAddress = email,
          templateId = templateId,
          personalisation = personalisation,
          replyToEmailId = replyToEmailId,
        )

        verify(exactly = 1) {
          mockNormalNotificationClient.sendEmail(
            "f3d78814-383f-4b5f-a681-9bd3ab912888",
            email,
            personalisation,
            null,
            replyToEmailId,
          )
        }
      }
    }

    @Nested
    inner class FeatureFlagTestAndGuestListEnabled {
      @Test
      fun `sendEmail sends replyToEmailId using the guest client`() {
        val emailNotificationService = createService(NotifyMode.TEST_AND_GUEST_LIST)

        every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit
        every {
          mockGuestListNotificationClient.sendEmail(
            "f3d78814-383f-4b5f-a681-9bd3ab912888",
            email,
            personalisation,
            null,
            replyToEmailId,
          )
        } returns mockk()

        emailNotificationService.sendEmail(
          recipientEmailAddress = email,
          templateId = templateId,
          personalisation = personalisation,
          replyToEmailId = replyToEmailId,
        )

        verify(exactly = 1) {
          mockGuestListNotificationClient.sendEmail(
            "f3d78814-383f-4b5f-a681-9bd3ab912888",
            email,
            personalisation,
            null,
            replyToEmailId,
          )
        }

        verify(exactly = 0) { mockGuestListNotificationClient.sendEmail(any(), any(), any(), any()) }
      }
    }
  }

  private fun createService(notifyMode: NotifyMode, logEmails: Boolean = false): EmailNotificationService {
    val service = EmailNotificationService(
      notifyConfig = NotifyConfig().apply {
        this.mode = notifyMode
        this.logEmails = logEmails
      },
      normalNotificationClient = mockNormalNotificationClient,
      guestListNotificationClient = mockGuestListNotificationClient,
      applicationEventPublisher = mockApplicationEventPublisher,
      sentryService = mockSentryService,
    )
    return service
  }
}
