package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.format.DateTimeFormatter

@Service
class Cas2ApplicationEmailService(
  private val featureFlagService: FeatureFlagService,
  private val cas2EmailService: Cas2EmailService,
  @Value("\${url-templates.frontend.cas2v2.submitted-application-overview}") private val submittedApplicationUrlTemplate: UrlTemplate,
) {

  fun applicationSubmitted(cas2Application: Cas2ApplicationEntity) {
    if (!featureFlagService.getBooleanFlag("isr-email-changes-enabled")) return

    val recipientEmailAddress = cas2Application.createdByUser.email ?: return
    val submittedAt = cas2Application.submittedAt ?: return

    cas2EmailService.sendEmail(
      recipientEmailAddress = recipientEmailAddress,
      templateId = Cas2NotifyTemplates.CAS2_APPLICATION_SUBMITTED,
      personalisation = mapOf(
        "cohort" to (cas2Application.cohort!!.displayName),
        "crn" to cas2Application.crn,
        "timeApplicationReceived" to submittedAt.format(DateTimeFormatter.ofPattern("HH:mm")),
        "dateApplicationReceived" to submittedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
        "nacroReferenceId" to cas2Application.id.toString(),
        "viewSubmittedApplicationUrl" to submittedApplicationUrlTemplate
          .resolve("applicationId", cas2Application.id.toString()),
      ),
      cas2Application = cas2Application,
    )
  }
}
