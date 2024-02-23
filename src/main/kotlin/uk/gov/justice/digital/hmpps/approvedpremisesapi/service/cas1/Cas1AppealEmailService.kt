package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

@Service
class Cas1AppealEmailService(
  private val emailNotificationService: EmailNotifier,
  private val notifyConfig: NotifyConfig,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
) {
  fun appealSuccess(application: ApplicationEntity, appeal: AppealEntity) {
    appealSuccessRecipients(application, appeal).forEach { emailAddress ->
      sendAppealSuccessEmailToEmailAddress(emailAddress, application)
    }
  }

  fun appealFailed(application: ApplicationEntity) {
    application.createdByUser.email?.let { emailAddress ->
      emailNotificationService.sendEmail(
        recipientEmailAddress = emailAddress,
        templateId = notifyConfig.templates.appealReject,
        personalisation = mapOf(
          "crn" to application.crn,
          "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
        ),
      )
    }
  }

  private fun sendAppealSuccessEmailToEmailAddress(emailAddress: String, application: ApplicationEntity) {
    emailNotificationService.sendEmail(
      recipientEmailAddress = emailAddress,
      templateId = notifyConfig.templates.appealSuccess,
      personalisation = mapOf(
        "crn" to application.crn,
        "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
      ),
    )
  }

  private fun appealSuccessRecipients(application: ApplicationEntity, appeal: AppealEntity): List<String> = listOfNotNull(
    application.createdByUser.email,
    appeal.createdBy.email,
  )
}
