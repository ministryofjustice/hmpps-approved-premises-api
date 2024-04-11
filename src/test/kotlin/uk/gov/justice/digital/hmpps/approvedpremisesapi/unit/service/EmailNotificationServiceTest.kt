package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.slf4j.Logger
import org.springframework.context.ApplicationEventPublisher
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyMode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SendEmailRequestedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException

class EmailNotificationServiceTest {
  private val mockNormalNotificationClient = mockk<NotificationClient>()
  private val mockGuestListNotificationClient = mockk<NotificationClient>()
  private val mockApplicationEventPublisher = mockk<ApplicationEventPublisher>()
  private val mockSentryService = mockk<SentryService>()
  private val logger = mockk<Logger>()

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
    fun `sendEmail logs an error if the notification fails`() {
      val exception = NotificationClientException("oh dear")
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

      every { logger.error(any<String>(), any()) } returns Unit

      emailNotificationService.sendEmail(
        recipientEmailAddress = "test@here.com",
        templateId = templateId,
        personalisation = personalisation,
      )

      verify {
        logger.error("Unable to send template $templateId to user test@here.com", exception)
      }
    }

    @ParameterizedTest
    @CsvSource(
      quoteCharacter = '\'',
      textBlock = """
    'Status code: 400 {"errors":[{"error":"BadRequestError","message":"Missing personalisation: applicationUrl"}],"status_code":400}', true
    'Status code: 400 {"errors":[{"error":"BadRequestError","message":"Missing personalisation: additionalDatesSet"}],"status_code":400}', true
    'Status code: 400 {"errors":[{"error":"BadRequestError","message":"Can\\u2019t send to this recipient using a team-only API key"}],"status_code":400}', false
    'Status code: 400 {"errors":[{"error":"ValidationError","message":"email_address Not a valid email address"}],"status_code":400}',false""",
    )
    fun `sendEmail logs exception in sentry if there is a personalisation error`(
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
      every { logger.error(any<String>(), any()) } returns Unit

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

        verify { mockGuestListNotificationClient.sendEmail(any(), any(), any(), any()) wasNot Called }
      }
    }
  }

  private fun createService(notifyMode: NotifyMode): EmailNotificationService {
    val service = EmailNotificationService(
      notifyConfig = NotifyConfig().apply {
        mode = notifyMode
      },
      normalNotificationClient = mockNormalNotificationClient,
      guestListNotificationClient = mockGuestListNotificationClient,
      applicationEventPublisher = mockApplicationEventPublisher,
      sentryService = mockSentryService,
    )
    service.log = logger

    every { logger.info(any<String>()) } returns Unit

    return service
  }
}
