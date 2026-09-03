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
  @Value("\${url-templates.frontend.cas2v2.application-overview}") private val submittedApplicationReferrerUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.cas2v2.submitted-application-overview}") private val submittedApplicationAssessorUrlTemplate: UrlTemplate,
) {

  fun applicationSubmitted(cas2Application: Cas2ApplicationEntity) {
    val submittedAt = cas2Application.submittedAt ?: error("Submitted At required")
    val cohort = requireNotNull(cas2Application.cohort)
    val timeReceived = submittedAt.format(DateTimeFormatter.ofPattern("HH:mm"))
    val dateReceived = submittedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    val referrerEmail = cas2Application.createdByUser.email

    val commonPersonalisation = mapOf(
      "cohort" to cohort.displayName,
      "crn" to cas2Application.crn,
      "timeApplicationReceived" to timeReceived,
      "dateApplicationReceived" to dateReceived,
      "nacroReferenceId" to cas2Application.id.toString(),
    )

    referrerEmail?.let {
      cas2EmailService.sendEmail(
        recipientEmailAddress = it,
        templateId = Cas2NotifyTemplates.CAS2_BAIL_APPLICATION_SUBMITTED,
        personalisation = commonPersonalisation + mapOf(
          "viewSubmittedApplicationUrl" to submittedApplicationReferrerUrlTemplate.resolve("id", cas2Application.id.toString()),
        ),
        cas2Application = cas2Application,
      )
    }

    cas2EmailService.sendEmail(
      recipientEmailAddress = notifyConfig.emailAddresses.cas2Assessors,
      templateId = Cas2NotifyTemplates.CAS2_BAIL_APPLICATION_TO_ASSESS,
      personalisation = commonPersonalisation + mapOf(
        "sla" to cohort.assessmentSla,
        "referrerName" to cas2Application.createdByUser.name,
        "referrerEmail" to (referrerEmail ?: ""),
        "referrerTelephoneNumber" to (cas2Application.telephoneNumber ?: ""),
        "viewSubmittedApplicationUrl" to submittedApplicationAssessorUrlTemplate.resolve("applicationId", cas2Application.id.toString()),
      ),
      cas2Application = cas2Application,
    )
  }
}
