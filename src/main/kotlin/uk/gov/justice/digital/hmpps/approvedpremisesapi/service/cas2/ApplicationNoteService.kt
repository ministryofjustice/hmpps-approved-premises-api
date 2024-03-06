package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
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
) {
  fun createApplicationNote(applicationId: UUID, note: NewCas2ApplicationNote):
    AuthorisableActionResult<ValidatableActionResult<Cas2ApplicationNoteEntity>> {
    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val isExternalUser = httpAuthService.getCas2AuthenticatedPrincipalOrThrow().isExternalUser()
    val user = getCas2User(isExternalUser)

    if (!isExternalUser && !isApplicationCreatedByUser(application, user as NomisUserEntity)) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application.submittedAt == null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has not been submitted"),
      )
    }

    val savedNote = saveNote(application, note.note, user)

    if (isExternalUser) {
      sendEmailToReferrer(application, savedNote)
    }

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        savedNote,
      ),
    )
  }

  private fun sendEmailToReferrer(application: Cas2ApplicationEntity, savedNote: Cas2ApplicationNoteEntity) {
    emailNotificationService.sendEmail(
      // TODO should we check if email exists? Throw error if it doesn't
      recipientEmailAddress = application.createdByUser.email!!,
      templateId = notifyConfig.templates.cas2NoteAddedForReferrer,
      personalisation = mapOf(
        "dateNoteAdded" to savedNote.createdAt.toLocalDate().toCas2UiFormat(),
        "timeNoteAdded" to savedNote.createdAt.toCas2UiFormattedHourOfDay(),
        "nomsNumber" to application.nomsNumber,
        "applicationType" to "Home Detention Curfew (HDC)",
        "applicationURl" to applicationUrlTemplate.replace("#id", application.id.toString()),
      ),
    )
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
