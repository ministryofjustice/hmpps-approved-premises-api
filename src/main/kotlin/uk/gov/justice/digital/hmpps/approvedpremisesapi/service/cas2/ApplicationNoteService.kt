package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ExternalUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import java.time.OffsetDateTime
import java.util.UUID

@Service("Cas2ApplicationNoteService")
class ApplicationNoteService(
  private val applicationRepository: Cas2ApplicationRepository,
  private val applicationNoteRepository: Cas2ApplicationNoteRepository,
  private val userService: NomisUserService,
  private val externalUserService: ExternalUserService,
  private val httpAuthService: HttpAuthService,
) {

  fun createApplicationNote(applicationId: UUID, note: NewCas2ApplicationNote):
    AuthorisableActionResult<ValidatableActionResult<Cas2ApplicationNoteEntity>> {
    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val isExternalUser = httpAuthService.getCas2AuthenticatedPrincipalOrThrow().isExternalUser()
    val user = getExternalOrNomisUser(isExternalUser)

    if (!isExternalUser) {
      if (!isApplicationCreatedByUser(application, user as NomisUserEntity)) {
        return AuthorisableActionResult.Unauthorised()
      }
    }

    if (application.submittedAt == null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has not been submitted"),
      )
    }

    val savedNote = saveNote(application, note.note, user)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        savedNote,
      ),
    )
  }

  private fun getExternalOrNomisUser(isExternalUser: Boolean): Cas2User {
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
