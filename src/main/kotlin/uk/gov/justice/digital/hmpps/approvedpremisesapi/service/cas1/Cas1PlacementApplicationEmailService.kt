package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

@Service
class Cas1PlacementApplicationEmailService(
  private val emailNotifier: EmailNotifier,
  private val notifyConfig: NotifyConfig,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${feature-flags.cas1-use-new-withdrawal-logic}") private val sendNewWithdrawalNotifications: Boolean,
  @Value("\${notify.send-placement-request-notifications}") private val sendPlacementRequestNotifications: Boolean,
) {

  fun placementApplicationSubmitted(placementApplication: PlacementApplicationEntity) {
    if (!sendPlacementRequestNotifications) {
      return
    }

    val createdByUser = placementApplication.createdByUser
    createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.placementRequestSubmitted,
        personalisation = mapOf(
          "crn" to placementApplication.application.crn,
        ),
      )
    }
  }

  fun placementApplicationWithdrawn(placementApplication: PlacementApplicationEntity, wasBeingAssessedBy: UserEntity?) {
    if (!sendNewWithdrawalNotifications) {
      return
    }

    val application = placementApplication.application

    val personalisation = mapOf(
      "crn" to application.crn,
      "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
    )

    val createdByUserEmail = placementApplication.createdByUser.email
    createdByUserEmail?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.placementRequestWithdrawn,
        personalisation = personalisation,
      )
    }

    val assessorEmail = wasBeingAssessedBy?.email
    assessorEmail?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.placementRequestWithdrawn,
        personalisation = personalisation,
      )
    }
  }
}
