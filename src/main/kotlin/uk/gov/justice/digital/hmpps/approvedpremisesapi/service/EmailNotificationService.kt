package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyMode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NotifyGuestListUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException

@Service
class EmailNotificationService(
  private val notifyConfig: NotifyConfig,
  @Qualifier("normalNotificationClient") private val normalNotificationClient: NotificationClient?,
  @Qualifier("guestListNotificationClient") private val guestListNotificationClient: NotificationClient?,
  private val guestListUserRepository: NotifyGuestListUserRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun sendEmail(user: UserEntity, templateId: String, personalisation: Map<String, *>) {
    try {
      if (notifyConfig.mode == NotifyMode.DISABLED) {
        log.info("Email sending is disabled - would have sent template $templateId to user ${user.id}")
        return
      }

      if (notifyConfig.mode == NotifyMode.TEST_AND_GUEST_LIST && guestListUserRepository.findByIdOrNull(user.id) != null) {
        guestListNotificationClient!!.sendEmail(templateId, user.email, personalisation, null)
      } else {
        normalNotificationClient!!.sendEmail(templateId, user.email, personalisation, null)
      }
    } catch (notificationClientException: NotificationClientException) {
      log.error("Unable to send email", notificationClientException)
    }
  }
}
