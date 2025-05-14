package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.ArrivalRecorded

@Service
class Cas1BookingManagementEmailService(
  private val emailNotifier: Cas1EmailNotifier,
) {
  @EventListener
  fun arrivalRecorded(arrivalRecorded: ArrivalRecorded) = arrivalRecorded(arrivalRecorded.spaceBooking)

  fun arrivalRecorded(spaceBooking: Cas1SpaceBookingEntity) {
    if (spaceBooking.transferType != null) {
      val application = spaceBooking.application!!
      val recipientEmailAddresses = spaceBooking.placementRequest!!.requestingUsersEmailAddresses()

      val toPersonalisation = spaceBooking.toEmailBookingInfo(spaceBooking.application!!).personalisationValues()
      val fromPersonalisation = spaceBooking.transferredFrom!!.toEmailBookingInfo(spaceBooking.application!!).personalisationValues()

      val personalisation = mapOf(
        "crn" to application.crn,
        "fromPremisesName" to fromPersonalisation.premisesName,
        "toPremisesName" to toPersonalisation.premisesName,
        "startDate" to toPersonalisation.startDate,
        "endDate" to toPersonalisation.endDate,
        "lengthStay" to toPersonalisation.lengthOfStay,
        "lengthStayUnit" to toPersonalisation.lengthOfStayUnit,
      )

      emailNotifier.sendEmails(
        recipientEmailAddresses = recipientEmailAddresses,
        templateId = Cas1NotifyTemplates.TRANSFER_COMPLETE,
        personalisation = personalisation,
        application = application,
      )

      if (spaceBooking.transferType == TransferType.EMERGENCY) {
        emailNotifier.sendEmails(
          recipientEmailAddresses = setOfNotNull(application.cruManagementArea?.emailAddress),
          templateId = Cas1NotifyTemplates.TRANSFER_COMPLETE_EMERGENCY_FOR_CRU,
          personalisation = personalisation,
          application = application,
        )
      }
    }
  }
}
