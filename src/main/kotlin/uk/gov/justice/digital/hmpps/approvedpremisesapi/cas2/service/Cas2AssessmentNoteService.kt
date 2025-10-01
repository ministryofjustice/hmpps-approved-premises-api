package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormattedHourOfDay
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas2AssessmentNoteService(
  private val assessmentRepository: Cas2AssessmentRepository,
  private val applicationNoteRepository: Cas2ApplicationNoteRepository,
  private val cas2UserService: Cas2UserService,
  private val httpAuthService: HttpAuthService,
  private val emailNotificationService: EmailNotificationService,
  private val userAccessService: Cas2UserAccessService,
  private val notifyConfig: NotifyConfig,
  private val cas2EmailService: Cas2EmailService,
  @Value("\${url-templates.frontend.cas2.application-overview}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2.submitted-application-overview}") private val assessmentUrlTemplate: String,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Suppress("ReturnCount")
  fun createAssessmentNote(assessmentId: UUID, note: NewCas2ApplicationNote): AuthorisableActionResult<ValidatableActionResult<Cas2ApplicationNoteEntity>> {
    val assessment = assessmentRepository.findByIdOrNull(assessmentId)
      ?: return AuthorisableActionResult.NotFound()

    if (httpAuthService.getCas2AuthenticatedPrincipalOrThrow().isExternalUser()) {
      val savedNote = saveNote(assessment, note.note, cas2UserService.getUserForRequest())
      sendEmailToReferrer(savedNote)

      return AuthorisableActionResult.Success(
        ValidatableActionResult.Success(
          savedNote,
        ),
      )
    } else {
      val user = cas2UserService.getUserForRequest()
      if (userAccessService.offenderIsFromSamePrisonAsUser(assessment.application.currentPrisonCode, user.activeNomisCaseloadId)) {
        val savedNote = saveNote(assessment, note.note, user)
        sendEmailToAssessors(savedNote)

        return AuthorisableActionResult.Success(
          ValidatableActionResult.Success(
            savedNote,
          ),
        )
      } else {
        return AuthorisableActionResult.Unauthorised()
      }
    }
  }

  private fun sendEmailToReferrer(savedNote: Cas2ApplicationNoteEntity) {
    val email = cas2EmailService.getReferrerEmail(savedNote.application)
    if (email != null) {
      emailNotificationService.sendCas2Email(
        recipientEmailAddress = email,
        templateId = Cas2NotifyTemplates.cas2NoteAddedForReferrer,
        personalisation = mapOf(
          "dateNoteAdded" to savedNote.createdAt.toLocalDate().toCas2UiFormat(),
          "timeNoteAdded" to savedNote.createdAt.toCas2UiFormattedHourOfDay(),
          "nomsNumber" to savedNote.application.nomsNumber,
          "applicationType" to "Home Detention Curfew (HDC)",
          "applicationUrl" to applicationUrlTemplate.replace("#id", savedNote.application.id.toString()),
        ),
      )
    } else {
      val msg = "Email not found for User ${savedNote.application.createdByUser!!.id}. Unable to send email for Note ${savedNote.id} on Application ${savedNote.application.id}"
      log.error(msg)
      Sentry.captureMessage(msg)
    }
  }

  private fun sendEmailToAssessors(
    savedNote: Cas2ApplicationNoteEntity,
  ) {
    emailNotificationService.sendCas2Email(
      recipientEmailAddress = notifyConfig.emailAddresses.cas2Assessors,
      templateId = Cas2NotifyTemplates.cas2NoteAddedForAssessor,
      personalisation = mapOf(
        "nacroReferenceId" to getNacroReferenceIdOrPlaceholder(savedNote.application.assessment!!),
        "nacroReferenceIdInSubject" to getSubjectLineReferenceIdOrPlaceholder(savedNote.application.assessment!!),
        "dateNoteAdded" to savedNote.createdAt.toLocalDate().toCas2UiFormat(),
        "timeNoteAdded" to savedNote.createdAt.toCas2UiFormattedHourOfDay(),
        "assessorName" to getAssessorNameOrPlaceholder(savedNote.application.assessment!!),
        "applicationType" to "Home Detention Curfew (HDC)",
        "applicationUrl" to assessmentUrlTemplate.replace("#applicationId", savedNote.application.id.toString()),
      ),
    )
  }

  private fun getSubjectLineReferenceIdOrPlaceholder(assessment: Cas2AssessmentEntity): String {
    if (assessment.nacroReferralId != null) {
      return "(${assessment.nacroReferralId!!})"
    }
    return ""
  }

  private fun getNacroReferenceIdOrPlaceholder(assessment: Cas2AssessmentEntity): String {
    if (assessment.nacroReferralId != null) {
      return assessment.nacroReferralId!!
    }
    return "Unknown. " +
      "The Nacro CAS-2 reference number has not been added to the application yet."
  }

  private fun getAssessorNameOrPlaceholder(assessment: Cas2AssessmentEntity): String {
    if (assessment.assessorName != null) {
      return assessment.assessorName!!
    }
    return "Unknown. " +
      "The assessor has not added their name to the application yet."
  }

  private fun saveNote(assessment: Cas2AssessmentEntity, body: String, user: Cas2UserEntity): Cas2ApplicationNoteEntity {
    val newNote = Cas2ApplicationNoteEntity(
      id = UUID.randomUUID(),
      application = assessment.application,
      body = body,
      createdAt = OffsetDateTime.now(),
      createdByUser = user,
      assessment = assessment,
    )

    return applicationNoteRepository.save(newNote)
  }
}
