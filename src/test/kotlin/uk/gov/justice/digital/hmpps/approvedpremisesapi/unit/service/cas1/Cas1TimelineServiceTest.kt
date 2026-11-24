package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.controller.Cas1TimelineService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService.Companion.FEATURE_FLAG_CAS1_EXCLUDE_ARCHIVED_TIMELINE_EVENTS
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ApplicationTimelineTransformer
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1TimelineServiceTest {
  @MockK
  lateinit var cas1ApplicationTimelineNoteService: Cas1ApplicationTimelineNoteService

  @MockK
  lateinit var applicationTimelineNoteTransformer: ApplicationTimelineNoteTransformer

  @MockK
  lateinit var domainEventService: Cas1DomainEventService

  @MockK
  lateinit var cas1ApplicationTimelineTransformer: Cas1ApplicationTimelineTransformer

  @MockK
  lateinit var featureFlagService: FeatureFlagService

  private lateinit var service: Cas1TimelineService

  private val archivedEvent = summaryOfType(DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN)
  private val activeEvent = summaryOfType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
  private val archivedTimelineEvent = mockk<Cas1TimelineEvent>()
  private val activeTimelineEvent = mockk<Cas1TimelineEvent>()

  @BeforeEach
  fun setup() {
    service = Cas1TimelineService(
      cas1ApplicationTimelineNoteService,
      applicationTimelineNoteTransformer,
      domainEventService,
      cas1ApplicationTimelineTransformer,
      featureFlagService,
    )

    every { cas1ApplicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(archivedEvent) } returns archivedTimelineEvent
    every { cas1ApplicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(activeEvent) } returns activeTimelineEvent
  }

  @Nested
  inner class GetSpaceBookingTimeline {
    private val bookingId = UUID.randomUUID()

    @Test
    fun `Excludes archived events when feature flag is enabled`() {
      every { featureFlagService.getBooleanFlag(FEATURE_FLAG_CAS1_EXCLUDE_ARCHIVED_TIMELINE_EVENTS) } returns true
      every { domainEventService.getAllDomainEventsForSpaceBooking(bookingId) } returns listOf(archivedEvent, activeEvent)

      val result = service.getSpaceBookingTimeline(bookingId)

      assertThat(result).containsExactly(activeTimelineEvent)
    }

    @Test
    fun `Includes archived events when feature flag is disabled`() {
      every { featureFlagService.getBooleanFlag(FEATURE_FLAG_CAS1_EXCLUDE_ARCHIVED_TIMELINE_EVENTS) } returns false
      every { domainEventService.getAllDomainEventsForSpaceBooking(bookingId) } returns listOf(archivedEvent, activeEvent)

      val result = service.getSpaceBookingTimeline(bookingId)

      assertThat(result).containsExactly(archivedTimelineEvent, activeTimelineEvent)
    }
  }

  @Nested
  inner class GetApplicationTimelineEvents {
    private val applicationId = UUID.randomUUID()

    @BeforeEach
    fun setupNotes() {
      every { cas1ApplicationTimelineNoteService.getApplicationTimelineNotesByApplicationId(applicationId) } returns emptyList()
    }

    @Test
    fun `Excludes archived events when feature flag is enabled`() {
      every { featureFlagService.getBooleanFlag(FEATURE_FLAG_CAS1_EXCLUDE_ARCHIVED_TIMELINE_EVENTS) } returns true
      every { domainEventService.getAllDomainEventsForApplication(applicationId) } returns listOf(archivedEvent, activeEvent)

      val result = service.getApplicationTimelineEvents(applicationId)

      assertThat(result).containsExactly(activeTimelineEvent)
    }

    @Test
    fun `Includes archived events when feature flag is disabled`() {
      every { featureFlagService.getBooleanFlag(FEATURE_FLAG_CAS1_EXCLUDE_ARCHIVED_TIMELINE_EVENTS) } returns false
      every { domainEventService.getAllDomainEventsForApplication(applicationId) } returns listOf(archivedEvent, activeEvent)

      val result = service.getApplicationTimelineEvents(applicationId)

      assertThat(result).containsExactly(archivedTimelineEvent, activeTimelineEvent)
    }
  }

  @Nested
  inner class ArchivedDomainEventTypes {
    @Test
    fun `Only match request withdrawn is archived`() {
      val archived = DomainEventType.entries.filter { it.cas1Info?.archived == true }

      assertThat(archived).containsExactly(DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN)
    }
  }

  private fun summaryOfType(type: DomainEventType): DomainEventSummary {
    val summary = mockk<DomainEventSummary>()
    every { summary.type } returns type
    return summary
  }
}
