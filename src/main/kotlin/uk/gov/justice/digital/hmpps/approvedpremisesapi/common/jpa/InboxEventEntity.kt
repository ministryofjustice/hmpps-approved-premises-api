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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.asHibernateProxy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.getHibernateClass
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

interface InboxEventRepository : JpaRepository<InboxEventEntity, UUID> {
  fun findAllByProcessedStatus(processedStatus: ProcessedStatus, pageable: Pageable): List<InboxEventEntity>
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
}
