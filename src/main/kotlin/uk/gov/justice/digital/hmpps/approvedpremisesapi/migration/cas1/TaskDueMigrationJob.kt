package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TaskDeadlineService
import javax.persistence.EntityManager

class TaskDueMigrationJob(
  private val assessmentRepository: AssessmentRepository,
  private val placementApplicationRepository: PlacementApplicationRepository,
  private val placementRequestRepository: PlacementRequestRepository,
  private val entityManager: EntityManager,
  private val taskDeadlineService: TaskDeadlineService,
  private val pageSize: Int,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = true

  override fun process() {
    log.info("Starting Migration process...")

    this.updateAssessments()
    this.updatePlacementApplications()
    this.updatePlacementRequests()
  }

  private fun updateAssessments() {
    log.info("Updating assessments....")
    var page = 1
    var hasNext = true
    var slice: Slice<ApprovedPremisesAssessmentEntity>

    while (hasNext) {
      log.info("Getting page $page")
      slice = assessmentRepository.findAllWithNullDueAt(PageRequest.of(0, pageSize))
      slice.content.forEach {
        assessmentRepository.updateDueAt(it.id, taskDeadlineService.getDeadline(it))
        entityManager.detach(it)
      }
      entityManager.clear()
      hasNext = slice.hasNext()
      page += 1
    }
  }

  private fun updatePlacementApplications() {
    log.info("Updating placement applications....")
    var page = 1
    var hasNext = true
    var slice: Slice<PlacementApplicationEntity>

    while (hasNext) {
      log.info("Getting page $page")
      slice = placementApplicationRepository.findAllWithNullDueAt(PageRequest.of(0, pageSize))
      slice.content.forEach {
        placementApplicationRepository.updateDueAt(it.id, taskDeadlineService.getDeadline(it))
        entityManager.detach(it)
      }
      entityManager.clear()
      hasNext = slice.hasNext()
      page += 1
    }
  }

  private fun updatePlacementRequests() {
    log.info("Updating placement requests....")
    var page = 1
    var hasNext = true
    var slice: Slice<PlacementRequestEntity>

    while (hasNext) {
      log.info("Getting page $page")
      slice = placementRequestRepository.findAllWithNullDueAt(PageRequest.of(0, pageSize))
      slice.content.forEach {
        placementRequestRepository.updateDueAt(it.id, taskDeadlineService.getDeadline(it))
        entityManager.detach(it)
      }
      entityManager.clear()
      hasNext = slice.hasNext()
      page += 1
    }
  }
}
