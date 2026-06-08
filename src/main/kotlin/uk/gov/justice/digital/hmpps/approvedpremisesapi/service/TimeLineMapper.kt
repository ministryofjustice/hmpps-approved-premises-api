package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.javers.core.diff.Change
import org.javers.core.diff.changetype.InitialValueChange
import org.javers.core.diff.changetype.NewObject
import org.javers.core.diff.changetype.ObjectRemoved
import org.javers.core.diff.changetype.PropertyChange
import org.javers.core.diff.changetype.TerminalValueChange
import org.javers.core.diff.changetype.ValueChange
import org.javers.core.diff.changetype.container.ContainerChange
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AuditEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ChangeType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineSection

@Component
class TimeLineMapper {

  fun buildSections(changes: List<Change>): List<TimelineSection> = changes
    .groupBy { it.toAuditEntityType() }
    .filter { it.key != null }
    .map { (entityType, changesForType) ->
      TimelineSection(
        entityType = entityType!!,
        changes = changesForType.map { it.toTimelineChange() },
      )
    }

  private fun Change.toAuditEntityType(): AuditEntityType? = when {
    affectedGlobalId.value().contains("ReleaseAction") -> AuditEntityType.RELEASE_ACTION
    affectedGlobalId.value().contains("ReleasePlan") -> AuditEntityType.RELEASE_PLAN
    affectedGlobalId.value().contains("MonitoringInformation") -> AuditEntityType.MONITORING_INFORMATION
    else -> null
  }

  private fun Change.toTimelineChange(): TimelineChange = when (this) {
    is InitialValueChange -> TimelineChange(
      changeType = ChangeType.CREATE,
      message = "Created ${objectLabel()} with ${fieldLabel()} '${right.toTimelineValue()}'",
      field = fieldLabel(),
      newValue = right.toTimelineValue(),
    )

    is TerminalValueChange -> TimelineChange(
      changeType = ChangeType.DELETE,
      message = "Deleted ${objectLabel()} with ${fieldLabel()} '${left.toTimelineValue()}'",
      field = fieldLabel(),
      oldValue = left.toTimelineValue(),
    )

    is ValueChange -> TimelineChange(
      changeType = ChangeType.UPDATE,
      message = "${fieldLabel().capitalize()} changed from '${left.toTimelineValue()}' to '${right.toTimelineValue()}'",
      field = fieldLabel(),
      oldValue = left.toTimelineValue(),
      newValue = right.toTimelineValue(),
    )

    is ContainerChange<*> ->
      TimelineChange(
        changeType = ChangeType.UPDATE,
        message = "${fieldLabel().capitalize()} changed from '${left.toTimelineValue()}' to '${right.toTimelineValue()}'",
        field = fieldLabel(),
        oldValue = left.toTimelineValue(),
        newValue = right.toTimelineValue(),
      )

    is NewObject -> TimelineChange(
      changeType = ChangeType.CREATE,
      message = "Created ${objectLabel()}",
    )

    is ObjectRemoved -> TimelineChange(
      changeType = ChangeType.DELETE,
      message = "Deleted ${objectLabel()}",
    )

    else -> TimelineChange(
      changeType = ChangeType.UPDATE,
      message = "Updated ${objectLabel()}",
    )
  }

  private fun PropertyChange<*>.fieldLabel(): String = getPropertyNameWithPath().toDisplayName()

  private fun Change.objectLabel(): String = when {
    affectedGlobalId.value().contains("ReleaseAction") -> "release action"
    affectedGlobalId.value().contains("ReleasePlan") -> "release plan"
    affectedGlobalId.value().contains("MonitoringInformation") -> "monitoring information"
    else -> "object"
  }

  private fun String.toDisplayName(): String = split(Regex("(?=\\p{Upper})|_"))
    .filter { it.isNotBlank() }
    .joinToString(" ") { it.lowercase() }

  private fun Any?.toTimelineValue(): String? = when (this) {
    null -> null
    is Collection<*> -> joinToString(prefix = "[", postfix = "]") { it.toTimelineValue().orEmpty() }
    else -> toString()
  }

  private fun String.capitalize() = replaceFirstChar { it.uppercase() }
}
