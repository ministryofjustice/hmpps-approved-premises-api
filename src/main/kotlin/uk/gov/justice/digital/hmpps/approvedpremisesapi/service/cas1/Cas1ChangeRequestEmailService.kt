package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlacementAppealAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlacementAppealCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlacementAppealRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlannedTransferRequestAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlannedTransferRequestCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlannedTransferRequestRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

@Service
class Cas1ChangeRequestEmailService(
  private val emailNotifier: Cas1EmailNotifier,
  @param:Value("\${url-templates.frontend.application-timeline}") private val applicationTimelineUrlTemplate: UrlTemplate,
  @param:Value("\${url-templates.frontend.cas1.cru-open-change-requests}") private val cruOpenChangeRequestsUrlTemplate: UrlTemplate,
  @param:Value("\${url-templates.frontend.cas1.cru-dashboard}") private val cruDashboardUrlTemplate: UrlTemplate,
  @param:Value("\${url-templates.frontend.cas1.space-booking-timeline}") private val spaceBookingTimelineUrlTemplate: UrlTemplate,
) {

  @EventListener
  fun placementAppealCreated(placementAppealCreated: PlacementAppealCreated) {
    val changeRequest = placementAppealCreated.changeRequest
    val application = changeRequest.placementRequest.application

    emailNotifier.sendEmails(
      recipientEmailAddresses = setOfNotNull(application.cruManagementArea?.emailAddress),
      templateId = Cas1NotifyTemplates.PLACEMENT_APPEAL_CREATED,
      personalisation = commonAppealPersonalisation(changeRequest),
      application = application,
    )
  }

  @EventListener
  fun placementAppealAccepted(placementAppealAccepted: PlacementAppealAccepted) {
    val changeRequest = placementAppealAccepted.changeRequest
    val application = changeRequest.placementRequest.application

    val personalisation = commonAppealPersonalisation(changeRequest)

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

  @EventListener
  fun placementAppealRejected(placementAppealRejected: PlacementAppealRejected) {
    val changeRequest = placementAppealRejected.changeRequest
    val application = changeRequest.placementRequest.application

    emailNotifier.sendEmails(
      recipientEmailAddresses = setOfNotNull(
        changeRequest.spaceBooking.premises.emailAddress,
        application.cruManagementArea?.emailAddress,
      ),
      templateId = Cas1NotifyTemplates.PLACEMENT_APPEAL_REJECTED,
      personalisation = commonAppealPersonalisation(changeRequest),
      application = application,
    )
  }

  @EventListener
  fun plannedTransferRequestCreated(plannedTransferRequestCreated: PlannedTransferRequestCreated) {
    val changeRequest = plannedTransferRequestCreated.changeRequest
    val application = changeRequest.placementRequest.application

    val fromPersonalisation = changeRequest.spaceBooking.toEmailBookingInfo(application).personalisationValues()

    val personalisation = mapOf(
      "fromPremisesName" to fromPersonalisation.premisesName,
      "crn" to application.crn,
      "cruOpenChangeRequestsUrl" to cruOpenChangeRequestsUrlTemplate.resolve(),
    )

    emailNotifier.sendEmails(
      recipientEmailAddresses = setOfNotNull(
        application.cruManagementArea?.emailAddress,
      ),
      templateId = Cas1NotifyTemplates.PLANNED_TRANSFER_REQUEST_CREATED,
      personalisation = personalisation,
      application = application,
    )
  }

  @EventListener
  fun plannedTransferRequestRejected(plannedTransferRequestRejected: PlannedTransferRequestRejected) {
    val changeRequest = plannedTransferRequestRejected.changeRequest
    val application = changeRequest.placementRequest.application
    val spaceBooking = changeRequest.spaceBooking

    val fromPersonalisation = spaceBooking.toEmailBookingInfo(application).personalisationValues()

    val personalisation =
      mapOf(
        "crn" to application.crn,
        "fromPremisesName" to fromPersonalisation.premisesName,
        "fromPlacementTimelineUrl" to spaceBooking.getTimelineUrl(),
      )

    emailNotifier.sendEmails(
      recipientEmailAddresses = setOfNotNull(spaceBooking.premises.emailAddress),
      templateId = Cas1NotifyTemplates.PLANNED_TRANSFER_REQUEST_REJECTED,
      personalisation = personalisation,
      application = application,
    )
  }

  @EventListener
  fun plannedTransferRequestAccepted(plannedTransferRequestAccepted: PlannedTransferRequestAccepted) {
    val changeRequest = plannedTransferRequestAccepted.changeRequest
    val application = changeRequest.placementRequest.application
    val originalBooking = changeRequest.spaceBooking
    val newBooking = plannedTransferRequestAccepted.newBooking

    val fromPersonalisation = originalBooking.toEmailBookingInfo(application).personalisationValues()
    val toPersonalisation = newBooking.toEmailBookingInfo(application).personalisationValues()

    val personalisation =
      mapOf(
        "crn" to application.crn,
        "fromPremisesName" to fromPersonalisation.premisesName,
        "toPremisesName" to toPersonalisation.premisesName,
        "toPlacementStartDate" to toPersonalisation.startDate,
        "toPlacementEndDate" to toPersonalisation.endDate,
        "toPlacementLengthStay" to toPersonalisation.lengthOfStay,
        "toPlacementLengthStayUnit" to toPersonalisation.lengthOfStayUnit,
        "toPlacementTimelineUrl" to newBooking.getTimelineUrl(),
      )

    emailNotifier.sendEmails(
      recipientEmailAddresses = setOfNotNull(
        originalBooking.premises.emailAddress,
      ),
      templateId = Cas1NotifyTemplates.PLANNED_TRANSFER_REQUEST_ACCEPTED_FOR_REQUESTING_AP,
      personalisation = personalisation,
      application = application,
    )

    emailNotifier.sendEmails(
      recipientEmailAddresses = setOfNotNull(
        newBooking.premises.emailAddress,
      ),
      templateId = Cas1NotifyTemplates.PLANNED_TRANSFER_REQUEST_ACCEPTED_FOR_TARGET_AP,
      personalisation = personalisation,
      application = application,
    )
  }

  private fun commonAppealPersonalisation(changeRequest: Cas1ChangeRequestEntity): Map<String, String> {
    val application = changeRequest.placementRequest.application

    return mapOf(
      "apName" to changeRequest.spaceBooking.premises.name,
      "crn" to application.crn,
      "cruDashboardUrl" to cruDashboardUrlTemplate.resolve(),
      "applicationTimelineUrl" to applicationTimelineUrlTemplate.resolve("applicationId", application.id.toString()),
    )
  }

  fun Cas1SpaceBookingEntity.getTimelineUrl() = spaceBookingTimelineUrlTemplate.resolve(
    mapOf(
      "premisesId" to premises.id.toString(),
      "bookingId" to id.toString(),
    ),
  )
}
