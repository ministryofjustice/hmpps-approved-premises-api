package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.util.Cas2v2ApplicationUtils
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
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
  private val applicationRepository: Cas2ApplicationRepository,
  private val applicationNoteRepository: Cas2ApplicationNoteRepository,
  private val cas2UserService: Cas2UserService,
  private val httpAuthService: HttpAuthService,
  private val emailNotificationService: EmailNotificationService,
  private val userAccessService: Cas2UserAccessService,
  private val notifyConfig: NotifyConfig,
  private val cas2EmailService: Cas2EmailService,
  @Value("\${url-templates.frontend.cas2.application-overview}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2.submitted-application-overview}") private val assessmentUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2v2.application-overview}") private val cas2v2ApplicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2v2.submitted-application-overview}") private val cas2v2AssessmentUrlTemplate: String,
  ) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Suppress("ReturnCount")
  fun createAssessmentNote(assessmentId: UUID, note: NewCas2ApplicationNote, serviceOrigin: Cas2ServiceOrigin): CasResult<Cas2ApplicationNoteEntity> {
    val assessment = assessmentRepository.findByIdAndServiceOrigin(assessmentId, serviceOrigin)
      ?: return CasResult.NotFound("Cas2AssessmentEntity", assessmentId.toString())

    return when (serviceOrigin) {
      Cas2ServiceOrigin.HDC -> createAssessmentNoteForHdc(assessment, note)
      Cas2ServiceOrigin.BAIL -> createAssessmentNoteForBail(assessment, note)
    }
  }

  private fun createAssessmentNoteForHdc(assessment: Cas2AssessmentEntity, note: NewCas2ApplicationNote): CasResult<Cas2ApplicationNoteEntity> {
    if (httpAuthService.getCas2AuthenticatedPrincipalOrThrow().isExternalUser()) {
      val savedNote = saveNote(assessment, note.note, cas2UserService.getUserForRequest(assessment.serviceOrigin))
      sendEmailToReferrer(savedNote)
      return CasResult.Success(savedNote)
    }
    val user = cas2UserService.getUserForRequest(assessment.serviceOrigin)
    return if (userAccessService.offenderIsFromSamePrisonAsUser(assessment.application.currentPrisonCode, user.activeNomisCaseloadId)) {
      val savedNote = saveNote(assessment, note.note, user)
      sendEmailToAssessors(savedNote)
      CasResult.Success(savedNote)
    } else {
      CasResult.Unauthorised()
    }
  }

  @Suppress("ReturnCount")
  private fun createAssessmentNoteForBail(assessment: Cas2AssessmentEntity, note: NewCas2ApplicationNote): CasResult<Cas2ApplicationNoteEntity> {
    val application = applicationRepository.findByIdAndServiceOrigin(assessment.application.id, assessment.serviceOrigin)
      ?: return CasResult.NotFound("Cas2ApplicationNoteEntity", assessment.application.id.toString())

    if (application.submittedAt == null) {
      return CasResult.GeneralValidationError("This application has not been submitted")
    }

    val user = cas2UserService.getUserForRequest(assessment.serviceOrigin)

    if (!userAccessService.userCanAddNote(user, application)) {
      return CasResult.Unauthorised()
    }

    val savedNote = saveNote(assessment, note.note, user)
    sendEmailForBail(user.isExternal(), application, assessment, savedNote)

    return CasResult.Success(savedNote)
  }

  private fun sendEmailForBail(
    isExternalUser: Boolean,
    application: Cas2ApplicationEntity,
    assessment: Cas2AssessmentEntity,
    savedNote: Cas2ApplicationNoteEntity,
  ) {
    if (isExternalUser) {
      sendEmailToReferrerForBail(application, savedNote)
    } else {
      sendEmailToAssessorsForBail(application, assessment, savedNote)
    }
  }

  private fun sendEmailToReferrerForBail(
    application: Cas2ApplicationEntity,
    savedNote: Cas2ApplicationNoteEntity,
  ) {
    if (application.createdByUser.email != null) {
      val applicationOrigin = application.applicationOrigin
      val applicationType = Cas2v2ApplicationUtils().getApplicationTypeFromApplicationOrigin(applicationOrigin)

      val templateId = when (applicationOrigin) {
        ApplicationOrigin.courtBail -> Cas2NotifyTemplates.CAS2_V2_NOTE_ADDED_FOR_REFERRER_COURT_BAIL
        ApplicationOrigin.prisonBail -> Cas2NotifyTemplates.CAS2_V2_NOTE_ADDED_FOR_REFERRER_PRISON_BAIL
        ApplicationOrigin.homeDetentionCurfew -> Cas2NotifyTemplates.CAS2_NOTE_ADDED_FOR_REFERRER
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
          "applicationUrl" to cas2v2ApplicationUrlTemplate.replace("#id", application.id.toString()),
        ),
      )
    } else {
      log.error("Email not found for User ${application.createdByUser.id}. Unable to send email for Note ${savedNote.id} on Application ${application.id}")
      Sentry.captureMessage("Email not found for User ${application.createdByUser.id}. Unable to send email for Note ${savedNote.id} on Application ${application.id}")
    }
  }

  private fun sendEmailToAssessorsForBail(
    application: Cas2ApplicationEntity,
    assessment: Cas2AssessmentEntity,
    savedNote: Cas2ApplicationNoteEntity,
  ) {
    val applicationOrigin = application.applicationOrigin
    val applicationType = Cas2v2ApplicationUtils().getApplicationTypeFromApplicationOrigin(applicationOrigin)

    val templateId = when (applicationOrigin) {
      ApplicationOrigin.courtBail -> Cas2NotifyTemplates.CAS2_V2_NOTE_ADDED_FOR_ASSESSOR_COURT_BAIL
      ApplicationOrigin.prisonBail -> Cas2NotifyTemplates.CAS2_V2_NOTE_ADDED_FOR_ASSESSOR_PRISON_BAIL
      ApplicationOrigin.homeDetentionCurfew -> Cas2NotifyTemplates.CAS2_NOTE_ADDED_FOR_ASSESSOR
    }

    emailNotificationService.sendCas2Email(
      recipientEmailAddress = notifyConfig.emailAddresses.cas2Assessors,
      templateId = templateId,
      personalisation = mapOf(
        "nacroReferenceId" to getNacroReferenceIdOrPlaceholder(assessment),
        "nacroReferenceIdInSubject" to getSubjectLineReferenceIdOrPlaceholder(assessment),
        "dateNoteAdded" to savedNote.createdAt.toLocalDate().toCas2UiFormat(),
        "timeNoteAdded" to savedNote.createdAt.toCas2UiFormattedHourOfDay(),
        "assessorName" to getAssessorNameOrPlaceholder(assessment),
        "nomsNumber" to application.nomsNumber,
        "crn" to application.crn,
        "applicationType" to applicationType,
        "applicationUrl" to cas2v2AssessmentUrlTemplate.replace("#applicationId", application.id.toString()),
      ),
    )
  }

  private fun sendEmailToReferrer(savedNote: Cas2ApplicationNoteEntity) {
    val email = cas2EmailService.getReferrerEmail(savedNote.application)
    if (email != null) {
      emailNotificationService.sendCas2Email(
        recipientEmailAddress = email,
        templateId = Cas2NotifyTemplates.CAS2_NOTE_ADDED_FOR_REFERRER,
        personalisation = mapOf(
          "dateNoteAdded" to savedNote.createdAt.toLocalDate().toCas2UiFormat(),
          "timeNoteAdded" to savedNote.createdAt.toCas2UiFormattedHourOfDay(),
          "nomsNumber" to savedNote.application.nomsNumber,
          "applicationType" to "Home Detention Curfew (HDC)",
          "applicationUrl" to applicationUrlTemplate.replace("#id", savedNote.application.id.toString()),
        ),
      )
    } else {
      val msg = "Email not found for User ${savedNote.application.createdByUser.id}. Unable to send email for Note ${savedNote.id} on Application ${savedNote.application.id}"
      log.error(msg)
      Sentry.captureMessage(msg)
    }
  }

  private fun sendEmailToAssessors(
    savedNote: Cas2ApplicationNoteEntity,
  ) {
    emailNotificationService.sendCas2Email(
      recipientEmailAddress = notifyConfig.emailAddresses.cas2Assessors,
      templateId = Cas2NotifyTemplates.CAS2_NOTE_ADDED_FOR_ASSESSOR,
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
