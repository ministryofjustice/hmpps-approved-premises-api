package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

@Service
class Cas1AppealEmailService(
  private val emailNotifier: Cas1EmailNotifier,
  private val notifyConfig: NotifyConfig,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
) {
  fun appealSuccess(application: ApprovedPremisesApplicationEntity, appeal: AppealEntity) {
    appealSuccessRecipients(application, appeal).forEach { emailAddress ->
      sendAppealSuccessEmailToEmailAddress(emailAddress, application)
    }
  }

  fun appealFailed(application: ApprovedPremisesApplicationEntity) {
    application.createdByUser.email?.let { emailAddress ->
      emailNotifier.sendEmail(
        recipientEmailAddress = emailAddress,
        templateId = notifyConfig.templates.appealReject,
        personalisation = mapOf(
          "crn" to application.crn,
          "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
        ),
        application = application,
      )
    }
  }

  private fun sendAppealSuccessEmailToEmailAddress(emailAddress: String, application: ApprovedPremisesApplicationEntity) {
    emailNotifier.sendEmail(
      recipientEmailAddress = emailAddress,
      templateId = notifyConfig.templates.appealSuccess,
      personalisation = mapOf(
        "crn" to application.crn,
        "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
      ),
      application = application,
    )
  }

  private fun appealSuccessRecipients(application: ApprovedPremisesApplicationEntity, appeal: AppealEntity): List<String> = listOfNotNull(
    application.createdByUser.email,
    appeal.createdBy.email,
  )
}
