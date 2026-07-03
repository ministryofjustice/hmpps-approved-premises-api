package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.AssessmentInfo
import java.time.Clock
import java.time.OffsetDateTime

@Service
class OASysSuitabilityService(
  private val clock: Clock,
  private val sentryService: SentryService,
) {
  var logger: Logger = LoggerFactory.getLogger(this::class.java)

  fun isSuitable(
    crn: String,
    assessmentInfo: AssessmentInfo,
    strategy: SuitabilityStrategy,
  ) = isSuitable(
    assessmentDates = assessmentInfo.toAssessmentDates(crn),
    strategy = strategy,
  )

  fun isSuitable(
    assessmentDates: OASysAssessmentDates,
    strategy: SuitabilityStrategy,
  ) = when (strategy) {
    SuitabilityStrategy.AllowAll -> true
    SuitabilityStrategy.CompletedInLastSixMonths -> limitToSixMonths(assessmentDates)
  }

  private fun limitToSixMonths(
    assessmentDates: OASysAssessmentDates,
  ): Boolean {
    val sixMonthThreshold = OffsetDateTime.now(clock).minusMonths(6)

    val crn = assessmentDates.crn
    val initiationDate = assessmentDates.initiationDate
    val dateCompleted = assessmentDates.dateCompleted

    logger.info("Retrieved an assessment with date completed of ${dateCompleted?.toLocalDate()} for CRN $crn")

    if (dateCompleted == null) {
      sentryService.captureErrorMessage("No completion date defined on assessment for $crn. Using initiation date/time of $initiationDate")
    }

    return (dateCompleted ?: initiationDate).isAfter(sixMonthThreshold)
  }

  data class OASysAssessmentDates(
    val crn: String,
    val initiationDate: OffsetDateTime,
    val dateCompleted: OffsetDateTime?,
  )

  enum class SuitabilityStrategy {
    AllowAll,
    CompletedInLastSixMonths,
  }
}

fun AssessmentInfo.toAssessmentDates(crn: String) = OASysSuitabilityService.OASysAssessmentDates(
  crn = crn,
  initiationDate = initiationDate,
  dateCompleted = dateCompleted,
)
