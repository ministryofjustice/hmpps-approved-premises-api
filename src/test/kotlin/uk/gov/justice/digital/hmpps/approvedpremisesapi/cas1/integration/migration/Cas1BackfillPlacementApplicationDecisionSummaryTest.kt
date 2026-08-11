package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.RequestForPlacementAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

class Cas1BackfillPlacementApplicationDecisionSummaryTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Test
  fun `backfill decision summary from the latest domain event, ignoring placement applications that don't require an update`() {
    val (user) = givenAUser()

    val placementApplicationWithDecisionAndNoSummary = givenAPlacementApplication(
      createdByUser = user,
      decision = PlacementApplicationDecision.ACCEPTED,
    )
    createRequestForPlacementAssessedEvent(
      placementApplication = placementApplicationWithDecisionAndNoSummary,
      decisionSummary = "the latest decision summary",
      createdAt = OffsetDateTime.now().minusDays(1),
    )
    createRequestForPlacementAssessedEvent(
      placementApplication = placementApplicationWithDecisionAndNoSummary,
      decisionSummary = "an older decision summary",
      createdAt = OffsetDateTime.now().minusDays(2),
    )

    val placementApplicationWithDecisionAndExistingSummary = givenAPlacementApplication(
      createdByUser = user,
      decision = PlacementApplicationDecision.REJECTED,
    ) {
      it.decisionSummary = "existing summary"
      placementApplicationRepository.save(it)
    }
    createRequestForPlacementAssessedEvent(
      placementApplication = placementApplicationWithDecisionAndExistingSummary,
      decisionSummary = "summary from event",
      createdAt = OffsetDateTime.now().minusDays(1),
    )

    val placementApplicationWithoutDecision = givenAPlacementApplication(
      createdByUser = user,
      decision = null,
    )
    createRequestForPlacementAssessedEvent(
      placementApplication = placementApplicationWithoutDecision,
      decisionSummary = "should be ignored",
      createdAt = OffsetDateTime.now().minusDays(1),
    )

    val placementApplicationWithNoEvent = givenAPlacementApplication(
      createdByUser = user,
      decision = PlacementApplicationDecision.ACCEPTED,
    )

    val placementApplicationWithDecisionAndEventWithoutSummary = givenAPlacementApplication(
      createdByUser = user,
      decision = PlacementApplicationDecision.ACCEPTED,
    )
    createRequestForPlacementAssessedEvent(
      placementApplication = placementApplicationWithDecisionAndEventWithoutSummary,
      decisionSummary = null,
      createdAt = OffsetDateTime.now().minusDays(1),
    )

    migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillPlacementApplicationDecisionSummary)

    assertThat(
      placementApplicationRepository.findByIdOrNull(placementApplicationWithDecisionAndNoSummary.id)!!.decisionSummary,
    ).isEqualTo("the latest decision summary")

    assertThat(
      placementApplicationRepository.findByIdOrNull(placementApplicationWithDecisionAndExistingSummary.id)!!.decisionSummary,
    ).isEqualTo("existing summary")

    assertThat(
      placementApplicationRepository.findByIdOrNull(placementApplicationWithoutDecision.id)!!.decisionSummary,
    ).isNull()

    assertThat(
      placementApplicationRepository.findByIdOrNull(placementApplicationWithNoEvent.id)!!.decisionSummary,
    ).isNull()

    assertThat(
      placementApplicationRepository.findByIdOrNull(placementApplicationWithDecisionAndEventWithoutSummary.id)!!.decisionSummary,
    ).isNull()
  }

  private fun createRequestForPlacementAssessedEvent(
    placementApplication: PlacementApplicationEntity,
    decisionSummary: String?,
    createdAt: OffsetDateTime,
  ) {
    val id = UUID.randomUUID()
    domainEventFactory.produceAndPersist {
      withId(id)
      withType(DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED)
      withApplicationId(placementApplication.application.id)
      withCreatedAt(createdAt)
      withData(
        jsonMapper.writeValueAsString(
          RequestForPlacementAssessedEnvelope(
            id = id,
            timestamp = Instant.now(),
            eventType = EventType.requestForPlacementAssessed,
            eventDetails = RequestForPlacementAssessedFactory()
              .withApplicationId(placementApplication.application.id)
              .withPlacementApplicationId(placementApplication.id)
              .withDecisionSummary(decisionSummary)
              .produce(),
          ),
        ),
      )
    }
  }
}
