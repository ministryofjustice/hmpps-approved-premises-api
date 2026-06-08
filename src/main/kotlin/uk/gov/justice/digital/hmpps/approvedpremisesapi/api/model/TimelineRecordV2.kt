package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant

data class TimelineRecordV2(
  val author: String,

  @field:JsonFormat(
    shape = JsonFormat.Shape.STRING,
    pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'",
    timezone = "UTC",
  )
  val commitDate: Instant,

  val message: String,

  val changes: List<TimelineChange>,
)
