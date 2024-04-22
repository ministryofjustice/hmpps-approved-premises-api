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
  @Value("\${url-templates.frontend.application-timeline}") private val applicationTimelineUrlTemplate: UrlTemplate,
) {

  fun placementApplicationSubmitted(placementApplication: PlacementApplicationEntity) {
    val createdByUser = placementApplication.createdByUser
    createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.placementRequestSubmittedV2,
        personalisation = getCommonPersonalisation(placementApplication),
      )
    }
  }

  fun placementApplicationAllocated(placementApplication: PlacementApplicationEntity) {
    val createdByUser = placementApplication.createdByUser
    createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.placementRequestAllocatedV2,
        personalisation = getCommonPersonalisation(placementApplication),
      )
    }
  }

  fun placementApplicationAccepted(placementApplication: PlacementApplicationEntity) {
    val createdByUser = placementApplication.createdByUser
    createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.placementRequestDecisionAcceptedV2,
        personalisation = getCommonPersonalisation(placementApplication),
      )
    }
  }

  fun placementApplicationRejected(placementApplication: PlacementApplicationEntity) {
    val createdByUser = placementApplication.createdByUser
    createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.placementRequestDecisionRejectedV2,
        personalisation = getCommonPersonalisation(placementApplication),
      )
    }
  }

  fun placementApplicationWithdrawn(
    placementApplication: PlacementApplicationEntity,
    wasBeingAssessedBy: UserEntity?,
    withdrawalTriggeredBy: WithdrawalTriggeredBy,
  ) {
    val personalisation = getCommonPersonalisation(placementApplication)

    if (withdrawalTriggeredBy is WithdrawalTriggeredByUser) {
      personalisation["withdrawnBy"] = withdrawalTriggeredBy.user.name
    }

    val template = notifyConfig.templates.placementRequestWithdrawnV2

    emailNotifier.sendEmails(
      recipientEmailAddresses = placementApplication.interestedPartiesEmailAddresses(),
      templateId = template,
      personalisation = personalisation,
    )

    val assessorEmail = wasBeingAssessedBy?.email
    assessorEmail?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = template,
        personalisation = personalisation,
      )
    }
  }

  private fun getCommonPersonalisation(placementApplication: PlacementApplicationEntity): MutableMap<String, String?> {
    val application = placementApplication.application
    val dates = placementApplication.placementDates

    return mutableMapOf(
      "crn" to application.crn,
      "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
      "applicationTimelineUrl" to applicationTimelineUrlTemplate.resolve("applicationId", application.id.toString()),
      "applicationArea" to application.apArea?.name,
      "startDate" to dates.getOrNull(0)?.expectedArrival.toString(),
      "endDate" to dates.getOrNull(0)?.expectedDeparture().toString(),
      "additionalDatesSet" to if (dates.size > 1) "yes" else "no",
    )
  }
}
