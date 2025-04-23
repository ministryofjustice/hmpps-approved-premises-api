package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

@Service
class Cas1ApplicationEmailService(
  private val emailNotifier: Cas1EmailNotifier,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application-timeline}") private val applicationTimelineUrlTemplate: UrlTemplate,
) {

  fun applicationSubmitted(
    application: ApprovedPremisesApplicationEntity,
  ) {
    application.createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = Cas1NotifyTemplates.APPLICATION_SUBMITTED,
        personalisation = mapOf(
          "name" to application.createdByUser.name,
          "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
          "crn" to application.crn,
        ),
        application = application,
      )
    }
  }

  fun applicationWithdrawn(
    application: ApprovedPremisesApplicationEntity,
    withdrawingUser: UserEntity,
  ) {
    if (!application.isSubmitted()) {
      return
    }

    emailNotifier.sendEmails(
      recipientEmailAddresses = application.interestedPartiesEmailAddresses(),
      templateId = Cas1NotifyTemplates.APPLICATION_WITHDRAWN_V2,
      personalisation = mapOf(
        "crn" to application.crn,
        "applicationTimelineUrl" to applicationTimelineUrlTemplate.resolve("applicationId", application.id.toString()),
        "withdrawnBy" to withdrawingUser.name,
      ),
      application = application,
    )
  }
}
