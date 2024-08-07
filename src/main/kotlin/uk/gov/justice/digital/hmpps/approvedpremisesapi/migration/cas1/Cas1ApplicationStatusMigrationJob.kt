package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import javax.persistence.EntityManager

class Cas1ApplicationStatusMigrationJob(
  private val applicationRepository: ApplicationRepository,
  private val entityManager: EntityManager,
  private val pageSize: Int,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = true

  override fun process() {
    log.info("Starting Migration process...")

    var page = 1
    var hasNext = true
    var slice: Slice<ApprovedPremisesApplicationEntity>

    while (hasNext) {
      log.info("Getting page $page")
      slice = applicationRepository.findAllWithNullStatus(PageRequest.of(0, pageSize))
      slice.content.forEach {
        setStatus(it)
      }
      entityManager.clear()
      hasNext = slice.hasNext()
      page += 1
    }
    log.info("Migration process complete!")
  }

  private fun setStatus(application: ApprovedPremisesApplicationEntity) {
    val assessment = application.getLatestAssessment()

    application.status = when {
      (assessment == null) -> ApprovedPremisesApplicationStatus.STARTED
      (assessment.allocatedToUser == null) -> ApprovedPremisesApplicationStatus.UNALLOCATED_ASSESSMENT
      (assessment.data == null) -> ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT
      (assessment.clarificationNotes.any { it.response == null }) -> ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION
      (assessment.submittedAt == null) -> ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS
      (assessment.decision == AssessmentDecision.ACCEPTED && application.getLatestBooking() == null) -> ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT
      (assessment.decision == AssessmentDecision.REJECTED) -> ApprovedPremisesApplicationStatus.REJECTED
      else -> ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED
    }

    log.info("Updating application ${application.id} to ${application.status}")
    entityManager.detach(application)
    applicationRepository.updateStatus(application.id, application.status!!)
  }
}
