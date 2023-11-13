package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import io.sentry.Sentry
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import java.util.stream.Stream
import javax.persistence.EntityManager

class ApplicationStatusMigrationJob(
  private val applicationRepository: ApplicationRepository,
  private val entityManager: EntityManager,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = true

  @Suppress("UNCHECKED_CAST")
  override fun process() {
    try {
      val applications = applicationRepository.findAllForService(
        ApprovedPremisesApplicationEntity::class.java,
      ) as Stream<ApprovedPremisesApplicationEntity>
      applications.peek(entityManager::detach).forEach {
        setStatus(it)
      }
    } catch (e: Exception) {
      Sentry.captureException(e)
    }
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
    applicationRepository.saveAndFlush(application)
  }
}
