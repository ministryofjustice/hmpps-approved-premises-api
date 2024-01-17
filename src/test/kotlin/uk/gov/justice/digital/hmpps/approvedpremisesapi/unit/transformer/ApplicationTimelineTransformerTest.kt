package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineTransformer
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationTimelineTransformerTest {

  private val applicationTimelineTransformer = ApplicationTimelineTransformer()

  @ParameterizedTest
  @MethodSource("domainEventTypeArgs")
  fun `transformDomainEventSummaryToTimelineEvent transforms domain event correctly`(args: Pair<DomainEventType, TimelineEventType>) {
    val (domainEventType, timelineEventType) = args
    val domainEvent = DomainEventSummary(
      id = UUID.randomUUID().toString(),
      type = domainEventType,
      occurredAt = OffsetDateTime.now(),
    )
    Assertions.assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      TimelineEvent(
        id = domainEvent.id,
        type = timelineEventType,
        occurredAt = domainEvent.occurredAt.toInstant(),
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
