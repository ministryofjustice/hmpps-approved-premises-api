package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InOutStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.PrisonerInPrisonSummary
import java.time.OffsetDateTime
import javax.persistence.EntityManager

private const val THROTTLE_DELAY_SECONDS = 1 * 1000L

class InmateStatusOnSubmissionMigrationJob(
  private val applicationRepository: ApplicationRepository,
  private val entityManager: EntityManager,
  private val pageSize: Int,
  private val throttle: Boolean,
  private val transactionTemplate: TransactionTemplate,
  private val prisonsApiClient: PrisonsApiClient,
) : MigrationJob() {

  override val shouldRunInTransaction = false

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun process() {
    var page = 1
    var hasNext = true
    var slice: Slice<ApprovedPremisesApplicationEntity>

    while (hasNext) {
      log.info("Getting page $page")
      slice = applicationRepository.getSubmittedApprovedPremisesApplicationsWithoutInOutStatus(PageRequest.of(0, pageSize))
      slice.content.forEach { application ->
        transactionTemplate.executeWithoutResult {
          updateInOutStatus(application)
        }

        if (throttle) {
          Thread.sleep(THROTTLE_DELAY_SECONDS)
        }
      }
      entityManager.clear()
      hasNext = slice.hasNext()
      page += 1
    }
  }

  private fun updateInOutStatus(application: ApprovedPremisesApplicationEntity) {
    log.info("Determine in out status for application ${application.id} submitted on ${application.submittedAt}")
    if(application.nomsNumber == null) {
      log.info("NOMS number is null, will set status to IN")
      applicationRepository.updateInOutStatus(application.id, InOutStatus.IN.name)
    } else {
      when (val timelineResult = prisonsApiClient.getPrisonTimeline(application.nomsNumber!!)) {
        is ClientResult.Success -> {
          val inOutStatus = determineInOutStatus(application.submittedAt!!, timelineResult.body)
          log.info("Determined status as $inOutStatus")
          applicationRepository.updateInOutStatus(application.id, inOutStatus.name)
        }

        is ClientResult.Failure -> {
          log.error("Unable to update application ${application.id}", timelineResult.toException())
        }
      }
    }
  }
}

fun determineInOutStatus(submissionDateTime: OffsetDateTime, inPrisonSummary: PrisonerInPrisonSummary): InOutStatus {
  val dateOfInterest = submissionDateTime.toLocalDateTime()

  val enclosingPeriod = inPrisonSummary.prisonPeriod?.firstOrNull { period ->
    period.entryDate.isBefore(dateOfInterest) &&
      (period.releaseDate == null || period.releaseDate.isAfter(dateOfInterest))
  } ?: return InOutStatus.OUT

  val inPrison = enclosingPeriod.movementDates.any { movement ->
    movement.dateInToPrison!!.isBefore(dateOfInterest) &&
      (movement.dateOutOfPrison == null || movement.dateOutOfPrison.isAfter(dateOfInterest))
  }

  return if (inPrison) InOutStatus.IN else InOutStatus.OUT
}
