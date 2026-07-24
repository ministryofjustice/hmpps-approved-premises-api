package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.asHibernateProxy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.getHibernateClass
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

interface InboxEventRepository : JpaRepository<InboxEventEntity, UUID> {
  fun findAllByProcessedStatus(processedStatus: ProcessedStatus, pageable: Pageable): List<InboxEventEntity>

  @Query(
    value = """SELECT 
      count(*) as count,
      processed_status as processed_status 
      from inbox_events 
      group by processed_status 
      order by processed_status""",
    nativeQuery = true,
  )
  fun findCountByStatus(): List<ProcessedStatusCount>

  @Query(
    value = "update inbox_events set processed_status = 'PENDING', processed_at = NULL where processed_status = 'FAILED'",
    nativeQuery = true,
  )
  @Modifying
  fun setFailedAsPending(): Int

  @Modifying
  @Query(
    value = """
    DELETE FROM inbox_events
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '90 days'
  """,
    nativeQuery = true,
  )
  fun deleteInboxEventsOlderThan90Days(): Int

  interface ProcessedStatusCount {
    fun getCount(): Long
    fun getProcessedStatus(): ProcessedStatus
  }
}

@Entity
@Table(name = "inbox_events")
data class InboxEventEntity(
  @Id
  val id: UUID,
  val eventType: String,
  val eventDetailUrl: String?,
  val eventOccurredAt: OffsetDateTime,
  val createdAt: Instant,
  @Enumerated(EnumType.STRING)
  var processedStatus: ProcessedStatus,
  var processedAt: Instant?,
  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  var payload: String,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (this.getHibernateClass() != other.getHibernateClass()) return false
    other as InboxEventEntity

    return id == other.id
  }

  override fun hashCode(): Int = this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass?.hashCode() ?: javaClass.hashCode()

  override fun toString(): String = "InboxEventEntity(id=$id)"
}

fun InboxEventEntity.uri(): URI = URI.create(requireNotNull(eventDetailUrl) { "Missing detail url" })

enum class ProcessedStatus {
  PENDING,
  PROCESSED,
  IGNORED,
  FAILED,
  FAILED_REVIEWED,
  ;

  companion object {
    fun forValue(value: String): ProcessedStatus = entries.first { it.name.equals(value, ignoreCase = true) }
  }
}
