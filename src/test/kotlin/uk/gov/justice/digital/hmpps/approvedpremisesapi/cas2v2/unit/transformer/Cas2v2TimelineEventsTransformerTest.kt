package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.unit.transformer

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2TimelineEventsTransformer
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2TimelineEventsTransformerTest {
  private val user = Cas2UserEntityFactory().produce()

  private val cas2v2ApplicationFactory = Cas2ApplicationEntityFactory().withCreatedByUser(user)

  private val submittedCas2v2ApplicationFactory = Cas2ApplicationEntityFactory()
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.now())

  private val timelineEventTransformer = Cas2v2TimelineEventsTransformer()

  @Nested
  inner class WhenThereAreTimelineEvents {
    @Test
    fun `transforms the timeline events from a submitted application`() {
      val statusCreatedAt = OffsetDateTime.now().minusDays(2)
      val statusUpdateEntity = Cas2StatusUpdateEntityFactory()
        .withCreatedAt(statusCreatedAt)
        .withLabel("status update")
        .withApplication(submittedCas2v2ApplicationFactory.produce())
        .withAssessor(
          Cas2UserEntityFactory().withName("Anne Assessor")
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
              statusUpdate = Cas2StatusUpdateEntityFactory().withApplication(submittedCas2v2ApplicationFactory.produce()).produce(),
            ),
            Cas2StatusUpdateDetailEntity(
              id = UUID.randomUUID(),
              statusDetailId = UUID.fromString("fc38f750-e9d2-4270-b542-d38286b9855c"),
              label = "second detail",
              statusUpdate = Cas2StatusUpdateEntityFactory().withApplication(submittedCas2v2ApplicationFactory.produce()).produce(),
            ),
          ),
        )
        .withCreatedAt(statusWithDetailCreatedAt)
        .withLabel("status update with details")
        .withApplication(submittedCas2v2ApplicationFactory.produce())
        .withAssessor(
          Cas2UserEntityFactory().withName("Anne Other Assessor")
            .withUserType(Cas2UserType.EXTERNAL)
            .produce(),
        )
        .produce()

      val nomisUser = Cas2UserEntityFactory().withName("Some Nomis User").produce()

      val noteCreatedAt = OffsetDateTime.now().minusDays(3)
      val note = Cas2ApplicationNoteEntity(
        id = UUID.randomUUID(),
        createdAt = noteCreatedAt,
        createdByUser = nomisUser,
        application = submittedCas2v2ApplicationFactory.produce(),
        body = "a comment",
        assessment = Cas2AssessmentEntityFactory().produce(),
      )

      val submittedAt = OffsetDateTime.now().minusDays(4)

      val jpaEntity = submittedCas2v2ApplicationFactory
        .withSubmittedAt(submittedAt)
        .withCreatedByUser(nomisUser)
        .withStatusUpdates(mutableListOf(statusUpdateEntity, statusUpdateWithDetailsEntity))
        .withNotes(mutableListOf(note))
        .produce()

      val transformation = timelineEventTransformer.transformApplicationToTimelineEvents(jpaEntity)

      Assertions.assertThat(transformation).isEqualTo(
        listOf(
          Cas2TimelineEvent(
            type = TimelineEventType.cas2StatusUpdate,
            occurredAt = statusWithDetailCreatedAt.toInstant(),
            label = statusUpdateWithDetailsEntity.label,
            createdByName = statusUpdateWithDetailsEntity.assessor?.name,
            body = "first detail, second detail",
          ),
          Cas2TimelineEvent(
            type = TimelineEventType.cas2StatusUpdate,
            occurredAt = statusCreatedAt.toInstant(),
            label = statusUpdateEntity.label,
            createdByName = statusUpdateEntity.assessor?.name,
            body = null,
          ),
          Cas2TimelineEvent(
            type = TimelineEventType.cas2Note,
            occurredAt = noteCreatedAt.toInstant(),
            label = "Note",
            createdByName = note.createdByUser.name,
            body = "a comment",
          ),
          Cas2TimelineEvent(
            type = TimelineEventType.cas2ApplicationSubmitted,
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
      val jpaEntity = cas2v2ApplicationFactory
        .produce()

      val transformation = timelineEventTransformer.transformApplicationToTimelineEvents(jpaEntity)

      Assertions.assertThat(transformation).isEqualTo(
        emptyList<Cas2TimelineEvent>(),
      )
    }
  }
}
