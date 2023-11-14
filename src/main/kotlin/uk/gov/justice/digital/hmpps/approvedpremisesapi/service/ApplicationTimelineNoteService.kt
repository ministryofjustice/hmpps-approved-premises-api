package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteRepository
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ApplicationTimelineNoteService(
  private val applicationTimelineNoteRepository: ApplicationTimelineNoteRepository,
) {

  fun getApplicationTimelineNotesByApplicationId(applicationId: UUID): List<ApplicationTimelineNoteEntity> =
    applicationTimelineNoteRepository.findApplicationTimelineNoteEntitiesByApplicationId(applicationId)

  fun saveApplicationTimelineNotes(applicationId: UUID, applicationTimelineNote: ApplicationTimelineNote):
    ApplicationTimelineNoteEntity {
    return applicationTimelineNoteRepository.save(
      ApplicationTimelineNoteEntity(
        id = UUID.randomUUID(),
        applicationId = applicationId,
        createdBy = applicationTimelineNote.createdByUserId,
        createdAtDate = OffsetDateTime.now(),
        body = applicationTimelineNote.note,
      ),
    )
  }
}
