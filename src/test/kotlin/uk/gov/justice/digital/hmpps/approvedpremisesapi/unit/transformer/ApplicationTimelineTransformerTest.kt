package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventAssociatedUrl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventUrlType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.domainevents.DomainEventDescriber
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationTimelineTransformerTest {
  private val mockDomainEventDescriber = mockk<DomainEventDescriber>()
  private val mockUserTransformer = mockk<UserTransformer>()

  private val applicationTimelineTransformer = ApplicationTimelineTransformer(
    UrlTemplate("http://somehost:3000/applications/#id"),
    UrlTemplate("http://somehost:3000/assessments/#id"),
    UrlTemplate("http://somehost:3000/premises/#premisesId/bookings/#bookingId"),
    UrlTemplate("http://somehost:3000/applications/#applicationId/appeals/#appealId"),
    mockDomainEventDescriber,
    mockUserTransformer,
  )

  data class DomainEventSummaryImpl(
    override val id: String,
    override val type: DomainEventType,
    override val occurredAt: OffsetDateTime,
    override val applicationId: UUID?,
    override val assessmentId: UUID?,
    override val bookingId: UUID?,
    override val premisesId: UUID?,
    override val appealId: UUID?,
    override val triggeredByUser: UserEntity?,
  ) : DomainEventSummary

  @ParameterizedTest
  @EnumSource(DomainEventType::class, names = ["APPROVED_PREMISES_.+"], mode = EnumSource.Mode.MATCH_ANY)
  fun `transformDomainEventSummaryToTimelineEvent transforms domain event correctly`(domainEventType: DomainEventType) {
    val userJpa = UserEntityFactory().withDefaultProbationRegion().produce()

    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = domainEventType,
      occurredAt = OffsetDateTime.now(),
      bookingId = null,
      applicationId = null,
      assessmentId = null,
      premisesId = null,
      appealId = null,
      triggeredByUser = userJpa,
    )

    val userApi = mockk<ApprovedPremisesUser>()
    every { mockUserTransformer.transformJpaToApi(userJpa, ServiceName.approvedPremises) } returns userApi
    every { mockDomainEventDescriber.getDescription(domainEvent) } returns "Some event"

    val result = applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)

    assertThat(result.id).isEqualTo(domainEvent.id)
    assertThat(result.type).isEqualTo(domainEventType.timelineEventType)
    assertThat(result.occurredAt).isEqualTo(domainEvent.occurredAt.toInstant())
    assertThat(result.associatedUrls).isEmpty()
    assertThat(result.content).isEqualTo("Some event")
    assertThat(result.createdBy).isEqualTo(userApi)
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent throws error if given CAS2 domain event type`() {
    val userJpa = UserEntityFactory().withDefaultProbationRegion().produce()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.CAS2_APPLICATION_SUBMITTED,
      occurredAt = OffsetDateTime.now(),
      bookingId = null,
      applicationId = null,
      assessmentId = null,
      premisesId = null,
      appealId = null,
      triggeredByUser = userJpa,
    )

    val exception = assertThrows<RuntimeException> {
      applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)
    }
    Assertions.assertThat(exception.message).isEqualTo("Cannot map CAS2_APPLICATION_SUBMITTED, only CAS1 is currently supported")
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent adds applicationUrl if application id defined`() {
    val applicationId = UUID.randomUUID()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
      occurredAt = OffsetDateTime.now(),
      bookingId = null,
      applicationId = applicationId,
      assessmentId = null,
      premisesId = null,
      appealId = null,
      triggeredByUser = null,
    )

    every { mockDomainEventDescriber.getDescription(domainEvent) } returns "Some event"

    Assertions.assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      TimelineEvent(
        id = domainEvent.id,
        type = TimelineEventType.approvedPremisesApplicationSubmitted,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = listOf(
          TimelineEventAssociatedUrl(TimelineEventUrlType.application, "http://somehost:3000/applications/$applicationId"),
        ),
        content = "Some event",
      ),
    )
  }

  @ParameterizedTest
  @EnumSource(DomainEventType::class, names = ["APPROVED_PREMISES_.+"], mode = EnumSource.Mode.MATCH_ANY)
  fun `transformDomainEventSummaryToTimelineEvent adds appealUrl for appeal events`(domainEventType: DomainEventType) {
    val applicationId = UUID.randomUUID()
    val appealId = UUID.randomUUID()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = domainEventType,
      occurredAt = OffsetDateTime.now(),
      bookingId = null,
      applicationId = applicationId,
      assessmentId = null,
      premisesId = null,
      appealId = appealId,
      triggeredByUser = null,
    )

    every { mockDomainEventDescriber.getDescription(domainEvent) } returns "Some event"

    Assertions.assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      TimelineEvent(
        id = domainEvent.id,
        type = domainEventType.timelineEventType,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = if (domainEventType == DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED) {
          listOf(
            TimelineEventAssociatedUrl(TimelineEventUrlType.assessmentAppeal, "http://somehost:3000/applications/$applicationId/appeals/$appealId"),
          )
        } else {
          listOf(
            TimelineEventAssociatedUrl(TimelineEventUrlType.application, "http://somehost:3000/applications/$applicationId"),
          )
        },
        content = "Some event",
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
      occurredAt = OffsetDateTime.now(),
      bookingId = bookingId,
      applicationId = null,
      assessmentId = null,
      premisesId = premisesId,
      appealId = null,
      triggeredByUser = null,
    )

    every { mockDomainEventDescriber.getDescription(domainEvent) } returns "Some event"

    assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      TimelineEvent(
        id = domainEvent.id,
        type = TimelineEventType.approvedPremisesBookingMade,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = listOf(
          TimelineEventAssociatedUrl(TimelineEventUrlType.booking, "http://somehost:3000/premises/$premisesId/bookings/$bookingId"),
        ),
        content = "Some event",
      ),
    )
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent adds assessmentUrl if assessment id defined`() {
    val assessmentId = UUID.randomUUID()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED,
      occurredAt = OffsetDateTime.now(),
      bookingId = null,
      applicationId = null,
      assessmentId = assessmentId,
      premisesId = null,
      appealId = null,
      triggeredByUser = null,
    )

    every { mockDomainEventDescriber.getDescription(domainEvent) } returns "Some event"

    Assertions.assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      TimelineEvent(
        id = domainEvent.id,
        type = TimelineEventType.approvedPremisesApplicationAssessed,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = listOf(
          TimelineEventAssociatedUrl(TimelineEventUrlType.assessment, "http://somehost:3000/assessments/$assessmentId"),
        ),
        content = "Some event",
      ),
    )
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent does not include assessment URL for info requests`() {
    val assessmentId = UUID.randomUUID()
    val applicationId = UUID.randomUUID()

    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED,
      occurredAt = OffsetDateTime.now(),
      bookingId = null,
      applicationId = applicationId,
      assessmentId = assessmentId,
      premisesId = null,
      appealId = null,
      triggeredByUser = null,
    )

    every { mockDomainEventDescriber.getDescription(domainEvent) } returns "Some event"

    Assertions.assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      TimelineEvent(
        id = domainEvent.id,
        type = TimelineEventType.approvedPremisesInformationRequest,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = listOf(
          TimelineEventAssociatedUrl(TimelineEventUrlType.application, "http://somehost:3000/applications/$applicationId"),
        ),
        content = "Some event",
      ),
    )
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent adds all possible url types`() {
    val applicationId = UUID.randomUUID()
    val assessmentId = UUID.randomUUID()
    val bookingId = UUID.randomUUID()
    val premisesId = UUID.randomUUID()
    val appealId = UUID.randomUUID()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
      occurredAt = OffsetDateTime.now(),
      bookingId = bookingId,
      applicationId = applicationId,
      assessmentId = assessmentId,
      premisesId = premisesId,
      appealId = appealId,
      triggeredByUser = null,
    )

    every { mockDomainEventDescriber.getDescription(domainEvent) } returns "Some event"

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
        content = "Some event",
      ),
    )
  }
}
