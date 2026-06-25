package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService

@Service
class Cas2EmailService(
  private val emailNotificationService: EmailNotificationService,
  private val notifyConfig: NotifyConfig,
) {

  fun sendEmail(
    recipientEmailAddress: String,
    templateId: String,
    personalisation: Map<String, *>,
    cas2Application: Cas2ApplicationEntity,
  ) {
    emailNotificationService.sendEmail(
      recipientEmailAddress = recipientEmailAddress,
      templateId = templateId,
      personalisation = personalisation,
      replyToEmailId = notifyConfig.emailAddresses.cas2ReplyToId,
      reference = cas2Application.id.toString(),
    )
  }
}
