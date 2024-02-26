package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

@Service
class Cas1PlacementRequestEmailService(
  private val emailNotifier: EmailNotifier,
  private val notifyConfig: NotifyConfig,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${feature-flags.cas1-use-new-withdrawal-logic}") private val sendNewWithdrawalNotifications: Boolean,
) {
  fun placementRequestWithdrawn(placementRequest: PlacementRequestEntity) {
    if (!sendNewWithdrawalNotifications) {
      return
    }

    val application = placementRequest.application

    val personalisation = mapOf(
      "crn" to application.crn,
      "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
    )

    if (placementRequest.isForApplicationsArrivalDate()) {
      val applicant = application.createdByUser
      applicant.email?.let { applicantEmail ->
        emailNotifier.sendEmail(
          recipientEmailAddress = applicantEmail,
          templateId = notifyConfig.templates.matchRequestWithdrawn,
          personalisation = personalisation,
        )
      }
    }

    if (!placementRequest.hasActiveBooking()) {
      val area = application.apArea
      area?.emailAddress?.let { cruEmail ->
        emailNotifier.sendEmail(
          recipientEmailAddress = cruEmail,
          templateId = notifyConfig.templates.matchRequestWithdrawn,
          personalisation = personalisation,
        )
      }
    }
  }
}
