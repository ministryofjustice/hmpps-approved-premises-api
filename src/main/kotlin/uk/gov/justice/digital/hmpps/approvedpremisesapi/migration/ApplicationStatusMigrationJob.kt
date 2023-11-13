package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus

class ApplicationStatusMigrationJob(
  private val applicationRepository: ApplicationRepository,
  private val pageSize: Int,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = true

  override fun process() {
    var slice = getApplications(PageRequest.of(0, pageSize))
    val applications = slice.content

    applications.forEach {
      setStatus(it)
    }

    while (slice.hasNext()) {
      slice = getApplications(slice.nextPageable())
      slice.get().forEach {
        setStatus(it)
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun getApplications(pageable: Pageable) = applicationRepository.findAllForService(
    ApprovedPremisesApplicationEntity::class.java,
    pageable,
  ) as Slice<ApprovedPremisesApplicationEntity>

  private fun setStatus(application: ApprovedPremisesApplicationEntity) {
    val assessment = application.getLatestAssessment()

    application.status = when {
      (assessment == null) -> ApprovedPremisesApplicationStatus.STARTED
      (assessment.allocatedToUser == null) -> ApprovedPremisesApplicationStatus.UNALLOCATED_ASSESSMENT
      (assessment.data == null) -> ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT
      (assessment.submittedAt == null) -> ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS
      (assessment.decision == AssessmentDecision.ACCEPTED && application.getLatestBooking() == null) -> ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT
      (assessment.decision == AssessmentDecision.REJECTED) -> ApprovedPremisesApplicationStatus.REJECTED
      else -> ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED
    }

    log.info("Updating application ${application.id} to ${application.status}")
    applicationRepository.save(application)
  }
}
