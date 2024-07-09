package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.MapKeyColumn
import javax.persistence.MapKeyEnumerated
import javax.persistence.Table
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType as Cas2EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.EventType as Cas3EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.EventType as Cas1EventType

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
        d.triggerSource as triggerSource,
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

  fun findByApplicationId(applicationId: UUID): List<DomainEventEntity>

  fun findByType(type: DomainEventType): List<DomainEventEntity>

  fun getByApplicationIdAndType(applicationId: UUID, type: DomainEventType): DomainEventEntity

  @Modifying
  @Query(
    """
    UPDATE domain_events 
    SET
        data = CAST(:updatedData as jsonb)
    WHERE id = :id
    """,
    nativeQuery = true,
  )
  fun updateData(id: UUID, updatedData: String)
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
  @Enumerated(value = EnumType.STRING)
  val triggerSource: TriggerSourceType? = null,
  val triggeredByUserId: UUID?,
  val nomsNumber: String?,
  @ElementCollection
  @MapKeyColumn(name = "name")
  @MapKeyEnumerated(EnumType.STRING)
  @Column(name = "value")
  @CollectionTable(name = "domain_events_metadata", joinColumns = [ JoinColumn(name = "domain_event_id") ])
  val metadata: Map<MetaDataName, String?> = emptyMap(),
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
      T::class == RequestForPlacementAssessedEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED ->
        objectMapper.readValue(this.data, T::class.java)
      T::class == FurtherInformationRequestedEnvelope::class && this.type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED ->
        objectMapper.readValue(this.data, T::class.java)
      else -> throw RuntimeException("Unsupported DomainEventData type ${T::class.qualifiedName}/${this.type.name}")
    }

    checkNotNull(this.applicationId) { "application id should not be null" }

    return DomainEvent(
      id = this.id,
      applicationId = this.applicationId,
      crn = this.crn,
      nomsNumber = this.nomsNumber,
      occurredAt = this.occurredAt.toInstant(),
      data = data,
    )
  }
}
enum class TriggerSourceType { USER, SYSTEM }

enum class MetaDataName {
  CAS1_APP_REASON_FOR_SHORT_NOTICE,
  CAS1_APP_REASON_FOR_SHORT_NOTICE_OTHER,
  CAS1_PLACEMENT_APPLICATION_ID,
  CAS1_REQUESTED_AP_TYPE,
  CAS1_PLACEMENT_REQUEST_ID,
  CAS1_CANCELLATION_ID,
}

