package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
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

  @Query(
    """
     SELECT 
        d.id as id,
        d.type as type, 
        d.occurredAt as occurredAt,
        d.applicationId as applicationId,
        d.assessmentId as assessmentId,
        d.bookingId as bookingId,
        b.premises.id as premisesId,
        a.id as appealId,
        u as triggeredByUser
      FROM DomainEventEntity d 
      LEFT OUTER JOIN BookingEntity b ON b.id = d.bookingId
      LEFT OUTER JOIN AppealEntity a ON a.application.id = d.applicationId 
      LEFT OUTER JOIN UserEntity u ON u.id = d.triggeredByUserId
      WHERE d.applicationId = :applicationId
    """,
  )
  fun findAllTimelineEventsByApplicationId(applicationId: UUID): List<DomainEventSummary>

  @Query(
    "SELECT * FROM domain_events domain_event " +
      "where date_part('month', domain_event.occurred_at) = :month " +
      "AND date_part('year', domain_event.occurred_at) = :year ",
    nativeQuery = true,
  )
  fun findAllCreatedInMonth(month: Int, year: Int): List<DomainEventEntity>
}

@Entity
@Table(name = "domain_events")
data class DomainEventEntity(
  @Id
  val id: UUID,
  val applicationId: UUID?,
  val assessmentId: UUID?,
  val bookingId: UUID?,
  val crn: String,
  @Enumerated(value = EnumType.STRING)
  val type: DomainEventType,
  val occurredAt: OffsetDateTime,
  val createdAt: OffsetDateTime,
  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  val data: String,
  val service: String,
  val triggeredByUserId: UUID?,
) {
  final inline fun <reified T> toDomainEvent(objectMapper: ObjectMapper): DomainEvent<T> {
    val data = when {
      T::class == ApplicationSubmittedEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED ->
        objectMapper.readValue(this.data, T::class.java)
      T::class == ApplicationAssessedEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED ->
        objectMapper.readValue(this.data, T::class.java)
      T::class == BookingMadeEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_BOOKING_MADE ->
        objectMapper.readValue(this.data, T::class.java)
      T::class == PersonArrivedEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED ->
        objectMapper.readValue(this.data, T::class.java)
      T::class == PersonNotArrivedEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED ->
        objectMapper.readValue(this.data, T::class.java)
      T::class == PersonDepartedEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED ->
        objectMapper.readValue(this.data, T::class.java)
      T::class == BookingNotMadeEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE ->
        objectMapper.readValue(this.data, T::class.java)
      T::class == BookingCancelledEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED ->
        objectMapper.readValue(this.data, T::class.java)
      T::class == BookingChangedEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED ->
        objectMapper.readValue(this.data, T::class.java)
      T::class == ApplicationWithdrawnEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN ->
        objectMapper.readValue(this.data, T::class.java)
      T::class == AssessmentAppealedEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED ->
        objectMapper.readValue(this.data, T::class.java)
      T::class == PlacementApplicationWithdrawnEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN ->
        objectMapper.readValue(this.data, T::class.java)
      T::class == PlacementApplicationAllocatedEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED ->
        objectMapper.readValue(this.data, T::class.java)
      T::class == MatchRequestWithdrawnEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN ->
        objectMapper.readValue(this.data, T::class.java)
      T::class == AssessmentAllocatedEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED ->
        objectMapper.readValue(this.data, T::class.java)
      T::class == RequestForPlacementCreatedEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED ->
        objectMapper.readValue(this.data, T::class.java)
      else -> throw RuntimeException("Unsupported DomainEventData type ${T::class.qualifiedName}/${this.type.name}")
    }

    checkNotNull(this.applicationId) { "application id should not be null" }

    return DomainEvent(
      id = this.id,
      applicationId = this.applicationId,
      crn = this.crn,
      occurredAt = this.occurredAt.toInstant(),
      data = data,
    )
  }
}

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
  APPROVED_PREMISES_ASSESSMENT_APPEALED,
  APPROVED_PREMISES_ASSESSMENT_ALLOCATED,
  APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN,
  APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED,
  APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN,
  APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED,
  CAS2_APPLICATION_SUBMITTED,
  CAS2_APPLICATION_STATUS_UPDATED,
  CAS3_BOOKING_CANCELLED,
  CAS3_BOOKING_CONFIRMED,
  CAS3_BOOKING_PROVISIONALLY_MADE,
  CAS3_PERSON_ARRIVED,
  CAS3_PERSON_ARRIVED_UPDATED,
  CAS3_PERSON_DEPARTED,
  CAS3_REFERRAL_SUBMITTED,
  CAS3_PERSON_DEPARTURE_UPDATED,
  CAS3_BOOKING_CANCELLED_UPDATED,
}
