package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

@Service
class Cas1ChangeRequestEmailService(
  private val emailNotifier: Cas1EmailNotifier,
  @Value("\${url-templates.frontend.application-timeline}") private val applicationTimelineUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.cas1.cru-dashboard}") private val cruDashboardUrlTemplate: UrlTemplate,
) {

  fun placementAppealCreated(changeRequest: Cas1ChangeRequestEntity) {
    val application = changeRequest.placementRequest.application

    emailNotifier.sendEmails(
      recipientEmailAddresses = setOfNotNull(application.cruManagementArea?.emailAddress),
      templateId = Cas1NotifyTemplates.PLACEMENT_APPEAL_CREATED,
      personalisation = commonPersonalisation(changeRequest),
      application = application,
    )
  }

  fun placementAppealAccepted(changeRequest: Cas1ChangeRequestEntity) {
    val application = changeRequest.placementRequest.application

    val personalisation = commonPersonalisation(changeRequest)

    emailNotifier.sendEmails(
      recipientEmailAddresses =
      application.interestedPartiesEmailAddresses() +
        (changeRequest.placementRequest.placementApplication?.interestedPartiesEmailAddresses() ?: emptySet()),
      templateId = Cas1NotifyTemplates.PLACEMENT_APPEAL_ACCEPTED_FOR_APPLICANT,
      personalisation = personalisation,
      application = application,
    )

    emailNotifier.sendEmails(
      recipientEmailAddresses = setOfNotNull(
        changeRequest.spaceBooking.premises.emailAddress,
      ),
      templateId = Cas1NotifyTemplates.PLACEMENT_APPEAL_ACCEPTED_FOR_PREMISES,
      personalisation = personalisation,
      application = application,
    )
  }

  fun placementAppealRejected(changeRequest: Cas1ChangeRequestEntity) {
    val application = changeRequest.placementRequest.application

    emailNotifier.sendEmails(
      recipientEmailAddresses = setOfNotNull(
        changeRequest.spaceBooking.premises.emailAddress,
        application.cruManagementArea?.emailAddress,
      ),
      templateId = Cas1NotifyTemplates.PLACEMENT_APPEAL_REJECTED,
      personalisation = commonPersonalisation(changeRequest),
      application = application,
    )
  }

  private fun commonPersonalisation(changeRequest: Cas1ChangeRequestEntity): Map<String, String> {
    val application = changeRequest.placementRequest.application

    return mapOf(
      "apName" to changeRequest.spaceBooking.premises.name,
      "crn" to application.crn,
      "cruDashboardUrl" to cruDashboardUrlTemplate.resolve(),
      "applicationTimelineUrl" to applicationTimelineUrlTemplate.resolve("applicationId", application.id.toString()),
    )
  }
}
