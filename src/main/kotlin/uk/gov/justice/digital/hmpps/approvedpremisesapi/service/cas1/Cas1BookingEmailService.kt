package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
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
  @Value("\${url-templates.frontend.application-timeline}") private val applicationTimelineUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.booking}") private val bookingUrlTemplate: UrlTemplate,
  @Value("\${feature-flags.cas1-use-new-withdrawal-logic}") private val sendNewWithdrawalNotifications: Boolean,
  @Value("\${feature-flags.cas1-aps530-withdrawal-email-improvements}") private val aps530WithdrawalEmailImprovements: Boolean,
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

  fun bookingWithdrawn(
    application: ApprovedPremisesApplicationEntity,
    booking: BookingEntity,
    withdrawingUser: UserEntity?,
  ) {
    if (!sendNewWithdrawalNotifications) {
      return
    }

    val allPersonalisation =
      buildCommonPersonalisation(application, booking).toMutableMap()

    allPersonalisation += "region" to booking.premises.probationRegion.name
    if (withdrawingUser != null) {
      allPersonalisation["withdrawnBy"] = withdrawingUser.name
    }

    val template = if (aps530WithdrawalEmailImprovements) {
      notifyConfig.templates.bookingWithdrawnV2
    } else {
      notifyConfig.templates.bookingWithdrawn
    }

    val applicationSubmittedByUser = application.createdByUser
    applicationSubmittedByUser.email?.let { email ->
      emailNotificationService.sendEmail(
        recipientEmailAddress = email,
        templateId = template,
        personalisation = allPersonalisation,
      )
    }

    val premises = booking.premises
    premises.emailAddress?.let { email ->
      emailNotificationService.sendEmail(
        recipientEmailAddress = email,
        templateId = template,
        personalisation = allPersonalisation,
      )
    }

    val area = application.apArea
    area?.emailAddress?.let { cruEmail ->
      emailNotificationService.sendEmail(
        recipientEmailAddress = cruEmail,
        templateId = template,
        personalisation = allPersonalisation,
      )
    }
  }

  fun buildCommonPersonalisation(application: ApplicationEntity, booking: BookingEntity): Map<String, Any> {
    val applicationSubmittedByUser = application.createdByUser

    val lengthOfStayDays = booking.arrivalDate.getDaysUntilInclusive(booking.departureDate).size
    val lengthOfStayWeeks = lengthOfStayDays.toDouble() / DAYS_IN_WEEK
    val lengthOfStayWeeksWholeNumber = (lengthOfStayDays.toDouble() % DAYS_IN_WEEK) == 0.0

    return mapOf(
      "name" to applicationSubmittedByUser.name,
      "apName" to booking.premises.name,
      "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
      "applicationTimelineUrl" to applicationTimelineUrlTemplate.resolve("applicationId", application.id.toString()),
      "bookingUrl" to bookingUrlTemplate.resolve(
        mapOf(
          "premisesId" to booking.premises.id.toString(),
          "bookingId" to booking.id.toString(),
        ),
      ),
      "crn" to application.crn,
      "startDate" to booking.arrivalDate.toString(),
      "endDate" to booking.departureDate.toString(),
      "lengthStay" to if (lengthOfStayWeeksWholeNumber) lengthOfStayWeeks.toInt() else lengthOfStayDays,
      "lengthStayUnit" to if (lengthOfStayWeeksWholeNumber) "weeks" else "days",
    )
  }
}
