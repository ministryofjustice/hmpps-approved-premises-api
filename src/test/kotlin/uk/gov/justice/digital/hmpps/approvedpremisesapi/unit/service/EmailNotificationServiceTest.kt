package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyMode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NotifyGuestListUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NotifyGuestListUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.service.notify.NotificationClient

class EmailNotificationServiceTest {
  private val mockNormalNotificationClient = mockk<NotificationClient>()
  private val mockGuestListNotificationClient = mockk<NotificationClient>()
  private val mockNotifyGuestListUserRepository = mockk<NotifyGuestListUserRepository>()

  @Test
  fun `sendEmail does not send an email if feature flag is set to DISABLED`() {
    val emailNotificationService = createServiceWithConfig {
      mode = NotifyMode.DISABLED
    }

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    emailNotificationService.sendEmail(
      user = user,
      templateId = "f3d78814-383f-4b5f-a681-9bd3ab912888",
      personalisation = mapOf(
        "name" to "Jim",
        "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444"
      )
    )

    verify(exactly = 0) { mockGuestListNotificationClient.sendEmail(any(), any(), any(), any()) }
    verify(exactly = 0) { mockNormalNotificationClient.sendEmail(any(), any(), any(), any()) }
  }

  @Test
  fun `sendEmail sends email using the guest list client if feature flag is set to TEST_AND_GUEST_LIST and user is in guest list`() {
    val emailNotificationService = createServiceWithConfig {
      mode = NotifyMode.TEST_AND_GUEST_LIST
    }

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    every { mockNotifyGuestListUserRepository.findByIdOrNull(user.id) } returns NotifyGuestListUserEntity(user.id)

    val templateId = "f3d78814-383f-4b5f-a681-9bd3ab912888"
    val personalisation = mapOf(
      "name" to "Jim",
      "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444"
    )

    every {
      mockGuestListNotificationClient.sendEmail(
        "f3d78814-383f-4b5f-a681-9bd3ab912888",
        user.email,
        personalisation,
        null
      )
    } returns mockk()

    emailNotificationService.sendEmail(
      user = user,
      templateId = templateId,
      personalisation = personalisation
    )

    verify(exactly = 1) {
      mockGuestListNotificationClient.sendEmail(
        "f3d78814-383f-4b5f-a681-9bd3ab912888",
        user.email,
        personalisation,
        null
      )
    }

    verify(exactly = 0) { mockNormalNotificationClient.sendEmail(any(), any(), any(), any()) }
  }

  @Test
  fun `sendEmail sends email using the normal client if feature flag is set to TEST_AND_GUEST_LIST and user is not in guest list`() {
    val emailNotificationService = createServiceWithConfig {
      mode = NotifyMode.TEST_AND_GUEST_LIST
    }

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    every { mockNotifyGuestListUserRepository.findByIdOrNull(user.id) } returns null

    val templateId = "f3d78814-383f-4b5f-a681-9bd3ab912888"
    val personalisation = mapOf(
      "name" to "Jim",
      "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444"
    )

    every {
      mockNormalNotificationClient.sendEmail(
        "f3d78814-383f-4b5f-a681-9bd3ab912888",
        user.email,
        personalisation,
        null
      )
    } returns mockk()

    emailNotificationService.sendEmail(
      user = user,
      templateId = templateId,
      personalisation = personalisation
    )

    verify(exactly = 1) {
      mockNormalNotificationClient.sendEmail(
        "f3d78814-383f-4b5f-a681-9bd3ab912888",
        user.email,
        personalisation,
        null
      )
    }

    verify(exactly = 0) { mockGuestListNotificationClient.sendEmail(any(), any(), any(), any()) }
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
      "assessmentUrl" to "https://frontend/assessment/73eff3e8-d2f0-434f-a776-4f975b891444"
    )

    every {
      mockNormalNotificationClient.sendEmail(
        "f3d78814-383f-4b5f-a681-9bd3ab912888",
        user.email,
        personalisation,
        null
      )
    } returns mockk()

    emailNotificationService.sendEmail(
      user = user,
      templateId = templateId,
      personalisation = personalisation
    )

    verify(exactly = 1) {
      mockNormalNotificationClient.sendEmail(
        "f3d78814-383f-4b5f-a681-9bd3ab912888",
        user.email,
        personalisation,
        null
      )
    }

    verify(exactly = 0) { mockGuestListNotificationClient.sendEmail(any(), any(), any(), any()) }

    verify(exactly = 0) { mockNotifyGuestListUserRepository.findByIdOrNull(user.id) }
  }

  private fun createServiceWithConfig(configBlock: NotifyConfig.() -> Unit) = EmailNotificationService(
    notifyConfig = NotifyConfig().apply(configBlock),
    normalNotificationClient = mockNormalNotificationClient,
    guestListNotificationClient = mockGuestListNotificationClient,
    guestListUserRepository = mockNotifyGuestListUserRepository
  )
}
