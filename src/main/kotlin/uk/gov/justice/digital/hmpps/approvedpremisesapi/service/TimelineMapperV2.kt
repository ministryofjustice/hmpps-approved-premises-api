package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.javers.core.ChangesByCommit
import org.javers.core.diff.Change
import org.javers.core.diff.changetype.NewObject
import org.javers.core.diff.changetype.ObjectRemoved
import org.javers.core.diff.changetype.PropertyChange
import org.javers.core.diff.changetype.ValueChange
import org.javers.core.diff.changetype.container.ContainerChange
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ChangeType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineRecordV2

@Component
class TimelineMapperV2 {

  fun toTimelineRecord(commitChanges: ChangesByCommit): TimelineRecordV2 {
    val commit = commitChanges.commit

    val changes = commitChanges.get()
      .filterNot { it.isNoise() }
      .mapNotNull { it.toTimelineChange() }

    return TimelineRecordV2(
      author = commit.author,
      commitDate = commit.commitDateInstant,
      message = buildCommitMessage(changes),
      changes = changes,
    )
  }

  private fun buildCommitMessage(changes: List<TimelineChange>): String {
    val hasCreate = changes.any { it.changeType == ChangeType.CREATE }
    val hasUpdate = changes.any { it.changeType == ChangeType.UPDATE }
    val hasDelete = changes.any { it.changeType == ChangeType.DELETE }

    return when {
      hasCreate && hasUpdate -> "Release plan updated"
      hasCreate -> "Release plan created"
      hasDelete && !hasCreate -> "Release plan deleted"
      else -> "Release plan updated"
    }
  }

  private fun Change.toTimelineChange(): TimelineChange? = when (this) {
    /**
     * For CREATE commit
     */
    /*    is InitialValueChange -> TimelineChange(
      changeType = ChangeType.CREATE,
      message = "Created ${objectLabel()} with ${fieldLabel()} '${right.toTimelineValue()}'",
      field = fieldLabel(),
      newValue = right.toTimelineValue(),
    )*/

    /**
     * For DELTET commit
     */

    /*    is TerminalValueChange -> TimelineChange(
          changeType = ChangeType.DELETE,
          message = "Deleted ${objectLabel()} with ${fieldLabel()} '${left.toTimelineValue()}'",
          field = fieldLabel(),
          oldValue = left.toTimelineValue(),
        )*/

    /**
     * For UPDATE commit using collections
     */

    /*    is ContainerChange<*> -> TimelineChange(
          changeType = ChangeType.UPDATE,
          message = "${fieldLabel().capitalize()} changed",
          field = fieldLabel(),
          oldValue = left.toTimelineValue(),
          newValue = right.toTimelineValue(),
        )*/

    is ValueChange ->
      if (left == null || right == null) {
        return null
      } else {
        TimelineChange(
          changeType = ChangeType.UPDATE,
          message = "${fieldLabel().capitalize()} changed from '${left.toTimelineValue()}' to '${right.toTimelineValue()}'",
          field = fieldLabel(),
          oldValue = left.toTimelineValue(),
          newValue = right.toTimelineValue(),
        )
      }

    is NewObject -> TimelineChange(
      changeType = ChangeType.CREATE,
      message = "Created ${objectLabel()}",
    )

    is ObjectRemoved -> TimelineChange(
      changeType = ChangeType.DELETE,
      message = "Deleted ${objectLabel()}",
    )

    else -> null
  }

  private fun Change.isNoise(): Boolean = this is ContainerChange<*> &&
    getPropertyNameWithPath() in setOf(
      "releaseActions",
      "monitoringInformation",
    )

  private fun PropertyChange<*>.fieldLabel(): String = getPropertyNameWithPath()
    .split(Regex("(?=\\p{Upper})|_"))
    .filter { it.isNotBlank() }
    .joinToString(" ") { it.lowercase() }

  private fun Change.objectLabel(): String = when {
    affectedGlobalId.value().contains("ReleaseAction") -> "release action"
    affectedGlobalId.value().contains("ReleasePlan") -> "release plan"
    affectedGlobalId.value().contains("MonitoringInformation") -> "monitoring information"
    else -> "object"
  }

  private fun Any?.toTimelineValue(): String? = when (this) {
    null -> null
    is Collection<*> -> joinToString(prefix = "[", postfix = "]") { it.toTimelineValue().orEmpty() }
    else -> toString()
  }

  private fun String.capitalize(): String = replaceFirstChar { it.uppercase() }
}
