package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyMode
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException

@Service
class EmailNotificationService(
  private val notifyConfig: NotifyConfig,
  @Qualifier("normalNotificationClient") private val normalNotificationClient: NotificationClient?,
  @Qualifier("guestListNotificationClient") private val guestListNotificationClient: NotificationClient?,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val sentryService: SentryService,
) : EmailNotifier {
  var log: Logger = LoggerFactory.getLogger(this::class.java)

  override fun sendEmail(
    recipientEmailAddress: String,
    templateId: String,
    personalisation: Map<String, *>,
    replyToEmailId: String?,
  ) {
    applicationEventPublisher.publishEvent(SendEmailRequestedEvent(EmailRequest(recipientEmailAddress, templateId, personalisation, replyToEmailId)))

    try {
      if (notifyConfig.mode == NotifyMode.DISABLED) {
        log.info("Email sending is disabled - would have sent template $templateId to user $recipientEmailAddress")
        return
      }

      if (notifyConfig.mode == NotifyMode.TEST_AND_GUEST_LIST) {
        guestListNotificationClient!!.sendEmail(
          templateId,
          recipientEmailAddress,
          personalisation,
          null,
          replyToEmailId,
        )
      } else {
        normalNotificationClient!!.sendEmail(
          templateId,
          recipientEmailAddress,
          personalisation,
          null,
          replyToEmailId,
        )
      }
    } catch (notificationClientException: NotificationClientException) {
      log.error("Unable to send template $templateId to user $recipientEmailAddress", notificationClientException)

      notificationClientException.message?.let { exceptionMessage ->
        if (exceptionMessage.contains("Missing personalisation")) {
          sentryService.captureException(notificationClientException)
        }
      }
    }
  }

  override fun sendCas2Email(
    recipientEmailAddress: String,
    templateId: String,
    personalisation: Map<String, *>,
  ) {
    sendEmail(
      recipientEmailAddress,
      templateId,
      personalisation,
      notifyConfig.emailAddresses.cas2ReplyToId,
    )
  }

  override fun sendEmails(
    recipientEmailAddresses: List<String>,
    templateId: String,
    personalisation: Map<String, *>,
    replyToEmailId: String?,
  ) = recipientEmailAddresses.toSet().forEach { sendEmail(it, templateId, personalisation, replyToEmailId) }
}

interface EmailNotifier {
  fun sendEmail(recipientEmailAddress: String, templateId: String, personalisation: Map<String, *>, replyToEmailId: String? = null)

  fun sendEmails(recipientEmailAddresses: List<String>, templateId: String, personalisation: Map<String, *>, replyToEmailId: String? = null)

  fun sendCas2Email(recipientEmailAddress: String, templateId: String, personalisation: Map<String, *>)
}

data class EmailRequest(val email: String, val templateId: String, val personalisation: Map<String, *>, val replyToEmailId: String? = null)

data class SendEmailRequestedEvent(val request: EmailRequest)
