package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserEntity
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
  private val applicationRepository: Cas2ApplicationRepository,
  private val assessmentRepository: Cas2AssessmentRepository,
  private val applicationNoteRepository: Cas2ApplicationNoteRepository,
  private val userService: Cas2UserService,
  private val externalUserService: ExternalUserService,
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

    val application = applicationRepository.findByIdOrNull(assessment.application.id)
      ?: return AuthorisableActionResult.NotFound()

    if (application.submittedAt == null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has not been submitted"),
      )
    }

    val isExternalUser = httpAuthService.getCas2AuthenticatedPrincipalOrThrow().isExternalUser()
    val user = getCas2User(isExternalUser)

    if (!isExternalUser && !nomisUserCanAddNote(application, user as NomisUserEntity)) {
      return AuthorisableActionResult.Unauthorised()
    }

    val savedNote = saveNote(application, assessment, note.note, user)

    sendEmail(isExternalUser, application, savedNote)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        savedNote,
      ),
    )
  }

  private fun sendEmail(
    isExternalUser: Boolean,
    application: Cas2ApplicationEntity,
    savedNote: Cas2ApplicationNoteEntity,
  ) {
    if (isExternalUser) {
      sendEmailToReferrer(application, savedNote)
    } else {
      sendEmailToAssessors(application, savedNote)
    }
  }

  private fun sendEmailToReferrer(application: Cas2ApplicationEntity, savedNote: Cas2ApplicationNoteEntity) {
    val email = cas2EmailService.getReferrerEmail(application)
    if (email != null) {
      emailNotificationService.sendCas2Email(
        recipientEmailAddress = email,
        templateId = Cas2NotifyTemplates.cas2NoteAddedForReferrer,
        personalisation = mapOf(
          "dateNoteAdded" to savedNote.createdAt.toLocalDate().toCas2UiFormat(),
          "timeNoteAdded" to savedNote.createdAt.toCas2UiFormattedHourOfDay(),
          "nomsNumber" to application.nomsNumber,
          "applicationType" to "Home Detention Curfew (HDC)",
          "applicationUrl" to applicationUrlTemplate.replace("#id", application.id.toString()),
        ),
      )
    } else {
      val msg = "Email not found for User ${application.getCreatedById()}. Unable to send email for Note ${savedNote.id} on Application ${application.id}"
      log.error(msg)
      Sentry.captureMessage(msg)
    }
  }

  private fun sendEmailToAssessors(
    application: Cas2ApplicationEntity,
    savedNote: Cas2ApplicationNoteEntity,
  ) {
    emailNotificationService.sendCas2Email(
      recipientEmailAddress = notifyConfig.emailAddresses.cas2Assessors,
      templateId = Cas2NotifyTemplates.cas2NoteAddedForAssessor,
      personalisation = mapOf(
        "nacroReferenceId" to getNacroReferenceIdOrPlaceholder(application.assessment!!),
        "nacroReferenceIdInSubject" to getSubjectLineReferenceIdOrPlaceholder(application.assessment!!),
        "dateNoteAdded" to savedNote.createdAt.toLocalDate().toCas2UiFormat(),
        "timeNoteAdded" to savedNote.createdAt.toCas2UiFormattedHourOfDay(),
        "assessorName" to getAssessorNameOrPlaceholder(application.assessment!!),
        "applicationType" to "Home Detention Curfew (HDC)",
        "applicationUrl" to assessmentUrlTemplate.replace("#applicationId", application.id.toString()),
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

  private fun getCas2User(isExternalUser: Boolean): Cas2User = if (isExternalUser) {
    externalUserService.getUserForRequest()
  } else {
    userService.getUserForRequest()
  }

  private fun nomisUserCanAddNote(application: Cas2ApplicationEntity, user: NomisUserEntity): Boolean = if (user.id == application.getCreatedById()) {
    true
  } else {
    userAccessService.offenderIsFromSamePrisonAsUser(application.referringPrisonCode, user.activeCaseloadId)
  }

  private fun saveNote(application: Cas2ApplicationEntity, assessment: Cas2AssessmentEntity, body: String, user: Cas2User): Cas2ApplicationNoteEntity {
    val newNote = Cas2ApplicationNoteEntity(
      id = UUID.randomUUID(),
      application = application,
      body = body,
      createdAt = OffsetDateTime.now(),
      createdByUser = user,
      assessment = assessment,
    )

    return applicationNoteRepository.save(newNote)
  }
}
