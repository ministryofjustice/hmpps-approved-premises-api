package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotifier

@Service
class Cas1ApplicationEmailService(
  val emailNotifier: EmailNotifier,
  private val notifyConfig: NotifyConfig,
) {
  fun applicationWithdrawn(application: ApprovedPremisesApplicationEntity) {
    val applicationCreatedByUser = application.createdByUser

    applicationCreatedByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.applicationWithdrawn,
        personalisation = mapOf(
          "crn" to application.crn,
        ),
      )
    }
  }
}
