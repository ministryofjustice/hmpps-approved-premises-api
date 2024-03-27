package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

@Service
class Cas1ApplicationEmailService(
  val emailNotifier: EmailNotifier,
  private val notifyConfig: NotifyConfig,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application-timeline}") private val applicationTimelineUrlTemplate: UrlTemplate,
  @Value("\${feature-flags.cas1-aps530-withdrawal-email-improvements}") private val aps530WithdrawalEmailImprovements: Boolean,
) {

  fun applicationSubmitted(
    application: ApprovedPremisesApplicationEntity,
  ) {
    application.createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.applicationSubmitted,
        personalisation = mapOf(
          "name" to application.createdByUser.name,
          "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
          "crn" to application.crn,
        ),
      )
    }
  }

  fun applicationWithdrawn(
    application: ApprovedPremisesApplicationEntity,
    withdrawingUser: UserEntity,
  ) {
    val applicationCreatedByUser = application.createdByUser

    val templateId = if (aps530WithdrawalEmailImprovements) {
      notifyConfig.templates.applicationWithdrawnV2
    } else {
      notifyConfig.templates.applicationWithdrawn
    }

    applicationCreatedByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = templateId,
        personalisation = mapOf(
          "crn" to application.crn,
          "applicationTimelineUrl" to applicationTimelineUrlTemplate.resolve("applicationId", application.id.toString()),
          "withdrawnBy" to withdrawingUser.name,
        ),
      )
    }
  }
}
