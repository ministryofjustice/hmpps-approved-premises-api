package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Constants.DAYS_IN_WEEK
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilInclusive

object Constants {
  const val DAYS_IN_WEEK = 7
}

@Service
class Cas1BookingEmailService(
  private val emailNotificationService: EmailNotifier,
  private val notifyConfig: NotifyConfig,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.booking}") private val bookingUrlTemplate: UrlTemplate,
  @Value("\${notify.send-new-withdrawal-notifications}") private val sendNewWithdrawalNotifications: Boolean,
) {

  fun bookingMade(application: ApplicationEntity, booking: BookingEntity) {
    val applicationSubmittedByUser = application.createdByUser

    val emailPersonalisation = buildCommonPersonalisation(
      application,
      booking,
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

  fun bookingWithdrawn(application: ApprovedPremisesApplicationEntity, booking: BookingEntity) {
    if(!sendNewWithdrawalNotifications) {
      return
    }

   val allPersonalisation =
     buildCommonPersonalisation(application, booking) +
     mapOf(
       "region" to booking.premises.probationRegion.name
     )

    val applicationSubmittedByUser = application.createdByUser
    applicationSubmittedByUser.email?.let { email ->
      emailNotificationService.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.bookingWithdrawn,
        personalisation = allPersonalisation,
      )
    }

    val premises = booking.premises
    premises.emailAddress?.let { email ->
      emailNotificationService.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.bookingWithdrawn,
        personalisation = allPersonalisation,
      )
    }

    val area = application.apArea
    area?.emailAddress?.let { cruEmail ->
      emailNotificationService.sendEmail(
        recipientEmailAddress = cruEmail,
        templateId = notifyConfig.templates.bookingWithdrawn,
        personalisation = allPersonalisation,
      )
    }
  }

  fun buildCommonPersonalisation(application: ApplicationEntity, booking: BookingEntity): Map<String,Any> {
    val applicationSubmittedByUser = application.createdByUser

    val lengthOfStayDays = booking.arrivalDate.getDaysUntilInclusive(booking.departureDate).size
    val lengthOfStayWeeks = lengthOfStayDays.toDouble() / DAYS_IN_WEEK
    val lengthOfStayWeeksWholeNumber = (lengthOfStayDays.toDouble() % DAYS_IN_WEEK) == 0.0

    return mapOf(
      "name" to applicationSubmittedByUser.name,
      "apName" to booking.premises.name,
      "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
      "bookingUrl" to bookingUrlTemplate.resolve(
        mapOf(
          "premisesId" to booking.premises.id.toString(),
          "bookingId" to booking.id.toString()
        )
      ),
      "crn" to application.crn,
      "startDate" to booking.arrivalDate.toString(),
      "endDate" to booking.departureDate.toString(),
      "lengthStay" to if (lengthOfStayWeeksWholeNumber) lengthOfStayWeeks.toInt() else lengthOfStayDays,
      "lengthStayUnit" to if (lengthOfStayWeeksWholeNumber) "weeks" else "days",
    )
  }
}
