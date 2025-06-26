package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

@Service
class Cas1PlacementApplicationEmailService(
  private val emailNotifier: Cas1EmailNotifier,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application-timeline}") private val applicationTimelineUrlTemplate: UrlTemplate,
) {

  fun placementApplicationSubmitted(placementApplication: PlacementApplicationEntity) {
    val createdByUser = placementApplication.createdByUser
    createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = Cas1NotifyTemplates.PLACEMENT_REQUEST_SUBMITTED_V2,
        personalisation = getCommonPersonalisation(placementApplication),
        application = placementApplication.application,
      )
    }
  }

  fun placementApplicationAllocated(placementApplication: PlacementApplicationEntity) {
    val createdByUser = placementApplication.createdByUser
    createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = Cas1NotifyTemplates.PLACEMENT_REQUEST_ALLOCATED_V2,
        personalisation = getCommonPersonalisation(placementApplication),
        application = placementApplication.application,
      )
    }
  }

  fun placementApplicationAccepted(placementApplication: PlacementApplicationEntity) {
    val createdByUser = placementApplication.createdByUser
    createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = Cas1NotifyTemplates.PLACEMENT_REQUEST_DECISION_ACCEPTED_V2,
        personalisation = getCommonPersonalisation(placementApplication),
        application = placementApplication.application,
      )
    }
  }

  fun placementApplicationRejected(placementApplication: PlacementApplicationEntity) {
    val createdByUser = placementApplication.createdByUser
    createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = Cas1NotifyTemplates.PLACEMENT_REQUEST_DECISION_REJECTED_V2,
        personalisation = getCommonPersonalisation(placementApplication),
        application = placementApplication.application,
      )
    }
  }

  fun placementApplicationWithdrawn(
    placementApplication: PlacementApplicationEntity,
    wasBeingAssessedBy: UserEntity?,
    withdrawalTriggeredBy: WithdrawalTriggeredBy,
  ) {
    val personalisation = getCommonPersonalisation(placementApplication)

    personalisation["withdrawnBy"] = withdrawalTriggeredBy.getName()

    val template = Cas1NotifyTemplates.PLACEMENT_REQUEST_WITHDRAWN_V2

    emailNotifier.sendEmails(
      recipientEmailAddresses = placementApplication.interestedPartiesEmailAddresses(),
      templateId = template,
      personalisation = personalisation,
      application = placementApplication.application,
    )

    val assessorEmail = wasBeingAssessedBy?.email
    assessorEmail?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = template,
        personalisation = personalisation,
        application = placementApplication.application,
      )
    }
  }

  private fun getCommonPersonalisation(placementApplication: PlacementApplicationEntity): MutableMap<String, String?> {
    val application = placementApplication.application

    return mutableMapOf(
      "crn" to application.crn,
      "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
      "applicationTimelineUrl" to applicationTimelineUrlTemplate.resolve("applicationId", application.id.toString()),
      "applicationArea" to application.apArea?.name,
      "startDate" to placementApplication.placementDates()?.expectedArrival.toString(),
      "endDate" to placementApplication.placementDates()?.expectedDeparture().toString(),
      "additionalDatesSet" to "no",
    )
  }
}
