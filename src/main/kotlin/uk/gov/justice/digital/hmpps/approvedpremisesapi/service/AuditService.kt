package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import jakarta.persistence.EntityManager
import org.hibernate.envers.AuditReader
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.RevisionNumber
import org.hibernate.envers.RevisionTimestamp
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity
import org.springframework.data.history.AnnotationRevisionMetadata
import org.springframework.data.history.Revision
import org.springframework.data.history.RevisionMetadata
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleaseActionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleasePlanEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleasePlanRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RevInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuditTimeLine
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuditTimeLineSection
import java.util.UUID

@Service
class AuditService(
  private val releasePlanRepository: ReleasePlanRepository,
  private val entityManager: EntityManager,
) {

  private val auditReader: AuditReader
    get() = AuditReaderFactory.get(entityManager)

  @Transactional(readOnly = true)
  fun getReleasePlanRevisions(
    releasePlanId: UUID,
  ): List<Revision<Int, ReleasePlanEntity>> = releasePlanRepository.findRevisions(releasePlanId)
    .content
    .toList()

  @Transactional(readOnly = true)
  fun getReleaseActionRevisions(
    releaseActionId: UUID,
  ): List<Revision<Int, ReleaseActionEntity>> = auditReader.createQuery()
    .forRevisionsOfEntity(ReleaseActionEntity::class.java, false, true)
    .add(AuditEntity.id().eq(releaseActionId))
    .resultList
    .map { it.toReleaseActionRevision() }

  @Transactional(readOnly = true)
  fun getRevisionsForSpaceBooking(
    spaceBooking: Cas1SpaceBookingEntity,
  ): List<Revision<Int, ReleasePlanEntity>> = releasePlanRepository.getBySpaceBooking(spaceBooking)
    ?.flatMap { releasePlan ->
      releasePlanRepository.findRevisions(releasePlan.id).content
    }
    ?.sortedBy { revision ->
      revision.metadata.revisionNumber.orElse(0)
    }
    ?: emptyList()

  @Transactional(readOnly = true)
  fun getAuditTimeLineForSpaceBooking(
    spaceBooking: Cas1SpaceBookingEntity,
  ): List<AuditTimeLine> {
    val releasePlans = releasePlanRepository.getBySpaceBooking(spaceBooking).orEmpty()
    if (releasePlans.isEmpty()) {
      return emptyList()
    }

    val planRevisionsById = releasePlans.associate { releasePlan ->
      releasePlan.id to releasePlanRepository.findRevisions(releasePlan.id).content.toList()
    }
    val actionRevisionsById = releasePlans
      .flatMap { it.releaseActions }
      .associate { action ->
        action.id to getReleaseActionRevisions(action.id)
      }
    val allRevisions = planRevisionsById.values.flatten() + actionRevisionsById.values.flatten()

    return allRevisions
      .groupBy { it.metadata.revisionNumber.orElse(0) }
      .map { (revisionNumber, revisions) ->
        buildTimeline(revisionNumber!!, revisions, planRevisionsById, actionRevisionsById)
      }
      .sortedByDescending { it.occurredAt }
  }

  private fun buildTimeline(
    revisionNumber: Int,
    revisions: List<Revision<Int, *>>,
    planRevisionMap: Map<UUID, List<Revision<Int, ReleasePlanEntity>>>,
    actionRevisionMap: Map<UUID, List<Revision<Int, ReleaseActionEntity>>>,
  ): AuditTimeLine {
    val revInfo = revisions.first().metadata.getDelegate<RevInfo>()
    val sections = revisions
      .mapNotNull { revision ->
        revision.toAuditTimelineSection(revisionNumber, planRevisionMap, actionRevisionMap)
      }
      .toMutableList()

    return AuditTimeLine(
      revisionNumber = revisionNumber,
      occurredAt = revInfo.getRevisionInstant(),
      userName = revInfo.username,
      title = "Release plan updated",
      sections = sections,
    )
  }

  private fun Revision<Int, *>.toAuditTimelineSection(
    revisionNumber: Int,
    planRevisionMap: Map<UUID, List<Revision<Int, ReleasePlanEntity>>>,
    actionRevisionMap: Map<UUID, List<Revision<Int, ReleaseActionEntity>>>,
  ): AuditTimeLineSection? = when (val entity = entity) {
    is ReleasePlanEntity -> buildSection(
      title = "Release plan",
      diff = getDiff(planRevisionMap[entity.id].previousEntity(revisionNumber), entity),
    )
    is ReleaseActionEntity -> buildSection(
      title = "Release action",
      diff = getDiff(actionRevisionMap[entity.id].previousEntity(revisionNumber), entity),
    )
    else -> null
  }

  private fun buildSection(title: String, diff: MutableList<String>): AuditTimeLineSection? {
    if (diff.isEmpty() || diff.first() == "No changes") {
      return null
    }

    return AuditTimeLineSection(
      title = title,
      content = diff,
    )
  }

  private fun <T : Any> List<Revision<Int, T>>.findPrevious(currentRevisionNumber: Int): Revision<Int, T>? = this.filter { it.metadata.revisionNumber.get() < currentRevisionNumber }
    .maxByOrNull { it.metadata.revisionNumber.get() }

  private fun <T : Any> List<Revision<Int, T>>?.previousEntity(currentRevisionNumber: Int): T? = this
    ?.findPrevious(currentRevisionNumber)
    ?.entity

  private fun Any?.toReleaseActionRevision(): Revision<Int, ReleaseActionEntity> {
    val auditRow = this.toReleaseActionAuditRow()

    return Revision.of(
      auditRow.toRevisionMetadata(),
      auditRow.entity,
    )
  }

  private fun Any?.toReleaseActionAuditRow(): AuditRow<ReleaseActionEntity> {
    val auditResult = this as Array<*>

    return AuditRow(
      entity = auditResult[0] as ReleaseActionEntity,
      revInfo = auditResult[1] as RevInfo,
      revisionType = auditResult[2] as RevisionType,
    )
  }

  private fun AuditRow<*>.toRevisionMetadata(): AnnotationRevisionMetadata<Int> = AnnotationRevisionMetadata(
    revInfo,
    RevisionNumber::class.java,
    RevisionTimestamp::class.java,
    revisionType.toRevisionMetadataType(),
  )

  private data class AuditRow<T : Any>(
    val entity: T,
    val revInfo: RevInfo,
    val revisionType: RevisionType,
  )

  private fun RevisionType.toRevisionMetadataType(): RevisionMetadata.RevisionType = when (this) {
    RevisionType.ADD -> RevisionMetadata.RevisionType.INSERT
    RevisionType.MOD -> RevisionMetadata.RevisionType.UPDATE
    RevisionType.DEL -> RevisionMetadata.RevisionType.DELETE
  }

  private fun getDiff(old: Any?, new: Any): MutableList<String> {
    val changes = mutableListOf<String>()
    if (old == null) {
      when (new) {
        is ReleasePlanEntity -> changes.add("Created release plan with description '${new.description}'")
        is ReleaseActionEntity -> changes.add("Created release action with description '${new.description}'")
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

    if (changes.isEmpty()) {
      changes.add("No changes")
    }

    return changes
  }

  private fun MutableList<String>.addIfChanged(fieldName: String, oldValue: Any?, newValue: Any?) {
    if (oldValue != newValue) {
      add("$fieldName changed from '$oldValue' to '$newValue'")
    }
  }
}
