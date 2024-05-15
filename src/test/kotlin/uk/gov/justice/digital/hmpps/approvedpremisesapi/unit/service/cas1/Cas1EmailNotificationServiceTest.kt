package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1EmailNotificationService
import java.util.UUID

class Cas1EmailNotificationServiceTest {

  private val emailNotificationService = mockk<EmailNotificationService>()

  private val service = Cas1EmailNotificationService(emailNotificationService)

  companion object {
    const val RECIPIENT_1 = "recipient1@somewhere.com"
    const val RECIPIENT_2 = "recipient2@somewhere.com"
    val TEMPLATE_ID = UUID.randomUUID().toString()
    val PERSONALISATION = mapOf(
      "field1" to "value1",
    )
  }

  val application = ApprovedPremisesApplicationEntityFactory().withDefaults().produce()

  @Test
  fun `sendEmail delegates to EmailNotificationService`() {
    every { emailNotificationService.sendEmail(RECIPIENT_1, TEMPLATE_ID, PERSONALISATION) } returns Unit

    service.sendEmail(
      RECIPIENT_1,
      TEMPLATE_ID,
      PERSONALISATION,
      application,
    )

    verify { emailNotificationService.sendEmail(RECIPIENT_1, TEMPLATE_ID, PERSONALISATION) }
  }

  @Test
  fun `sendEmails delegates to EmailNotificationService`() {
    every { emailNotificationService.sendEmails(setOf(RECIPIENT_1, RECIPIENT_2), TEMPLATE_ID, PERSONALISATION) } returns Unit

    service.sendEmails(
      setOf(RECIPIENT_1, RECIPIENT_2),
      TEMPLATE_ID,
      PERSONALISATION,
      application,
    )

    verify { emailNotificationService.sendEmails(setOf(RECIPIENT_1, RECIPIENT_2), TEMPLATE_ID, PERSONALISATION) }
  }
}
