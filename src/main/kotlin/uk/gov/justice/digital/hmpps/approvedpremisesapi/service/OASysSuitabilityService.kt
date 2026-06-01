package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime

@Service
class OASysSuitabilityService(
  private val clock: Clock,
  private val sentryService: SentryService,
) {
  fun isSuitable(info: OASysAssessmentDates): Boolean {
    val sixMonthThreshold = OffsetDateTime.now(clock).minusMonths(6)

    val crn = info.crn
    val initiationDate = info.initiationDate
    val dateCompleted = info.dateCompleted

    val suitable = (dateCompleted ?: initiationDate).isAfter(sixMonthThreshold)

    if (dateCompleted == null) {
      sentryService.captureErrorMessage("No completion date defined on assessment for $crn. Using initiation date/time of $initiationDate")
    } else if (!suitable) {
      sentryService.captureErrorMessage("Have received an assessment with a completion date/time more than 6 months ago for $crn and date/time $dateCompleted")
    }

    return suitable
  }

  data class OASysAssessmentDates(
    val crn: String,
    val initiationDate: OffsetDateTime,
    val dateCompleted: OffsetDateTime?,
  )
}
