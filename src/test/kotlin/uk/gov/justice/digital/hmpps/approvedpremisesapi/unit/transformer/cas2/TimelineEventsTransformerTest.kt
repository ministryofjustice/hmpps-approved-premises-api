package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas2

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ExternalUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.TimelineEventsTransformer
import java.time.OffsetDateTime
import java.util.UUID

class TimelineEventsTransformerTest {

  private val user = NomisUserEntityFactory().produce()

  private val cas2ApplicationFactory = Cas2ApplicationEntityFactory().withCreatedByUser(user)

  private val submittedCas2ApplicationFactory = Cas2ApplicationEntityFactory()
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.now())

  private val timelineEventTransformer = TimelineEventsTransformer()

  @Nested
  inner class WhenThereAreTimelineEvents {
    @Test
    fun `transforms the timeline events from a submitted application`() {
      val statusCreatedAt = OffsetDateTime.now().minusDays(2)
      val statusUpdateEntity = Cas2StatusUpdateEntityFactory()
        .withCreatedAt(statusCreatedAt)
        .withLabel("status update")
        .withApplication(submittedCas2ApplicationFactory.produce())
        .withAssessor(
          ExternalUserEntityFactory().withName("Anne Assessor")
            .produce(),
        ).produce()

      val statusWithDetailCreatedAt = OffsetDateTime.now().minusDays(1)
      val statusUpdateWithDetailsEntity = Cas2StatusUpdateEntityFactory()
        .withStatusUpdateDetails(
          listOf(
            Cas2StatusUpdateDetailEntity(
              id = UUID.randomUUID(),
              statusDetailId = UUID.fromString("fc38f750-e9d2-4270-b542-d38286b9855c"),
              label = "first detail",
              statusUpdate = Cas2StatusUpdateEntityFactory().withApplication(submittedCas2ApplicationFactory.produce()).produce(),
            ),
            Cas2StatusUpdateDetailEntity(
              id = UUID.randomUUID(),
              statusDetailId = UUID.fromString("fc38f750-e9d2-4270-b542-d38286b9855c"),
              label = "second detail",
              statusUpdate = Cas2StatusUpdateEntityFactory().withApplication(submittedCas2ApplicationFactory.produce()).produce(),
            ),
          ),
        )
        .withCreatedAt(statusWithDetailCreatedAt)
        .withLabel("status update with details")
        .withApplication(submittedCas2ApplicationFactory.produce())
        .withAssessor(
          ExternalUserEntityFactory().withName("Anne Other Assessor")
            .produce(),
        )
        .produce()

      val nomisUser = NomisUserEntityFactory().withName("Some Nomis User").produce()

      val noteCreatedAt = OffsetDateTime.now().minusDays(3)
      val note = Cas2ApplicationNoteEntity(
        id = UUID.randomUUID(),
        createdAt = noteCreatedAt,
        createdByUser = nomisUser,
        application = submittedCas2ApplicationFactory.produce(),
        body = "a comment",
        assessment = Cas2AssessmentEntityFactory().produce(),
      )

      val submittedAt = OffsetDateTime.now().minusDays(4)

      val jpaEntity = submittedCas2ApplicationFactory
        .withSubmittedAt(submittedAt)
        .withCreatedByUser(nomisUser)
        .withStatusUpdates(mutableListOf(statusUpdateEntity, statusUpdateWithDetailsEntity))
        .withNotes(mutableListOf(note))
        .produce()

      val transformation = timelineEventTransformer.transformApplicationToTimelineEvents(jpaEntity)

      Assertions.assertThat(transformation).isEqualTo(
        listOf(
          Cas2TimelineEvent(
            type = TimelineEventType.CAS2_STATUS_UPDATE,
            occurredAt = statusWithDetailCreatedAt.toInstant(),
            label = statusUpdateWithDetailsEntity.label,
            createdByName = statusUpdateWithDetailsEntity.assessor.name,
            body = "first detail, second detail",
          ),
          Cas2TimelineEvent(
            type = TimelineEventType.CAS2_STATUS_UPDATE,
            occurredAt = statusCreatedAt.toInstant(),
            label = statusUpdateEntity.label,
            createdByName = statusUpdateEntity.assessor.name,
            body = null,
          ),
          Cas2TimelineEvent(
            type = TimelineEventType.CAS2_NOTE,
            occurredAt = noteCreatedAt.toInstant(),
            label = "Note",
            createdByName = note.getUser().name,
            body = "a comment",
          ),
          Cas2TimelineEvent(
            type = TimelineEventType.CAS2_APPLICATION_SUBMITTED,
            occurredAt = submittedAt.toInstant(),
            label = "Application submitted",
            createdByName = "Some Nomis User",
          ),
        ),
      )
    }
  }

  @Nested
  inner class WhenThereAreNoTimelineEvents {
    @Test
    fun `returns an empty list on an in progress application`() {
      val jpaEntity = cas2ApplicationFactory
        .produce()

      val transformation = timelineEventTransformer.transformApplicationToTimelineEvents(jpaEntity)

      Assertions.assertThat(transformation).isEqualTo(
        emptyList<Cas2TimelineEvent>(),
      )
    }
  }
}