enum class DomainEventType(val typeName: String, val typeDescription: String, val timelineEventType: TimelineEventType?) {
  APPROVED_PREMISES_APPLICATION_SUBMITTED(
    Cas1EventType.applicationSubmitted.value,
    "An application has been submitted for an Approved Premises placement",
    TimelineEventType.approvedPremisesApplicationSubmitted,
  ),
  APPROVED_PREMISES_APPLICATION_ASSESSED(
    Cas1EventType.applicationAssessed.value,
    "An application has been assessed for an Approved Premises placement",
    TimelineEventType.approvedPremisesApplicationAssessed,
  ),
  APPROVED_PREMISES_BOOKING_MADE(
    Cas1EventType.bookingMade.value,
    "An Approved Premises booking has been made",
    TimelineEventType.approvedPremisesBookingMade,
  ),
  APPROVED_PREMISES_PERSON_ARRIVED(
    Cas1EventType.personArrived.value,
    "Someone has arrived at an Approved Premises for their Booking",
    TimelineEventType.approvedPremisesPersonArrived,
  ),
  APPROVED_PREMISES_PERSON_NOT_ARRIVED(
    Cas1EventType.personNotArrived.value,
    "Someone has failed to arrive at an Approved Premises for their Booking",
    TimelineEventType.approvedPremisesPersonNotArrived,
  ),
  APPROVED_PREMISES_PERSON_DEPARTED(
    Cas1EventType.personDeparted.value,
    "Someone has left an Approved Premises",
    TimelineEventType.approvedPremisesPersonDeparted,
  ),
  APPROVED_PREMISES_BOOKING_NOT_MADE(
    Cas1EventType.bookingNotMade.value,
    "It was not possible to create a Booking on this attempt",
    TimelineEventType.approvedPremisesBookingNotMade,
  ),
  APPROVED_PREMISES_BOOKING_CANCELLED(
    Cas1EventType.bookingCancelled.value,
    "An Approved Premises Booking has been cancelled",
    TimelineEventType.approvedPremisesBookingCancelled,
  ),
  APPROVED_PREMISES_BOOKING_CHANGED(
    Cas1EventType.bookingChanged.value,
    "An Approved Premises Booking has been changed",
    TimelineEventType.approvedPremisesBookingChanged,
  ),
  APPROVED_PREMISES_APPLICATION_WITHDRAWN(
    Cas1EventType.applicationWithdrawn.value,
    "An Approved Premises Application has been withdrawn",
    TimelineEventType.approvedPremisesApplicationWithdrawn,
  ),
  APPROVED_PREMISES_ASSESSMENT_APPEALED(
    Cas1EventType.assessmentAppealed.value,
    "An Approved Premises Assessment has been appealed",
    TimelineEventType.approvedPremisesAssessmentAppealed,
  ),
  APPROVED_PREMISES_ASSESSMENT_ALLOCATED(
    Cas1EventType.assessmentAllocated.value,
    "An Approved Premises Assessment has been allocated",
    TimelineEventType.approvedPremisesAssessmentAllocated,
  ),
  APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED(
    Cas1EventType.informationRequestMade.value,
    "An information request has been made for an Approved Premises Assessment",
    TimelineEventType.approvedPremisesInformationRequest,
  ),
  APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN(
    Cas1EventType.placementApplicationWithdrawn.value,
    "An Approved Premises Request for Placement has been withdrawn",
    TimelineEventType.approvedPremisesPlacementApplicationWithdrawn,
  ),
  APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED(
    Cas1EventType.placementApplicationAllocated.value,
    "An Approved Premises Request for Placement has been allocated",
    TimelineEventType.approvedPremisesPlacementApplicationAllocated,
  ),
  APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN(
    Cas1EventType.matchRequestWithdrawn.value,
    "An Approved Premises Match Request has been withdrawn",
    TimelineEventType.approvedPremisesMatchRequestWithdrawn,
  ),
  APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED(
    Cas1EventType.requestForPlacementCreated.value,
    "An Approved Premises Request for Placement has been created",
    TimelineEventType.approvedPremisesRequestForPlacementCreated,
  ),
  APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED(
    Cas1EventType.requestForPlacementAssessed.value,
    "An request for placement has been assessed",
    TimelineEventType.approvedPremisesRequestForPlacementAssessed,
  ),
  CAS2_APPLICATION_SUBMITTED(
    Cas2EventType.applicationSubmitted.value,
    "An application has been submitted for a CAS2 placement",
    null,
  ),
  CAS2_APPLICATION_STATUS_UPDATED(
    Cas2EventType.applicationStatusUpdated.value,
    "An assessor has updated the status of a CAS2 application",
    null,
  ),
  CAS3_BOOKING_CANCELLED(
    Cas3EventType.bookingCancelled.value,
    "A booking for a Transitional Accommodation premises has been cancelled",
    null,
  ),
  CAS3_BOOKING_CONFIRMED(
    Cas3EventType.bookingConfirmed.value,
    "A booking has been confirmed for a Transitional Accommodation premises",
    null,
  ),
  CAS3_BOOKING_PROVISIONALLY_MADE(
    Cas3EventType.bookingProvisionallyMade.value,
    "A booking has been provisionally made for a Transitional Accommodation premises",
    null,
  ),
  CAS3_PERSON_ARRIVED(
    Cas3EventType.personArrived.value,
    "Someone has arrived at a Transitional Accommodation premises for their booking",
    null,
  ),
  CAS3_PERSON_ARRIVED_UPDATED(
    Cas3EventType.personArrivedUpdated.value,
    "Someone has changed arrival date at a Transitional Accommodation premises for their booking",
    null,
  ),
  CAS3_PERSON_DEPARTED(
    Cas3EventType.personDeparted.value,
    "Someone has left a Transitional Accommodation premises",
    null,
  ),
  CAS3_REFERRAL_SUBMITTED(
    Cas3EventType.referralSubmitted.value,
    "A referral for Transitional Accommodation has been submitted",
    null,
  ),
  CAS3_PERSON_DEPARTURE_UPDATED(
    Cas3EventType.personDepartureUpdated.value,
    "Person has updated departure date of Transitional Accommodation premises",
    null,
  ),
  CAS3_BOOKING_CANCELLED_UPDATED(
    Cas3EventType.bookingCancelledUpdated.value,
    "A cancelled booking for a Transitional Accommodation premises has been updated",
    null,
  ),
}
