package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormattedHourOfDay
import java.time.format.DateTimeFormatter

@Service
class Cas2ApplicationNoteEmailService(
  private val cas2EmailService: Cas2EmailService,
  private val notifyConfig: NotifyConfig,
  @Value("\${url-templates.frontend.cas2v2.application-overview}")
  private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.cas2v2.submitted-application-overview}")
  private val assessmentUrlTemplate: UrlTemplate,
) {

  companion object {
    private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  }

  fun assessorNoteAdded(
    cas2Application: Cas2ApplicationEntity,
    applicationNote: Cas2ApplicationNoteEntity,
  ) {
    sendNoteAddedEmail(
      cas2Application = cas2Application,
      applicationNote = applicationNote,
      recipientEmail = notifyConfig.emailAddresses.cas2Assessors,
      resolvedUrl = assessmentUrlTemplate.resolve(mapOf("applicationId" to cas2Application.id.toString())),
      templateId = Cas2NotifyTemplates.CAS2_BAIL_APPLICATION_ASSESSOR_NOTE_ADDED,
    )
  }

  fun refererNoteAdded(
    cas2Application: Cas2ApplicationEntity,
    applicationNote: Cas2ApplicationNoteEntity,
  ) {
    val recipientEmail = cas2Application.createdByUser.email ?: return

    sendNoteAddedEmail(
      cas2Application = cas2Application,
      applicationNote = applicationNote,
      recipientEmail = recipientEmail,
      resolvedUrl = applicationUrlTemplate.resolve(mapOf("id" to cas2Application.id.toString())),
      templateId = Cas2NotifyTemplates.CAS2_BAIL_APPLICATION_REFERRER_NOTE_ADDED,
    )
  }

  private fun sendNoteAddedEmail(
    cas2Application: Cas2ApplicationEntity,
    applicationNote: Cas2ApplicationNoteEntity,
    recipientEmail: String,
    resolvedUrl: String,
    templateId: String,
  ) {
    val submittedAt = requireNotNull(cas2Application.submittedAt)
    val cohort = requireNotNull(cas2Application.cohort)

    val personalisation = mapOf(
      "dateNoteAdded" to applicationNote.createdAt.toLocalDate().toCas2UiFormat(),
      "timeNoteAdded" to applicationNote.createdAt.toCas2UiFormattedHourOfDay(),
      "cohort" to cohort.displayName,
      "crn" to cas2Application.crn,
      "timeApplicationReceived" to submittedAt.format(TIME_FORMAT),
      "dateApplicationReceived" to submittedAt.format(DATE_FORMAT),
      "nacroReferenceId" to cas2Application.id.toString(),
      "viewSubmittedApplicationUrl" to resolvedUrl,
    )

    cas2EmailService.sendEmail(
      recipientEmailAddress = recipientEmail,
      templateId = templateId,
      personalisation = personalisation,
      cas2Application = cas2Application,
    )
  }
}
