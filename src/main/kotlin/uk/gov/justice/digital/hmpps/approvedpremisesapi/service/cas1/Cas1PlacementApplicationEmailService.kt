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
) {

  fun placementApplicationSubmitted(placementApplication: PlacementApplicationEntity) {
    val templateId = if (sendNewWithdrawalNotifications) {
      notifyConfig.templates.placementRequestSubmittedV2
    } else {
      notifyConfig.templates.placementRequestSubmitted
    }

    val createdByUser = placementApplication.createdByUser
    createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = templateId,
        personalisation = getPersonalisation(placementApplication),
      )
    }
  }

  fun placementApplicationAllocated(placementApplication: PlacementApplicationEntity) {
    val createdByUser = placementApplication.createdByUser
    createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.placementRequestAllocated,
        personalisation = getPersonalisation(placementApplication),
      )
    }
  }

  fun placementApplicationAccepted(placementApplication: PlacementApplicationEntity) {
    val createdByUser = placementApplication.createdByUser
    createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.placementRequestDecisionAccepted,
        personalisation = mapOf(
          "crn" to placementApplication.application.crn,
        ),
      )
    }
  }

  fun placementApplicationRejected(placementApplication: PlacementApplicationEntity) {
    val createdByUser = placementApplication.createdByUser
    createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.placementRequestDecisionRejected,
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

    val personalisation = getPersonalisation(placementApplication)

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

  private fun getPersonalisation(placementApplication: PlacementApplicationEntity): Map<String, String?> {
    val application = placementApplication.application
    val dates = placementApplication.placementDates

    return mapOf(
      "crn" to application.crn,
      "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
      "applicationArea" to application.apArea?.name,
      "startDate" to dates.getOrNull(0)?.expectedArrival.toString(),
      "endDate" to dates.getOrNull(0)?.expectedDeparture().toString(),
      "additionalDatesSet" to if (dates.size > 1) "yes" else "no",
    )
  }
}
