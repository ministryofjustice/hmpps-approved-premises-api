package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2

import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2v2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Constants.HDC_APPLICATION_TYPE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.Cas2v2ApplicationUtils
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormattedHourOfDay
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas2v2ApplicationNoteService(
  private val cas2v2ApplicationRepository: Cas2v2ApplicationRepository,
  private val cas2v2AssessmentRepository: Cas2v2AssessmentRepository,
  private val cas2v2ApplicationNoteRepository: Cas2v2ApplicationNoteRepository,
  private val userService: Cas2v2UserService,
  private val userAccessService: Cas2v2UserAccessService,
  private val emailNotificationService: EmailNotificationService,
  private val notifyConfig: NotifyConfig,
  @Value("\${url-templates.frontend.cas2v2.application-overview}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2v2.submitted-application-overview}") private val assessmentUrlTemplate: String,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Suppress("ReturnCount")
  fun createAssessmentNote(assessmentId: UUID, note: NewCas2v2ApplicationNote): CasResult<Cas2v2ApplicationNoteEntity> {
    val assessment = cas2v2AssessmentRepository.findByIdOrNull(assessmentId)
      ?: return CasResult.NotFound("Cas2v2ApplicationNoteEntity", assessmentId.toString())

    val application = cas2v2ApplicationRepository.findByIdOrNull(assessment.application.id)
      ?: return CasResult.NotFound("Cas2v2ApplicationNoteEntity", assessmentId.toString())

    if (application.submittedAt == null) {
      return CasResult.GeneralValidationError("This application has not been submitted")
    }

    val user = userService.getUserForRequest()

    if (!userAccessService.userCanAddNote(user, application)) {
      return CasResult.Unauthorised()
    }

    val savedNote = saveNote(application, assessment, note.note, user)
    sendEmail(user.isExternal(), application, savedNote)

    return CasResult.Success(savedNote)
  }

  private fun sendEmail(
    isExternalUser: Boolean,
    application: Cas2v2ApplicationEntity,
    savedNote: Cas2v2ApplicationNoteEntity,
  ) {
    if (isExternalUser) {
      sendEmailToReferrer(application, savedNote)
    } else {
      sendEmailToAssessors(application, savedNote)
    }
  }

  private fun sendEmailToReferrer(
    application: Cas2v2ApplicationEntity,
    savedNote: Cas2v2ApplicationNoteEntity,
  ) {
    if (application.createdByUser.email != null) {
      val applicationOrigin = application.applicationOrigin
      val applicationType = Cas2v2ApplicationUtils().getApplicationTypeFromApplicationOrigin(applicationOrigin)

      val templateId = when (applicationOrigin) {
        ApplicationOrigin.courtBail -> notifyConfig.templates.cas2v2NoteAddedForReferrerCourtBail
        ApplicationOrigin.prisonBail-> notifyConfig.templates.cas2v2NoteAddedForReferrerPrisonBail
        ApplicationOrigin.homeDetentionCurfew -> notifyConfig.templates.cas2NoteAddedForReferrer
      }
      emailNotificationService.sendCas2Email(
        recipientEmailAddress = application.createdByUser.email!!,
        templateId = templateId,
        personalisation = mapOf(
          "dateNoteAdded" to savedNote.createdAt.toLocalDate().toCas2UiFormat(),
          "timeNoteAdded" to savedNote.createdAt.toCas2UiFormattedHourOfDay(),
          "nomsNumber" to application.nomsNumber,
          "crn" to application.crn,
          "applicationType" to applicationType,
          "applicationUrl" to applicationUrlTemplate.replace("#id", application.id.toString()),
        ),
      )
    } else {
      log.error("Email not found for User ${application.createdByUser.id}. Unable to send email for Note ${savedNote.id} on Application ${application.id}")
      Sentry.captureMessage("Email not found for User ${application.createdByUser.id}. Unable to send email for Note ${savedNote.id} on Application ${application.id}")
    }
  }

  private fun sendEmailToAssessors(
    application: Cas2v2ApplicationEntity,
    savedNote: Cas2v2ApplicationNoteEntity,
  ) {

    val applicationOrigin = application.applicationOrigin
    val applicationType = Cas2v2ApplicationUtils().getApplicationTypeFromApplicationOrigin(applicationOrigin)

    val templateId = when (applicationOrigin) {
      ApplicationOrigin.courtBail -> notifyConfig.templates.cas2v2NoteAddedForAssessorCourtBail
      ApplicationOrigin.prisonBail-> notifyConfig.templates.cas2v2NoteAddedForAssessorPrisonBail
      ApplicationOrigin.homeDetentionCurfew -> notifyConfig.templates.cas2NoteAddedForAssessor
    }

    emailNotificationService.sendCas2Email(
      recipientEmailAddress = notifyConfig.emailAddresses.cas2Assessors,
      templateId = templateId,
      personalisation = mapOf(
        "nacroReferenceId" to getNacroReferenceIdOrPlaceholder(application.assessment!!),
        "nacroReferenceIdInSubject" to getSubjectLineReferenceIdOrPlaceholder(application.assessment!!),
        "dateNoteAdded" to savedNote.createdAt.toLocalDate().toCas2UiFormat(),
        "timeNoteAdded" to savedNote.createdAt.toCas2UiFormattedHourOfDay(),
        "assessorName" to getAssessorNameOrPlaceholder(application.assessment!!),
        "nomsNumber" to application.nomsNumber,
        "crn" to application.crn,
        "applicationType" to applicationType,
        "applicationUrl" to assessmentUrlTemplate.replace("#applicationId", application.id.toString()),
      ),
    )
  }

  private fun getSubjectLineReferenceIdOrPlaceholder(assessment: Cas2v2AssessmentEntity): String {
    if (assessment.nacroReferralId != null) {
      return "(${assessment.nacroReferralId!!})"
    }
    return ""
  }

  private fun getNacroReferenceIdOrPlaceholder(assessment: Cas2v2AssessmentEntity): String {
    if (assessment.nacroReferralId != null) {
      return assessment.nacroReferralId!!
    }
    return "Unknown. " +
      "The Nacro CAS-2 reference number has not been added to the application yet."
  }

  private fun getAssessorNameOrPlaceholder(assessment: Cas2v2AssessmentEntity): String {
    if (assessment.assessorName != null) {
      return assessment.assessorName!!
    }
    return "Unknown. " + "The assessor has not added their name to the application yet."
  }

  private fun saveNote(application: Cas2v2ApplicationEntity, assessment: Cas2v2AssessmentEntity, body: String, user: Cas2v2UserEntity): Cas2v2ApplicationNoteEntity {
    val newNote = Cas2v2ApplicationNoteEntity(
      id = UUID.randomUUID(),
      application = application,
      body = body,
      createdAt = OffsetDateTime.now(),
      createdByUser = user,
      assessment = assessment,
    )

    return cas2v2ApplicationNoteRepository.save(newNote)
  }
}
