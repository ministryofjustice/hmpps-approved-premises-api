package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import com.amazonaws.services.sns.model.NotFoundException
import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ExternalUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormattedHourOfDay
import java.time.OffsetDateTime
import java.util.UUID

@Service("Cas2ApplicationNoteService")
class ApplicationNoteService(
  private val applicationRepository: Cas2ApplicationRepository,
  private val applicationNoteRepository: Cas2ApplicationNoteRepository,
  private val userService: NomisUserService,
  private val externalUserService: ExternalUserService,
  private val httpAuthService: HttpAuthService,
  private val emailNotificationService: EmailNotificationService,
  private val notifyConfig: NotifyConfig,
  @Value("\${url-templates.frontend.cas2.application-overview}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2.submitted-application-overview}") private val assessmentUrlTemplate: String,
) {

  private val log = LoggerFactory.getLogger(this::class.java)
  fun createApplicationNote(applicationId: UUID, note: NewCas2ApplicationNote):
    AuthorisableActionResult<ValidatableActionResult<Cas2ApplicationNoteEntity>> {
    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    if (application.submittedAt == null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has not been submitted"),
      )
    }

    val isExternalUser = httpAuthService.getCas2AuthenticatedPrincipalOrThrow().isExternalUser()
    val user = getCas2User(isExternalUser)

    if (!isExternalUser && !isApplicationCreatedByUser(application, user as NomisUserEntity)) {
      return AuthorisableActionResult.Unauthorised()
    }

    val savedNote = saveNote(application, note.note, user)

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
    if (application.createdByUser.email != null) {
      emailNotificationService.sendCas2Email(
        recipientEmailAddress = application.createdByUser.email!!,
        templateId = notifyConfig.templates.cas2NoteAddedForReferrer,
        personalisation = mapOf(
          "dateNoteAdded" to savedNote.createdAt.toLocalDate().toCas2UiFormat(),
          "timeNoteAdded" to savedNote.createdAt.toCas2UiFormattedHourOfDay(),
          "nomsNumber" to application.nomsNumber,
          "applicationType" to "Home Detention Curfew (HDC)",
          "applicationUrl" to applicationUrlTemplate.replace("#id", application.id.toString()),
        ),
      )
    } else {
      log.error("Email not found for User ${application.createdByUser.id}. Unable to send email for Note ${savedNote.id} on Application ${application.id}")
      Sentry.captureException(
        RuntimeException(
          "Email not found for User ${application.createdByUser.id}. Unable to send email for Note ${savedNote.id} on Application ${application.id}",
          NotFoundException("Email not found for User ${application.createdByUser.id}"),
        ),
      )
    }
  }

  private fun sendEmailToAssessors(
    application: Cas2ApplicationEntity,
    savedNote: Cas2ApplicationNoteEntity,
  ) {
    emailNotificationService.sendCas2Email(
      recipientEmailAddress = notifyConfig.emailAddresses.cas2Assessors,
      templateId = notifyConfig.templates.cas2NoteAddedForAssessor,
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

  private fun getCas2User(isExternalUser: Boolean): Cas2User {
    return if (isExternalUser) {
      externalUserService.getUserForRequest()
    } else {
      userService.getUserForRequest()
    }
  }

  private fun isApplicationCreatedByUser(application: Cas2ApplicationEntity, user: NomisUserEntity) =
    application.createdByUser.id == user.id

  private fun saveNote(application: Cas2ApplicationEntity, body: String, user: Cas2User): Cas2ApplicationNoteEntity {
    val newNote = Cas2ApplicationNoteEntity(
      id = UUID.randomUUID(),
      application = application,
      body = body,
      createdAt = OffsetDateTime.now(),
      createdByUser = user,
    )

    return applicationNoteRepository.save(newNote)
  }
}
