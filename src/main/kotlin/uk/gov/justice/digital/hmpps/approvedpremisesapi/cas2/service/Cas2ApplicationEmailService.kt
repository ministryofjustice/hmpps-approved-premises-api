package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.format.DateTimeFormatter

@Service
class Cas2ApplicationEmailService(
  private val cas2EmailService: Cas2EmailService,
  private val notifyConfig: NotifyConfig,
  @Value("\${url-templates.frontend.cas2v2.submitted-application-overview}") private val submittedApplicationUrlTemplate: UrlTemplate,
) {

  fun applicationSubmitted(cas2Application: Cas2ApplicationEntity) {
    val recipientEmailAddress = cas2Application.createdByUser.email ?: return
    val submittedAt = cas2Application.submittedAt ?: return
    val cohort = requireNotNull(cas2Application.cohort)
    val timeReceived = submittedAt.format(DateTimeFormatter.ofPattern("HH:mm"))
    val dateReceived = submittedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    val commonPersonalisation = mapOf(
      "cohort" to cohort.displayName,
      "crn" to cas2Application.crn,
      "timeApplicationReceived" to timeReceived,
      "dateApplicationReceived" to dateReceived,
      "nacroReferenceId" to cas2Application.id.toString(),
      "viewSubmittedApplicationUrl" to submittedApplicationUrlTemplate
        .resolve("applicationId", cas2Application.id.toString()),
    )

    cas2EmailService.sendEmail(
      recipientEmailAddress = recipientEmailAddress,
      templateId = Cas2NotifyTemplates.CAS2_BAIL_APPLICATION_SUBMITTED,
      personalisation = commonPersonalisation,
      cas2Application = cas2Application,
    )

    cas2EmailService.sendEmail(
      recipientEmailAddress = notifyConfig.emailAddresses.cas2Assessors,
      templateId = Cas2NotifyTemplates.CAS2_BAIL_APPLICATION_TO_ASSESS,
      personalisation = commonPersonalisation + mapOf(
        "sla" to cohort.assessmentSla,
        "referrerName" to cas2Application.createdByUser.name,
        "referrerEmail" to cas2Application.createdByUser.email,
        "referrerTelephoneNumber" to cas2Application.telephoneNumber,
      ),
      cas2Application = cas2Application,
    )
  }
}
