package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormattedHourOfDay
import java.time.format.DateTimeFormatter

@Service
class Cas2ApplicationStatusUpdateEmailService(
  private val cas2EmailService: Cas2EmailService,
  @Value("\${url-templates.frontend.cas2v2.application-overview}") private val applicationOverviewUrlTemplate: UrlTemplate,
) {

  fun statusUpdate(cas2Application: Cas2ApplicationEntity, status: Cas2StatusUpdateEntity) {
    val recipientEmailAddress = cas2Application.createdByUser.email ?: return
    val submittedAt = requireNotNull(cas2Application.submittedAt) ?: return
    val cohort = requireNotNull(cas2Application.cohort)
    val timeReceived = submittedAt.format(DateTimeFormatter.ofPattern("HH:mm"))
    val dateReceived = submittedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    val personalisation = mapOf(
      "applicationStatusChange" to status.label,
      "dateStatusChanged" to status.createdAt.toLocalDate().toCas2UiFormat(),
      "timeStatusChanged" to status.createdAt.toCas2UiFormattedHourOfDay(),
      "cohort" to cohort.displayName,
      "crn" to cas2Application.crn,
      "timeApplicationReceived" to timeReceived,
      "dateApplicationReceived" to dateReceived,
      "nacroReferenceId" to cas2Application.id.toString(),
      "viewSubmittedApplicationUrl" to applicationOverviewUrlTemplate
        .resolve("applicationId", cas2Application.id.toString()),
    )

    cas2EmailService.sendEmail(
      recipientEmailAddress = recipientEmailAddress,
      templateId = Cas2NotifyTemplates.CAS2_BAIL_APPLICATION_STATUS_UPDATE,
      personalisation = personalisation,
      cas2Application = cas2Application,
    )
  }
}
