package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas2ApplicationNoteService(
  private val cas2ApplicationRepository: Cas2ApplicationRepository,
  private val cas2AssessmentRepository: Cas2AssessmentRepository,
  private val cas2ApplicationNoteRepository: Cas2ApplicationNoteRepository,
  private val userService: Cas2UserService,
  private val userAccessService: Cas2UserAccessService,
  private val cas2ApplicationNoteEmailService: Cas2ApplicationNoteEmailService,
) {

  @Suppress("ReturnCount")
  fun createAssessmentNote(assessmentId: UUID, note: NewCas2ApplicationNote): CasResult<Cas2ApplicationNoteEntity> {
    val assessment = cas2AssessmentRepository.findByIdAndServiceOrigin(assessmentId, Cas2ServiceOrigin.BAIL)
      ?: return CasResult.NotFound("Cas2ApplicationNoteEntity", assessmentId.toString())

    val application = cas2ApplicationRepository.findByIdAndServiceOrigin(assessment.application.id, assessment.serviceOrigin)
      ?: return CasResult.NotFound("Cas2ApplicationNoteEntity", assessmentId.toString())

    if (application.submittedAt == null) {
      return CasResult.GeneralValidationError("This application has not been submitted")
    }

    val user = userService.getUserForRequest()

    if (!userAccessService.userCanAddNote(user, application)) {
      return CasResult.Unauthorised()
    }

    val savedNote = saveNote(application, assessment, note.note, user)
    sendEmail(user.isExternal(), application, assessment, savedNote)

    return CasResult.Success(savedNote)
  }

  private fun sendEmail(
    isExternalUser: Boolean,
    application: Cas2ApplicationEntity,
    assessment: Cas2AssessmentEntity,
    savedNote: Cas2ApplicationNoteEntity,
  ) {
    if (isExternalUser) {
      cas2ApplicationNoteEmailService.assessorNoteAdded(application, assessment, savedNote)
    } else {
      cas2ApplicationNoteEmailService.refererNoteAdded(application, assessment, savedNote)
    }
  }

  private fun saveNote(application: Cas2ApplicationEntity, assessment: Cas2AssessmentEntity, body: String, user: Cas2UserEntity): Cas2ApplicationNoteEntity {
    val newNote = Cas2ApplicationNoteEntity(
      id = UUID.randomUUID(),
      application = application,
      body = body,
      createdAt = OffsetDateTime.now(),
      createdByUser = user,
      assessment = assessment,
    )

    return cas2ApplicationNoteRepository.save(newNote)
  }
}
