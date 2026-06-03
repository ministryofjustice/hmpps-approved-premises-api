package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.javers.core.ChangesByCommit
import org.javers.core.Javers
import org.javers.repository.jql.QueryBuilder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AuditRecordType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CreateFieldChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineRecord
import java.util.UUID

@Service
class JaversTimelineService(
  private val javers: Javers,
) {

  fun <T : Any> getFullAuditHistory(id: UUID, entityClass: Class<T>): List<TimelineRecord> {
    val query = QueryBuilder.byInstanceId(id, entityClass)
      .limit(200)
      .build()

    return javers.findChanges(query)
      .groupByCommit()
      .map { it.toTimelineRecord() }
  }

  private fun ChangesByCommit.toTimelineRecord(): TimelineRecord {
    val commit = getCommit()
    return TimelineRecord(
      type = AuditRecordType.UPDATE,
      author = commit.author,
      commitDate = commit.commitDateInstant,
      changes = get().map { it.toString() }.map { CreateFieldChange(field = "Change", value = it) },
      extraInformation = mapOf("commitId" to commit.id.toString()),
    )
  }
}
