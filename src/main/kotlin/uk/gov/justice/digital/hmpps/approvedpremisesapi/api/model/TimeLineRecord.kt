package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TimelineRecord(
  val author: String,

  @field:JsonFormat(
    shape = JsonFormat.Shape.STRING,
    pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'",
    timezone = "UTC",
  )
  val commitDate: Instant,

  val sections: List<TimelineSection>,
)

data class TimelineSection(
  val entityType: AuditEntityType,
  val changes: List<TimelineChange>,
)

enum class AuditEntityType {
  RELEASE_PLAN,
  RELEASE_ACTION,
  MONITORING_INFORMATION,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TimelineChange(

  val changeType: ChangeType,

  /**
   * Message for UI.
   */
  val message: String,

  /**
   * Only for UPDATE changes.
   */
  val field: String? = null,

  val oldValue: String? = null,

  val newValue: String? = null,
)

enum class ChangeType {
  CREATE,
  UPDATE,
  DELETE,
}
