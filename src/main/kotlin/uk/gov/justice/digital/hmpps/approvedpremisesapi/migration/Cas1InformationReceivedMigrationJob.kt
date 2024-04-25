package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentDomainEventService
import javax.persistence.EntityManager

class Cas1InformationReceivedMigrationJob(
  private val cas1AssessmentDomainEventService: Cas1AssessmentDomainEventService,
  private val clarificationNoteRepository: AssessmentClarificationNoteRepository,
  private val entityManager: EntityManager,
  private val migrationLogger: MigrationLogger,
  private val transactionTemplate: TransactionTemplate,
  private val pageSize: Int,
) : MigrationJob() {
  override val shouldRunInTransaction = false

  override fun process() {
    var page = 1
    var hasNext = true
    var slice: Slice<AssessmentClarificationNoteEntity>

    while (hasNext) {
      migrationLogger.info("Getting page $page for max page size $pageSize")
      slice = clarificationNoteRepository.findAllWhereHasDomainEventFalse(PageRequest.of(0, pageSize))

      slice.content.forEach { clarificationNote ->
        transactionTemplate.executeWithoutResult {
          try {
            migrationLogger.info("Creating domain event for ${clarificationNote.id}")
            cas1AssessmentDomainEventService.furtherInformationRequested(clarificationNote.assessment, clarificationNote, false)
            clarificationNoteRepository.updateHasDomainEvent(clarificationNote.id)
          } catch (e: IllegalStateException) {
            migrationLogger.error(e.message ?: "An unknown error occurred")
          } catch (e: RuntimeException) {
            val errorMessage = e.message ?: "An unknown error occurred"
            migrationLogger.error("Can't create domain event for ${clarificationNote.id} - $errorMessage", e)
          }
        }
      }

      entityManager.clear()
      hasNext = slice.hasNext()
      page += 1
    }
  }
}
