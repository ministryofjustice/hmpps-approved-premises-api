package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import java.time.OffsetDateTime
import java.util.UUID

class Cas1BackfillPlacementApplicationWithdrawalOccurredAtTest : IntegrationTestBase() {

  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Test
  fun `backfill withdrawal occurred at from the latest withdrawal domain event, ignoring placement applications that don't require an update`() {
    val (user) = givenAUser()

    val latestOccurredAt = OffsetDateTime.now().minusDays(1).withNano(0)
    val olderOccurredAt = latestOccurredAt.minusDays(1)

    val placementApplicationWithNoWithdrawalDate = givenAPlacementApplication(
      createdByUser = user,
    ) {
      it.isWithdrawn = true
      it.withdrawalOccurredAt = null
      placementApplicationRepository.save(it)
    }

    createPlacementApplicationWithdrawnEvent(
      placementApplication = placementApplicationWithNoWithdrawalDate,
      occurredAt = olderOccurredAt,
    )

    createPlacementApplicationWithdrawnEvent(
      placementApplication = placementApplicationWithNoWithdrawalDate,
      occurredAt = latestOccurredAt,
    )

    val existingWithdrawalDate = latestOccurredAt.minusDays(9)

    val placementApplicationWithExistingWithdrawalDate = givenAPlacementApplication(
      createdByUser = user,
    ) {
      it.isWithdrawn = true
      it.withdrawalOccurredAt = existingWithdrawalDate
      placementApplicationRepository.save(it)
    }

    createPlacementApplicationWithdrawnEvent(
      placementApplication = placementApplicationWithExistingWithdrawalDate,
      occurredAt = latestOccurredAt,
    )

    val placementApplicationNotWithdrawn = givenAPlacementApplication(
      createdByUser = user,
    )

    createPlacementApplicationWithdrawnEvent(
      placementApplication = placementApplicationNotWithdrawn,
      occurredAt = latestOccurredAt,
    )

    val withdrawnPlacementApplicationWithoutEvent = givenAPlacementApplication(
      createdByUser = user,
    ) {
      it.isWithdrawn = true
      placementApplicationRepository.save(it)
    }

    migrationJobService.runMigrationJob(
      MigrationJobType.cas1BackfillPlacementApplicationWithdrawalOccurredAt,
    )

    assertThat(
      placementApplicationRepository.findByIdOrNull(
        placementApplicationWithNoWithdrawalDate.id,
      )!!.withdrawalOccurredAt,
    ).isEqualTo(latestOccurredAt)

    assertThat(
      placementApplicationRepository.findByIdOrNull(
        placementApplicationWithExistingWithdrawalDate.id,
      )!!.withdrawalOccurredAt,
    ).isEqualTo(existingWithdrawalDate)

    assertThat(
      placementApplicationRepository.findByIdOrNull(
        placementApplicationNotWithdrawn.id,
      )!!.withdrawalOccurredAt,
    ).isNull()

    assertThat(
      placementApplicationRepository.findByIdOrNull(
        withdrawnPlacementApplicationWithoutEvent.id,
      )!!.withdrawalOccurredAt,
    ).isNull()
  }

  private fun createPlacementApplicationWithdrawnEvent(
    placementApplication: PlacementApplicationEntity,
    occurredAt: OffsetDateTime,
  ) {
    val id = UUID.randomUUID()

    domainEventFactory.produceAndPersist {
      withId(id)
      withType(
        DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN,
      )
      withApplicationId(placementApplication.application.id)
      withOccurredAt(occurredAt)
      withCreatedAt(occurredAt)

      withData(
        jsonMapper.writeValueAsString(
          PlacementApplicationWithdrawnEnvelope(
            id = id,
            timestamp = occurredAt.toInstant(),
            eventType = EventType.placementApplicationWithdrawn,
            eventDetails = PlacementApplicationWithdrawnFactory()
              .withApplicationId(placementApplication.application.id)
              .withPlacementApplicationId(placementApplication.id)
              .withWithdrawnAt(occurredAt.toInstant())
              .produce(),
          ),
        ),
      )
    }
  }
}
