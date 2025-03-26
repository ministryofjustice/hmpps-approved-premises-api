package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas2

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ExternalUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Agency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.TimelineEventsTransformer
import java.time.OffsetDateTime
import java.util.UUID

class TimelineEventsTransformerTest {

  private val user = NomisUserEntityFactory().produce()

  private val cas2ApplicationFactory = Cas2ApplicationEntityFactory().withCreatedByUser(user)

  private val submittedCas2ApplicationFactory = Cas2ApplicationEntityFactory()
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.now())

  private val mockPrisonsApiClient = mockk<PrisonsApiClient>()

  private val timelineEventTransformer = TimelineEventsTransformer(mockPrisonsApiClient)

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

      val pomUser = NomisUserEntityFactory().withName("Pom User").produce()
      val application = submittedCas2ApplicationFactory.produce()

      val firstApplicationAssignment = Cas2ApplicationAssignmentEntity(
        id = UUID.randomUUID(),
        createdAt = OffsetDateTime.now().minusDays(70),
        prisonCode = "FEI",
        allocatedPomUser = nomisUser,
        application = application,
      )

      val newPomAssignedApplicationAssignment = Cas2ApplicationAssignmentEntity(
        id = UUID.randomUUID(),
        createdAt = OffsetDateTime.now().minusDays(55),
        prisonCode = "FEI",
        allocatedPomUser = pomUser,
        application = application,
      )

      val prisonTransferApplicationAssignment = Cas2ApplicationAssignmentEntity(
        id = UUID.randomUUID(),
        createdAt = OffsetDateTime.now().minusDays(13),
        prisonCode = "MDI",
        allocatedPomUser = null,
        application = application,
      )

      val applicationAssignments = mutableListOf(
        firstApplicationAssignment,
        newPomAssignedApplicationAssignment,
        prisonTransferApplicationAssignment,
      )

      val submittedAt = OffsetDateTime.now().minusDays(4)

      val jpaEntity = submittedCas2ApplicationFactory
        .withSubmittedAt(submittedAt)
        .withCreatedByUser(nomisUser)
        .withStatusUpdates(mutableListOf(statusUpdateEntity, statusUpdateWithDetailsEntity))
        .withNotes(mutableListOf(note))
        .withApplicationAssignments(applicationAssignments)
        .produce()

      val prisonMdi = Agency(agencyId = "MDI", description = "Moorland (HMP & YOI)", agencyType = "prison")
      every { mockPrisonsApiClient.getAgencyDetails("MDI") } returns ClientResult.Success(
        HttpStatus.OK,
        prisonMdi,
      )

      val prisonFei = Agency(agencyId = "FEI", description = "Fosse Way (HMP)", agencyType = "prison")
      every { mockPrisonsApiClient.getAgencyDetails("FEI") } returns ClientResult.Success(
        HttpStatus.OK,
        prisonFei,
      )

      val transformation = timelineEventTransformer.transformApplicationToTimelineEvents(jpaEntity)

      Assertions.assertThat(transformation).isEqualTo(
        listOf(
          Cas2TimelineEvent(
            type = TimelineEventType.cas2StatusUpdate,
            occurredAt = statusWithDetailCreatedAt.toInstant(),
            label = statusUpdateWithDetailsEntity.label,
            createdByName = statusUpdateWithDetailsEntity.assessor.name,
            body = "first detail, second detail",
          ),
          Cas2TimelineEvent(
            type = TimelineEventType.cas2StatusUpdate,
            occurredAt = statusCreatedAt.toInstant(),
            label = statusUpdateEntity.label,
            createdByName = statusUpdateEntity.assessor.name,
            body = null,
          ),
          Cas2TimelineEvent(
            type = TimelineEventType.cas2Note,
            occurredAt = noteCreatedAt.toInstant(),
            label = "Note",
            createdByName = note.getUser().name,
            body = "a comment",
          ),
          Cas2TimelineEvent(
            type = TimelineEventType.cas2ApplicationSubmitted,
            occurredAt = submittedAt.toInstant(),
            label = "Application submitted",
            createdByName = "Some Nomis User",
          ),
          Cas2TimelineEvent(
            type = TimelineEventType.cas2PrisonTransfer,
            occurredAt = prisonTransferApplicationAssignment.createdAt.toInstant(),
            label = "Prison transfer from Fosse Way (HMP) to Moorland (HMP & YOI)",
            createdByName = null,
          ),
          Cas2TimelineEvent(
            type = TimelineEventType.cas2NewPomAssigned,
            occurredAt = newPomAssignedApplicationAssignment.createdAt.toInstant(),
            label = "New Prison offender manager Pom User allocated",
            createdByName = null,
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
