package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.http.HttpStatus
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
    val inOutStatus = if (application.nomsNumber == null) InOutStatus.IN else getInOutStatusFromPrisonTimeline(application)

    if (inOutStatus != null) {
      log.info("Determined status as $inOutStatus")
      applicationRepository.updateInOutStatus(application.id, inOutStatus.name)
    } else {
      log.info("Could not determine in out status for application ${application.id}")
    }
  }

  private fun getInOutStatusFromPrisonTimeline(application: ApprovedPremisesApplicationEntity): InOutStatus? {
    return when (val timelineResult = prisonsApiClient.getPrisonTimeline(application.nomsNumber!!)) {
      is ClientResult.Success -> {
        determineInOutStatus(application.submittedAt!!, timelineResult.body)
      }

      is ClientResult.Failure -> {
        if (timelineResult is ClientResult.Failure.StatusCode && timelineResult.status == HttpStatus.NOT_FOUND) {
          log.info("404 retrieving prison timeline for application ${application.id} will mark status as IN")
          InOutStatus.IN
        } else {
          log.error(
            "Error retrieving prison timeline for application ${application.id}",
            timelineResult.toException(),
          )
          null
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
