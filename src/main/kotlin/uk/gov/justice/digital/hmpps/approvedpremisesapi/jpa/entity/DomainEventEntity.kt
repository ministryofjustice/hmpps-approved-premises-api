package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface DomainEventRepository : JpaRepository<DomainEventEntity, UUID> {

  @Query("SELECT new uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary(cast(d.id as string), d.type, d.occurredAt)  FROM DomainEventEntity d WHERE d.applicationId = :applicationId")
  fun findAllTimelineEventsByApplicationId(applicationId: UUID): List<DomainEventSummary>
}

@Entity
@Table(name = "domain_events")
data class DomainEventEntity(
  @Id
  val id: UUID,
  val applicationId: UUID,
  val crn: String,
  @Enumerated(value = EnumType.STRING)
  val type: DomainEventType,
  val occurredAt: OffsetDateTime,
  val createdAt: OffsetDateTime,
  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  val data: String,
)

enum class DomainEventType {
  APPROVED_PREMISES_APPLICATION_SUBMITTED,
  APPROVED_PREMISES_APPLICATION_ASSESSED,
  APPROVED_PREMISES_BOOKING_MADE,
  APPROVED_PREMISES_PERSON_ARRIVED,
  APPROVED_PREMISES_PERSON_NOT_ARRIVED,
  APPROVED_PREMISES_PERSON_DEPARTED,
  APPROVED_PREMISES_BOOKING_NOT_MADE,
  APPROVED_PREMISES_BOOKING_CANCELLED,
  APPROVED_PREMISES_BOOKING_CHANGED,
  APPROVED_PREMISES_APPLICATION_WITHDRAWN,
  CAS3_PERSON_ARRIVED,
  CAS3_PERSON_DEPARTED,
}
