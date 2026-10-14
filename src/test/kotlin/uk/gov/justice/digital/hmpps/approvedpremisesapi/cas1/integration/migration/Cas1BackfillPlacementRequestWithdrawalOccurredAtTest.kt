package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.MatchRequestWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import java.time.OffsetDateTime
import java.util.UUID

class Cas1BackfillPlacementRequestWithdrawalOccurredAtTest : IntegrationTestBase() {

  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Test
  fun `backfill withdrawal occurred at from the latest withdrawal domain event, ignoring placement requests that don't require an update`() {
    val (user) = givenAUser()

    val latestOccurredAt = OffsetDateTime.now().minusDays(1).withNano(0)
    val olderOccurredAt = latestOccurredAt.minusDays(1)

    val (placementRequestWithNoWithdrawalDate, _) = givenAPlacementRequest(
      createdByUser = user,
      isWithdrawn = true,
    )

    createMatchRequestWithdrawnEvent(
      placementRequest = placementRequestWithNoWithdrawalDate,
      occurredAt = olderOccurredAt,
    )

    createMatchRequestWithdrawnEvent(
      placementRequest = placementRequestWithNoWithdrawalDate,
      occurredAt = latestOccurredAt,
    )

    val existingWithdrawalDate = latestOccurredAt.minusDays(9)

    val (placementRequestWithExistingWithdrawalDate, _) = givenAPlacementRequest(
      createdByUser = user,
      isWithdrawn = true,
    ).let { (placementRequest, application) ->
      placementRequest.withdrawalOccurredAt = existingWithdrawalDate
      placementRequestRepository.save(placementRequest)
      placementRequest to application
    }

    createMatchRequestWithdrawnEvent(
      placementRequest = placementRequestWithExistingWithdrawalDate,
      occurredAt = latestOccurredAt,
    )

    val (placementRequestNotWithdrawn, _) = givenAPlacementRequest(
      createdByUser = user,
    )

    createMatchRequestWithdrawnEvent(
      placementRequest = placementRequestNotWithdrawn,
      occurredAt = latestOccurredAt,
    )

    val (withdrawnPlacementRequestWithoutEvent, _) = givenAPlacementRequest(
      createdByUser = user,
      isWithdrawn = true,
    )

    migrationJobService.runMigrationJob(
      MigrationJobType.cas1BackfillPlacementRequestWithdrawalOccurredAt,
    )

    assertThat(
      placementRequestRepository.findByIdOrNull(
        placementRequestWithNoWithdrawalDate.id,
      )!!.withdrawalOccurredAt,
    ).isEqualTo(latestOccurredAt)

    assertThat(
      placementRequestRepository.findByIdOrNull(
        placementRequestWithExistingWithdrawalDate.id,
      )!!.withdrawalOccurredAt,
    ).isEqualTo(existingWithdrawalDate)

    assertThat(
      placementRequestRepository.findByIdOrNull(
        placementRequestNotWithdrawn.id,
      )!!.withdrawalOccurredAt,
    ).isNull()

    assertThat(
      placementRequestRepository.findByIdOrNull(
        withdrawnPlacementRequestWithoutEvent.id,
      )!!.withdrawalOccurredAt,
    ).isNull()
  }

  private fun createMatchRequestWithdrawnEvent(
    placementRequest: PlacementRequestEntity,
    occurredAt: OffsetDateTime,
  ) {
    val id = UUID.randomUUID()

    domainEventFactory.produceAndPersist {
      withId(id)
      withType(
        DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN,
      )
      withApplicationId(placementRequest.application.id)
      withOccurredAt(occurredAt)
      withCreatedAt(occurredAt)

      withData(
        jsonMapper.writeValueAsString(
          MatchRequestWithdrawnEnvelope(
            id = id,
            timestamp = occurredAt.toInstant(),
            eventType = EventType.matchRequestWithdrawn,
            eventDetails = MatchRequestWithdrawnFactory()
              .withApplicationId(placementRequest.application.id)
              .withMatchRequestId(placementRequest.id)
              .withWithdrawnAt(occurredAt.toInstant())
              .produce(),
          ),
        ),
      )
    }
  }
}
