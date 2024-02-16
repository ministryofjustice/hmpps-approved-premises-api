package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Constants.DAYS_IN_WEEK
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilInclusive

object Constants {
  const val DAYS_IN_WEEK = 7
}

@Service
class Cas1BookingEmailService(
  private val emailNotificationService: EmailNotificationService,
  private val notifyConfig: NotifyConfig,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.booking}") private val bookingUrlTemplate: String,
) {

  fun bookingMade(application: ApprovedPremisesApplicationEntity, booking: BookingEntity) {
    val applicationSubmittedByUser = application.createdByUser

    val lengthOfStayDays = booking.arrivalDate.getDaysUntilInclusive(booking.departureDate).size
    val lengthOfStayWeeks = lengthOfStayDays.toDouble() / DAYS_IN_WEEK
    val lengthOfStayWeeksWholeNumber = (lengthOfStayDays.toDouble() % DAYS_IN_WEEK) == 0.0

    val emailPersonalisation = mapOf(
      "name" to applicationSubmittedByUser.name,
      "apName" to booking.premises.name,
      "applicationUrl" to applicationUrlTemplate.replace("#id", application.id.toString()),
      "bookingUrl" to bookingUrlTemplate.replace("#premisesId", booking.premises.id.toString())
        .replace("#bookingId", booking.id.toString()),
      "crn" to application.crn,
      "startDate" to booking.arrivalDate.toString(),
      "endDate" to booking.departureDate.toString(),
      "lengthStay" to if (lengthOfStayWeeksWholeNumber) lengthOfStayWeeks.toInt() else lengthOfStayDays,
      "lengthStayUnit" to if (lengthOfStayWeeksWholeNumber) "weeks" else "days",
    )

    if (applicationSubmittedByUser.email != null) {
      emailNotificationService.sendEmail(
        recipientEmailAddress = applicationSubmittedByUser.email!!,
        templateId = notifyConfig.templates.bookingMade,
        personalisation = emailPersonalisation,
      )
    }

    if (booking.premises.emailAddress != null) {
      emailNotificationService.sendEmail(
        recipientEmailAddress = booking.premises.emailAddress!!,
        templateId = notifyConfig.templates.bookingMadePremises,
        personalisation = emailPersonalisation,
      )
    }
  }
}
