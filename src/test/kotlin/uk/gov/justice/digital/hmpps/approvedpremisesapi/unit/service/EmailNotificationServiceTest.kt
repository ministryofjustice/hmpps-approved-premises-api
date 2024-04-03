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
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyMode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NotifyGuestListUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NotifyGuestListUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SendEmailRequestedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException

class EmailNotificationServiceTest {
  private val mockNormalNotificationClient = mockk<NotificationClient>()
  private val mockGuestListNotificationClient = mockk<NotificationClient>()
  private val mockNotifyGuestListUserRepository = mockk<NotifyGuestListUserRepository>()
  private val mockApplicationEventPublisher = mockk<ApplicationEventPublisher>()
  private val mockSentryService = mockk<SentryService>()

  @Test
  fun `sendEmail does not send an email if feature flag is set to DISABLED`() {
    val emailNotificationService = createServiceWithConfig {
      mode = NotifyMode.DISABLED
    }

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit

    emailNotificationService.sendEmail(
      recipientEmailAddress = user.email!!,
      templateId = "f3d78814-383f-4b5f-a681-9bd3ab912888",
      personalisation = mapOf(
        "name" to "Jim",
        "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444",
      ),
    )

    verify(exactly = 0) { mockGuestListNotificationClient.sendEmail(any(), any(), any(), any(), any()) }
    verify(exactly = 0) { mockNormalNotificationClient.sendEmail(any(), any(), any(), any()) }
  }

