package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyMode
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException
import kotlin.reflect.full.memberProperties

@Service
class EmailNotificationService(
  private val notifyConfig: NotifyConfig,
  @Qualifier("normalNotificationClient") private val normalNotificationClient: NotificationClient?,
  @Qualifier("guestListNotificationClient") private val guestListNotificationClient: NotificationClient?,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val sentryService: SentryService,
) : EmailNotifier {
  var log: Logger = LoggerFactory.getLogger(this::class.java)

  companion object {
    fun resolveTemplateName(templateId: String) = Cas2NotifyTemplates::class.memberProperties
      .firstOrNull { it.get(Cas2NotifyTemplates) == templateId }?.name
      ?: Cas1NotifyTemplates::class.memberProperties
        .firstOrNull { it.getter.call() == templateId }?.name
  }

  val errorSuppressionList = listOf(
    // full message is 'Can`t send to this recipient using a team-only API key'. Have excluded part
    // with non-ascii characters to avoid complications
    "this recipient using a team-only API key",
    "Not a valid email address",
  )

  override fun sendEmail(
    recipientEmailAddress: String,
    templateId: String,
    personalisation: Map<String, *>,
    replyToEmailId: String?,
  ) {
    val emailRequest = EmailRequest(recipientEmailAddress, templateId, personalisation, replyToEmailId)
    applicationEventPublisher.publishEvent(SendEmailRequestedEvent(emailRequest))

    if (notifyConfig.logEmails) {
      logEmail(emailRequest)
    }

    try {
      if (notifyConfig.mode == NotifyMode.DISABLED) {
        log.info("Email sending is disabled")
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
      val templateName = resolveTemplateName(templateId)
      log.error("Unable to send template $templateName ($templateId) to user $recipientEmailAddress", notificationClientException)

      notificationClientException.message?.let { exceptionMessage ->
        val suppress = errorSuppressionList.any { exceptionMessage.lowercase().contains(it.lowercase()) }

        if (!suppress) {
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
    recipientEmailAddresses: Set<String>,
    templateId: String,
    personalisation: Map<String, *>,
    replyToEmailId: String?,
  ) = recipientEmailAddresses.forEach { sendEmail(it, templateId, personalisation, replyToEmailId) }

  private fun logEmail(emailRequest: EmailRequest) {
    val templateId = emailRequest.templateId
    val templateName = resolveTemplateName(templateId)

    log.info(
      "Sending email with template $templateName ($templateId) to user ${emailRequest.email} " +
        "with replyToId ${emailRequest.replyToEmailId}. Personalisation is ${emailRequest.personalisation}",
    )
  }
}

interface EmailNotifier {
  fun sendEmail(recipientEmailAddress: String, templateId: String, personalisation: Map<String, *>, replyToEmailId: String? = null)

  fun sendEmails(recipientEmailAddresses: Set<String>, templateId: String, personalisation: Map<String, *>, replyToEmailId: String? = null)

  fun sendCas2Email(recipientEmailAddress: String, templateId: String, personalisation: Map<String, *>)
}

data class EmailRequest(
  val email: String,
  val templateId: String,
  val personalisation: Map<String, *>,
  val replyToEmailId: String? = null,
)

data class SendEmailRequestedEvent(val request: EmailRequest)
