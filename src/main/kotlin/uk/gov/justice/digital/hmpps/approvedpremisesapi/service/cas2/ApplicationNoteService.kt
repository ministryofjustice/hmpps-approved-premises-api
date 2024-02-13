package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import java.time.OffsetDateTime
import java.util.UUID

@Service("Cas2ApplicationNoteService")
class ApplicationNoteService(
  private val applicationRepository: Cas2ApplicationRepository,
  private val applicationNoteRepository: Cas2ApplicationNoteRepository,
  private val userService: NomisUserService,
) {

  fun createApplicationNote(applicationId: UUID, note: NewCas2ApplicationNote):
    AuthorisableActionResult<ValidatableActionResult<Cas2ApplicationNoteEntity>> {
    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application.submittedAt == null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has not been submitted"),
      )
    }

    val note = Cas2ApplicationNoteEntity(
      id = UUID.randomUUID(),
      application = application,
      body = note.note,
      createdAt = OffsetDateTime.now(),
      createdByNomisUser = user,
    )

    val savedNote = applicationNoteRepository.save(note)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        savedNote,
      ),
    )
  }
}
