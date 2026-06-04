package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant

/**
 * {
 *   "author": "audit_user",
 *   "commitDate": "2026-06-03T06:35:03Z",
 *   "sections": [
 *     {
 *       "entityType": "RELEASE_PLAN",
 *       "changes": [
 *         {
 *           "changeType": "UPDATE",
 *           "field": "description",
 *           "oldValue": "Initial Plan",
 *           "newValue": "Updated Plan",
 *           "message": "Description changed from 'Initial Plan' to 'Updated Plan'"
 *         }
 *       ]
 *     },
 *     {
 *       "entityType": "RELEASE_ACTION",
 *       "changes": [
 *         {
 *           "changeType": "CREATE",
 *           "message": "Created release action with description 'Action 2'"
 *         },
 *         {
 *           "changeType": "DELETE",
 *           "message": "Deleted release action with description 'Action 1'"
 *         }
 *       ]
 *     }
 *   ]
 * }
 */

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
}

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
