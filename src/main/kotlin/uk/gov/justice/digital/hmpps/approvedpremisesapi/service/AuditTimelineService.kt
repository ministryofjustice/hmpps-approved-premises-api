package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import jakarta.persistence.EntityManager
import org.hibernate.envers.AuditReader
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity
import org.springframework.data.history.RevisionMetadata
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleaseActionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleasePlanEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RevInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuditTimeLine
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuditTimeLineSection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RevisionSnapshot
import java.util.UUID

@Service
class AuditTimelineService(
  private val entityManager: EntityManager,
) {

  private val auditReader: AuditReader
    get() = AuditReaderFactory.get(entityManager)

  @Transactional(readOnly = true)
  fun getTimelineForSpaceBooking(
    spaceBooking: Cas1SpaceBookingEntity,
  ): List<AuditTimeLine> {
    val snapshots = loadSnapshots(spaceBooking)

    return buildTimeline(snapshots)
  }

  private fun loadSnapshots(
    spaceBooking: Cas1SpaceBookingEntity,
  ): List<RevisionSnapshot> {
    val planEvents = loadPlanEvents(spaceBooking)
    val actionEvents = loadActionEvents(planEvents.map { it.entityId }.toSet())

    return (planEvents + actionEvents)
      .sortedWith(
        compareBy<RevisionSnapshot> { it.revisionNumber }
          .thenBy { it.timestamp },
      )
  }

  private fun loadPlanEvents(
    spaceBooking: Cas1SpaceBookingEntity,
  ): List<RevisionSnapshot> = auditReader.createQuery()
    .forRevisionsOfEntity(ReleasePlanEntity::class.java, false, true)
    .add(AuditEntity.property("spaceBooking").eq(spaceBooking))
    .resultList
    .map { it?.toRevisionSnapshot(EventType.RELEASE_PLAN) as RevisionSnapshot }

  private fun loadActionEvents(
    releasePlanIds: Set<UUID>,
  ): List<RevisionSnapshot> {
    if (releasePlanIds.isEmpty()) {
      return emptyList()
    }

    val actionIds = entityManager
      .createNativeQuery(
        """
        SELECT DISTINCT id
        FROM release_plan_release_action_aud
        WHERE release_plan_id IN (:releasePlanIds)
        """.trimIndent(),
      )
      .setParameter("releasePlanIds", releasePlanIds)
      .resultList
      .filterIsInstance<UUID>()

    if (actionIds.isEmpty()) {
      return emptyList()
    }

    return auditReader.createQuery()
      .forRevisionsOfEntity(ReleaseActionEntity::class.java, false, true)
      .add(AuditEntity.id().`in`(actionIds))
      .resultList
      .map { it?.toRevisionSnapshot(EventType.RELEASE_ACTION) as RevisionSnapshot }
  }

  private fun Any.toRevisionSnapshot(type: EventType): RevisionSnapshot {
    val arr = this as Array<*>

    val entity = arr[0]
    val revInfo = arr[1] as RevInfo
    val revisionType = arr[2] as RevisionType

    return RevisionSnapshot(
      revisionNumber = revInfo.id,
      timestamp = revInfo.getRevisionInstant(),
      user = revInfo.username,
      type = type,
      entityType = entity!!::class.simpleName ?: "Unknown",
      entityId = extractId(entity),
      entity = entity,
      revisionType = revisionType.toRevisionMetadataType(),
    )
  }

  private fun buildTimeline(
    snapshots: List<RevisionSnapshot>,
  ): List<AuditTimeLine> {
    val grouped = snapshots.groupBy { it.revisionNumber }

    val sortedRevisionNumbers = grouped.keys.sortedBy { it }

    val timeline = mutableListOf<AuditTimeLine>()

    val planHistory = mutableMapOf<UUID, ReleasePlanEntity>()
    val actionHistory = mutableMapOf<UUID, ReleaseActionEntity>()

    sortedRevisionNumbers.forEach { rev ->

      val events = grouped[rev].orEmpty()

      val sections = mutableListOf<AuditTimeLineSection>()

      val first = events.first()

      events.forEach { event ->

        when (event.type) {
          EventType.RELEASE_PLAN -> {
            val old = planHistory[event.entityId]
            val new = event.entity

            val diff = getDiff(old, new, event.revisionType)

            if (diff.isNotEmpty()) {
              sections += AuditTimeLineSection(
                title = "Release Plan",
                content = diff,
              )
            }

            if (event.revisionType == RevisionMetadata.RevisionType.DELETE) {
              planHistory.remove(event.entityId)
            } else {
              planHistory[event.entityId] = new!! as ReleasePlanEntity
            }
          }

          EventType.RELEASE_ACTION -> {
            val old = actionHistory[event.entityId]
            val new = event.entity

            val diff = getDiff(old, new, event.revisionType)

            if (diff.isNotEmpty()) {
              sections += AuditTimeLineSection(
                title = "Release Actions",
                content = diff,
              )
            }

            if (event.revisionType == RevisionMetadata.RevisionType.DELETE) {
              actionHistory.remove(event.entityId)
            } else {
              actionHistory[event.entityId] = new!! as ReleaseActionEntity
            }
          }
        }
      }

      timeline += AuditTimeLine(
        revisionNumber = rev!!,
        occurredAt = first.timestamp,
        userName = first.user,
        title = "Release plan updated",
        sections = sections,
      )
    }

    return timeline.sortedByDescending { it.revisionNumber }
  }

  private fun getDiff(
    old: Any?,
    new: Any?,
    revisionType: RevisionMetadata.RevisionType,
  ): MutableList<String> {
    val changes = mutableListOf<String>()

    if (revisionType == RevisionMetadata.RevisionType.DELETE) {
      val deletedEntity = old ?: new
      when (deletedEntity) {
        is ReleasePlanEntity ->
          changes.add("Deleted release plan with description '${deletedEntity.description}'")

        is ReleaseActionEntity ->
          changes.add("Deleted release action with description '${deletedEntity.description}'")
      }
      return changes
    }

    if (old == null) {
      when (new) {
        is ReleasePlanEntity ->
          changes.add("Created release plan with description '${new.description}'")

        is ReleaseActionEntity ->
          changes.add("Created release action with description '${new.description}'")
      }
      return changes
    }

    when (new) {
      is ReleasePlanEntity -> {
        val oldEntity = old as ReleasePlanEntity
        changes.addIfChanged("Description", oldEntity.description, new.description)
        changes.addIfChanged("Expected release time", oldEntity.expectedReleaseTime, new.expectedReleaseTime)
        changes.addIfChanged("Expected arrival time", oldEntity.expectedArrivalTime, new.expectedArrivalTime)
        changes.addIfChanged("Other information", oldEntity.otherInformation, new.otherInformation)
      }

      is ReleaseActionEntity -> {
        val oldEntity = old as ReleaseActionEntity
        changes.addIfChanged("Description", oldEntity.description, new.description)
        changes.addIfChanged("Action cadence", oldEntity.actionCadence, new.actionCadence)
      }
    }

    return changes
  }

  private fun MutableList<String>.addIfChanged(fieldName: String, oldValue: Any?, newValue: Any?) {
    if (oldValue != newValue) {
      add("$fieldName changed from '$oldValue' to '$newValue'")
    }
  }

  private fun extractId(entity: Any): UUID = entity::class.java.methods
    .first { it.name == "getId" }
    .invoke(entity) as UUID

  private fun RevisionType.toRevisionMetadataType(): RevisionMetadata.RevisionType = when (this) {
    RevisionType.ADD -> RevisionMetadata.RevisionType.INSERT
    RevisionType.MOD -> RevisionMetadata.RevisionType.UPDATE
    RevisionType.DEL -> RevisionMetadata.RevisionType.DELETE
  }
}