  @Test
  fun `sendEmail raises a Send Email Requested event even if feature flag is set to DISABLED`() {
    val emailNotificationService = createServiceWithConfig {
      mode = NotifyMode.DISABLED
    }

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit

    emailNotificationService.sendEmail(
      recipientEmailAddress = user.email!!,
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
            user.email!!,
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
  fun `sendEmail sends email using the guest list client if feature flag is set to TEST_AND_GUEST_LIST and user is in guest list`() {
    val emailNotificationService = createServiceWithConfig {
      mode = NotifyMode.TEST_AND_GUEST_LIST
    }

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit
    every { mockNotifyGuestListUserRepository.findByIdOrNull(user.id) } returns NotifyGuestListUserEntity(user.id)

    val templateId = "f3d78814-383f-4b5f-a681-9bd3ab912888"
    val personalisation = mapOf(
      "name" to "Jim",
      "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444",
    )

    every {
      mockGuestListNotificationClient.sendEmail(
        "f3d78814-383f-4b5f-a681-9bd3ab912888",
        user.email,
        personalisation,
        null,
        null,
      )
    } returns mockk()

    if (user.email != null) {
      emailNotificationService.sendEmail(
        recipientEmailAddress = user.email!!,
        templateId = templateId,
        personalisation = personalisation,
      )
    }

    verify(exactly = 1) {
      mockGuestListNotificationClient.sendEmail(
        "f3d78814-383f-4b5f-a681-9bd3ab912888",
        user.email,
        personalisation,
        null,
        null,
      )
    }

    verify(exactly = 0) { mockNormalNotificationClient.sendEmail(any(), any(), any(), any()) }
  }

  @Test
  fun `sendEmail sends email using the normal client if feature flag is set to ENABLED, does not check if Guest List User`() {
    val emailNotificationService = createServiceWithConfig {
      mode = NotifyMode.ENABLED
    }

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val templateId = "f3d78814-383f-4b5f-a681-9bd3ab912888"
    val personalisation = mapOf(
      "name" to "Jim",
      "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444",
    )

    every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit
    every {
      mockNormalNotificationClient.sendEmail(
        "f3d78814-383f-4b5f-a681-9bd3ab912888",
        user.email,
        personalisation,
        null,
        null,
      )
    } returns mockk()

    if (user.email != null) {
      emailNotificationService.sendEmail(
        recipientEmailAddress = user.email!!,
        templateId = templateId,
        personalisation = personalisation,
      )
    }

    verify(exactly = 1) {
      mockNormalNotificationClient.sendEmail(
        "f3d78814-383f-4b5f-a681-9bd3ab912888",
        user.email,
        personalisation,
        null,
        null,
      )
    }

    verify(exactly = 0) { mockGuestListNotificationClient.sendEmail(any(), any(), any(), any(), any()) }

    verify(exactly = 0) { mockNotifyGuestListUserRepository.findByIdOrNull(user.id) }
  }

  @Test
  fun `sendEmail sends two emails if premises has email using the normal client if feature flag is set to ENABLED, does not check if Guest List User`() {
    val emailNotificationService = createServiceWithConfig {
      mode = NotifyMode.ENABLED
    }

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val templateId = "f3d78814-383f-4b5f-a681-9bd3ab912888"
    val personalisation = mapOf(
      "name" to "Jim",
      "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444",
    )

    every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit
    every {
      mockNormalNotificationClient.sendEmail(
        "f3d78814-383f-4b5f-a681-9bd3ab912888",
        any(),
        personalisation,
        null,
        null,
      )
    } returns mockk()
    if (user.email != null) {
      emailNotificationService.sendEmail(
        recipientEmailAddress = user.email!!,
        templateId = templateId,
        personalisation = personalisation,
      )
    }

    verify(exactly = 1) {
      mockNormalNotificationClient.sendEmail(
        "f3d78814-383f-4b5f-a681-9bd3ab912888",
        any(),
        personalisation,
        null,
        null,
      )
    }

    verify(exactly = 0) { mockGuestListNotificationClient.sendEmail(any(), any(), any(), any(), any()) }
    verify(exactly = 0) { mockNotifyGuestListUserRepository.findByIdOrNull(user.id) }
  }

  @Test
  fun `sendEmail logs an error if the notification fails`() {
    val logger = mockk<Logger>()
    val exception = NotificationClientException("oh dear")
    val emailNotificationService = createServiceWithConfig {
      mode = NotifyMode.ENABLED
    }
    emailNotificationService.log = logger

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val templateId = "f3d78814-383f-4b5f-a681-9bd3ab912888"
    val personalisation = mapOf(
      "name" to "Jim",
      "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444",
    )

    every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit
    every {
      mockNormalNotificationClient.sendEmail(
        "f3d78814-383f-4b5f-a681-9bd3ab912888",
        user.email,
        personalisation,
        null,
        null,
      )
    } throws exception

    every { logger.error(any<String>(), any()) } returns Unit

    emailNotificationService.sendEmail(
      recipientEmailAddress = user.email!!,
      templateId = templateId,
      personalisation = personalisation,
    )

    verify {
      logger.error("Unable to send template $templateId to user ${user.email}", exception)
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
    val logger = mockk<Logger>()
    val exception = NotificationClientException(message)
    val emailNotificationService = createServiceWithConfig {
      mode = NotifyMode.ENABLED
    }
    emailNotificationService.log = logger

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val templateId = "f3d78814-383f-4b5f-a681-9bd3ab912888"
    val personalisation = mapOf(
      "name" to "Jim",
      "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444",
    )

    every { mockApplicationEventPublisher.publishEvent(any(SendEmailRequestedEvent::class)) } returns Unit
    every {
      mockNormalNotificationClient.sendEmail(
        "f3d78814-383f-4b5f-a681-9bd3ab912888",
        user.email,
        personalisation,
        null,
        null,
      )
    } throws exception

    every { mockSentryService.captureException(any()) } returns Unit
    every { logger.error(any<String>(), any()) } returns Unit

    emailNotificationService.sendEmail(
      recipientEmailAddress = user.email!!,
      templateId = templateId,
      personalisation = personalisation,
    )

    if (shouldRaiseInSentry) {
      verify { mockSentryService.captureException(exception) }
    } else {
      verify { mockSentryService wasNot Called }
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
        val emailNotificationService = createServiceWithConfig {
          mode = NotifyMode.ENABLED
        }

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
        val emailNotificationService = createServiceWithConfig {
          mode = NotifyMode.TEST_AND_GUEST_LIST
        }

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

  private fun createServiceWithConfig(configBlock: NotifyConfig.() -> Unit) = EmailNotificationService(
    notifyConfig = NotifyConfig().apply(configBlock),
    normalNotificationClient = mockNormalNotificationClient,
    guestListNotificationClient = mockGuestListNotificationClient,
    applicationEventPublisher = mockApplicationEventPublisher,
    sentryService = mockSentryService,
  )
}
