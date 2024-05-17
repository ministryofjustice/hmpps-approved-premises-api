package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1EmailNotificationService
import java.util.UUID

class Cas1EmailNotificationServiceTest {

  private val emailNotificationService = mockk<EmailNotificationService>()
  private val featureFlagService = mockk<FeatureFlagService>()

  private val service = Cas1EmailNotificationService(emailNotificationService, featureFlagService)

  companion object {
    const val RECIPIENT_1 = "recipient1@somewhere.com"
    const val RECIPIENT_2 = "recipient2@somewhere.com"
    const val NOTIFY_REPLY_TO_EMAIL_ID = "29226e7d-cdf7-44f2-b5c5-38de6ecf8df1"
    val TEMPLATE_ID = UUID.randomUUID().toString()
    val PERSONALISATION = mapOf(
      "field1" to "value1",
    )
  }

  val application = ApprovedPremisesApplicationEntityFactory()
    .withDefaults()
    .withApArea(
      ApAreaEntityFactory()
        .withNotifyReplyToEmailId(NOTIFY_REPLY_TO_EMAIL_ID)
        .produce(),
    )
    .produce()

  @Test
  fun `sendEmail delegates to EmailNotificationService use-cru-for-reply-to disabled`() {
    every { featureFlagService.getBooleanFlag("cas1-email-use-cru-for-reply-to", default = false) } returns false
    every { emailNotificationService.sendEmail(RECIPIENT_1, TEMPLATE_ID, PERSONALISATION, replyToEmailId = null) } returns Unit

    service.sendEmail(
      RECIPIENT_1,
      TEMPLATE_ID,
      PERSONALISATION,
      application,
    )

    verify { emailNotificationService.sendEmail(RECIPIENT_1, TEMPLATE_ID, PERSONALISATION, replyToEmailId = null) }
  }

  @Test
  fun `sendEmail delegates to EmailNotificationService use-cru-for-reply-to enabled`() {
    every { featureFlagService.getBooleanFlag("cas1-email-use-cru-for-reply-to", default = false) } returns true
    every { emailNotificationService.sendEmail(RECIPIENT_1, TEMPLATE_ID, PERSONALISATION, replyToEmailId = NOTIFY_REPLY_TO_EMAIL_ID) } returns Unit

    service.sendEmail(
      RECIPIENT_1,
      TEMPLATE_ID,
      PERSONALISATION,
      application,
    )

    verify { emailNotificationService.sendEmail(RECIPIENT_1, TEMPLATE_ID, PERSONALISATION, replyToEmailId = NOTIFY_REPLY_TO_EMAIL_ID) }
  }

  @Test
  fun `sendEmails delegates to EmailNotificationService use-cru-for-reply-to disabled`() {
    every { featureFlagService.getBooleanFlag("cas1-email-use-cru-for-reply-to", default = false) } returns false
    every { emailNotificationService.sendEmails(setOf(RECIPIENT_1, RECIPIENT_2), TEMPLATE_ID, PERSONALISATION, replyToEmailId = null) } returns Unit

    service.sendEmails(
      setOf(RECIPIENT_1, RECIPIENT_2),
      TEMPLATE_ID,
      PERSONALISATION,
      application,
    )

    verify { emailNotificationService.sendEmails(setOf(RECIPIENT_1, RECIPIENT_2), TEMPLATE_ID, PERSONALISATION, replyToEmailId = null) }
  }

  @Test
  fun `sendEmails delegates to EmailNotificationService use-cru-for-reply-to enabled`() {
    every { featureFlagService.getBooleanFlag("cas1-email-use-cru-for-reply-to", default = false) } returns true
    every { emailNotificationService.sendEmails(setOf(RECIPIENT_1, RECIPIENT_2), TEMPLATE_ID, PERSONALISATION, replyToEmailId = NOTIFY_REPLY_TO_EMAIL_ID) } returns Unit

    service.sendEmails(
      setOf(RECIPIENT_1, RECIPIENT_2),
      TEMPLATE_ID,
      PERSONALISATION,
      application,
    )

    verify { emailNotificationService.sendEmails(setOf(RECIPIENT_1, RECIPIENT_2), TEMPLATE_ID, PERSONALISATION, replyToEmailId = NOTIFY_REPLY_TO_EMAIL_ID) }
  }
}
