package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime
import java.util.UUID

class DomainEventEntityFactory : Factory<DomainEventEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationId: Yielded<UUID?> = { UUID.randomUUID() }
  private var assessmentId: Yielded<UUID?> = { null }
  private var bookingId: Yielded<UUID?> = { null }
  private var crn: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var type: Yielded<DomainEventType> = { DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED }
  private var occurredAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(7) }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(7) }
  private var data: Yielded<String> = { "{}" }
  private var service: Yielded<String> = { randomOf(listOf("CAS1", "CAS2", "CAS3")) }
  private var triggeredByUserId: Yielded<UUID?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withApplicationId(applicationId: UUID?) = apply {
    this.applicationId = { applicationId }
  }

  fun withAssessmentId(assessmentId: UUID?) = apply {
    this.assessmentId = { assessmentId }
  }

  fun withBookingId(bookingId: UUID?) = apply {
    this.bookingId = { bookingId }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withType(type: DomainEventType) = apply {
    this.type = { type }
  }

  fun withOccurredAt(occurredAt: OffsetDateTime) = apply {
    this.occurredAt = { occurredAt }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withData(data: String) = apply {
    this.data = { data }
  }

  fun withData(data: Any) = apply {
    this.data = { ObjectMapper().findAndRegisterModules().writeValueAsString(data) }
  }

  fun withService(service: ServiceName) = apply {
    this.service = {
      when (service) {
        ServiceName.approvedPremises -> "CAS1"
        ServiceName.cas2 -> "CAS2"
        ServiceName.temporaryAccommodation -> "CAS3"
      }
    }
  }

  override fun produce(): DomainEventEntity = DomainEventEntity(
    id = this.id(),
    applicationId = this.applicationId(),
    assessmentId = this.assessmentId(),
    bookingId = this.bookingId(),
    crn = this.crn(),
    type = this.type(),
    occurredAt = this.occurredAt(),
    createdAt = this.createdAt(),
    data = this.data(),
    service = this.service(),
    triggeredByUserId = this.triggeredByUserId(),
  )
}
