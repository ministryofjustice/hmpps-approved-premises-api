package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventAssociatedUrl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventUrlType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toTimestamp
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationTimelineTransformerTest {

  private val applicationTimelineTransformer = ApplicationTimelineTransformer(
    UrlTemplate("http://somehost:3000/applications/#id"),
    UrlTemplate("http://somehost:3000/assessments/#id"),
    UrlTemplate("http://somehost:3000/premises/#premisesId/bookings/#bookingId"),
  )

  data class DomainEventSummaryImpl(
    override val id: String,
    override val type: DomainEventType,
    override val occurredAt: Timestamp,
    override val applicationId: UUID?,
    override val assessmentId: UUID?,
    override val bookingId: UUID?,
    override val premisesId: UUID?,
  ) : DomainEventSummary

  @ParameterizedTest
  @MethodSource("domainEventTypeArgs")
  fun `transformDomainEventSummaryToTimelineEvent transforms domain event correctly`(args: Pair<DomainEventType, TimelineEventType>) {
    val (domainEventType, timelineEventType) = args
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = domainEventType,
      occurredAt = OffsetDateTime.now().toTimestamp(),
      bookingId = null,
      applicationId = null,
      assessmentId = null,
      premisesId = null,
    )
    Assertions.assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      TimelineEvent(
        id = domainEvent.id,
        type = timelineEventType,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = emptyList(),
      ),
    )
  }

  @Test
  fun `transformDomainEventTypeToTimelineEventType throws error if given CAS2 domain event type`() {
    val cas2DomainEventType = DomainEventType.CAS2_APPLICATION_SUBMITTED

    val exception = assertThrows<RuntimeException> {
      applicationTimelineTransformer.transformDomainEventTypeToTimelineEventType(cas2DomainEventType)
    }
    Assertions.assertThat(exception.message).isEqualTo("Only CAS1 is currently supported")
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent adds applicationUrl if application id defined`() {
    val applicationId = UUID.randomUUID()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
      occurredAt = OffsetDateTime.now().toTimestamp(),
      bookingId = null,
      applicationId = applicationId,
      assessmentId = null,
      premisesId = null,
    )

    Assertions.assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      TimelineEvent(
        id = domainEvent.id,
        type = TimelineEventType.approvedPremisesApplicationSubmitted,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = listOf(
          TimelineEventAssociatedUrl(TimelineEventUrlType.application, "http://somehost:3000/applications/$applicationId"),
        ),
      ),
    )
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent adds bookingUrl if booking id defined`() {
    val bookingId = UUID.randomUUID()
    val premisesId = UUID.randomUUID()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
      occurredAt = OffsetDateTime.now().toTimestamp(),
      bookingId = bookingId,
      applicationId = null,
      assessmentId = null,
      premisesId = premisesId,
    )

    Assertions.assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      TimelineEvent(
        id = domainEvent.id,
        type = TimelineEventType.approvedPremisesBookingMade,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = listOf(
          TimelineEventAssociatedUrl(TimelineEventUrlType.booking, "http://somehost:3000/premises/$premisesId/bookings/$bookingId"),
        ),
      ),
    )
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent adds assessmentUrl if assessment id defined`() {
    val assessmentId = UUID.randomUUID()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED,
      occurredAt = OffsetDateTime.now().toTimestamp(),
      bookingId = null,
      applicationId = null,
      assessmentId = assessmentId,
      premisesId = null,
    )

    Assertions.assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      TimelineEvent(
        id = domainEvent.id,
        type = TimelineEventType.approvedPremisesApplicationAssessed,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = listOf(
          TimelineEventAssociatedUrl(TimelineEventUrlType.assessment, "http://somehost:3000/assessments/$assessmentId"),
        ),
      ),
    )
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent adds all possible url types`() {
    val applicationId = UUID.randomUUID()
    val assessmentId = UUID.randomUUID()
    val bookingId = UUID.randomUUID()
    val premisesId = UUID.randomUUID()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
      occurredAt = OffsetDateTime.now().toTimestamp(),
      bookingId = bookingId,
      applicationId = applicationId,
      assessmentId = assessmentId,
      premisesId = premisesId,
    )

    Assertions.assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      TimelineEvent(
        id = domainEvent.id,
        type = TimelineEventType.approvedPremisesBookingMade,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = listOf(
          TimelineEventAssociatedUrl(TimelineEventUrlType.application, "http://somehost:3000/applications/$applicationId"),
          TimelineEventAssociatedUrl(TimelineEventUrlType.assessment, "http://somehost:3000/assessments/$assessmentId"),
          TimelineEventAssociatedUrl(TimelineEventUrlType.booking, "http://somehost:3000/premises/$premisesId/bookings/$bookingId"),
        ),
      ),
    )
  }

  private companion object {
    @JvmStatic
    fun domainEventTypeArgs() = listOf(
      DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED to TimelineEventType.approvedPremisesApplicationSubmitted,
      DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED to TimelineEventType.approvedPremisesApplicationAssessed,
      DomainEventType.APPROVED_PREMISES_BOOKING_MADE to TimelineEventType.approvedPremisesBookingMade,
      DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED to TimelineEventType.approvedPremisesPersonArrived,
      DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED to TimelineEventType.approvedPremisesPersonNotArrived,
      DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED to TimelineEventType.approvedPremisesPersonDeparted,
      DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE to TimelineEventType.approvedPremisesBookingNotMade,
      DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED to TimelineEventType.approvedPremisesBookingCancelled,
      DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED to TimelineEventType.approvedPremisesBookingChanged,
      DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN to TimelineEventType.approvedPremisesApplicationWithdrawn,
    )
  }
}
