package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class OASysSuitabilityService {

  @SuppressWarnings("UnusedParameter", "FunctionOnlyReturningConstant")
  fun isUsable(assessmentDates: OASysAssessmentDates) = true

  data class OASysAssessmentDates(
    val crn: String,
    val initiationDate: OffsetDateTime,
    val dateCompleted: OffsetDateTime?,
  )
}
