package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import java.time.OffsetDateTime
import java.util.UUID

@Component
class Cas1SoftDeleteApplicationTimelineNotes(
  val applicationTimelineNoteRepository: ApplicationTimelineNoteRepository,
  private val seedLogger: SeedLogger,
) : SeedJob<ApprovedPremisesApplicationIdNoteIdCsvRow>(
  requiredHeaders = setOf("applicationId", "timelineNoteId"),
) {

  override fun deserializeRow(columns: Map<String, String>) = ApprovedPremisesApplicationIdNoteIdCsvRow(
    columns["applicationId"]!!.trim(),
    columns["timelineNoteId"]!!.trim(),
  )

  override fun processRow(row: ApprovedPremisesApplicationIdNoteIdCsvRow) {
    val applicationId = row.applicationId
    val timelineNoteId = row.timelineNoteId

    val timelineNote = applicationTimelineNoteRepository.findByIdOrNull(UUID.fromString(timelineNoteId))

    timelineNote?.let {
      if (it.applicationId.toString() != applicationId) {
        seedLogger.error("Timeline note with id $timelineNoteId does not belong to application with id $applicationId.")
      } else {
        softDeleteTimelineNote(it)
      }
    } ?: seedLogger.error("Timeline note with id $timelineNoteId not found.")
  }

  private fun softDeleteTimelineNote(timelineNote: ApplicationTimelineNoteEntity) {
    timelineNote.deletedAt = OffsetDateTime.now()
    applicationTimelineNoteRepository.save(timelineNote)
    seedLogger.info("Soft deleted timeline note with id ${timelineNote.id}.")
  }
}

data class ApprovedPremisesApplicationIdNoteIdCsvRow(
  val applicationId: String,
  val timelineNoteId: String,
)
