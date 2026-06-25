package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2EmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService

class Cas2EmailServiceTest {
  private val mockEmailNotificationService = mockk<EmailNotificationService>(relaxed = true)
  private val mockNotifyConfig = mockk<NotifyConfig>()

  private val service = Cas2EmailService(
    emailNotificationService = mockEmailNotificationService,
    notifyConfig = mockNotifyConfig,
  )

  @Test
  fun `sendEmail calls the underlying EmailNotificationService with the correct parameters`() {
    val recipientEmailAddress = "test@example.com"
    val templateId = "template-id"
    val personalisation = mapOf("key" to "value")
    val user = Cas2UserEntityFactory().produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(user)
      .produce()

    val cas2ReplyToId = "reply-to-id"

    every { mockNotifyConfig.emailAddresses.cas2ReplyToId } returns cas2ReplyToId

    service.sendEmail(
      recipientEmailAddress = recipientEmailAddress,
      templateId = templateId,
      personalisation = personalisation,
      cas2Application = application,
    )

    verify(exactly = 1) {
      mockEmailNotificationService.sendEmail(
        recipientEmailAddress = recipientEmailAddress,
        templateId = templateId,
        personalisation = personalisation,
        replyToEmailId = cas2ReplyToId,
        reference = application.id.toString(),
      )
    }
  }
}
