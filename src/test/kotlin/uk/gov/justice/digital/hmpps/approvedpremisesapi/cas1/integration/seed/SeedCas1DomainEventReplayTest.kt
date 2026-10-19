package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.Cas1DomainEventReplaySeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DomainEventEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedCas1DomainEventReplayTest : SeedTestBase() {

  @Test
  fun `Replays a domain event`() {
    val domainEvent = domainEventRepository.save(
      DomainEventEntityFactory()
        .withId(UUID.fromString("73afe4d9-73ff-4a8a-88b8-d7c8a2373877"))
        .withCrn("CRN123")
        .withNomsNumber("theNoms")
        .withData("{ }")
        .withApplicationId(UUID.randomUUID())
        .withType(DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN)
        .produce(),
    )

    seed(
      SeedFileType.approvedPremisesReplayDomainEvents,
      rowsToCsv(
        listOf(
          Cas1DomainEventReplaySeedCsvRow(
            domainEvent.id.toString(),
          ),
        ),
      ),
    )

    val message = snsDomainEventListener.blockForMessage(DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN)

    assertThat(message.detailUrl).isEqualTo("http://api/events/application-withdrawn/73afe4d9-73ff-4a8a-88b8-d7c8a2373877")
  }

  @Test
  fun `Replays a domain event with a CRN override`() {
    val domainEvent = domainEventRepository.save(
      DomainEventEntityFactory()
        .withId(UUID.fromString("73afe4d9-73ff-4a8a-88b8-d7c8a2373878"))
        .withCrn("CRN123")
        .withNomsNumber("theNoms")
        .withData("{ }")
        .withApplicationId(UUID.randomUUID())
        .withType(DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN)
        .produce(),
    )

    seed(
      SeedFileType.approvedPremisesReplayDomainEvents,
      rowsToCsv(
        listOf(
          Cas1DomainEventReplaySeedCsvRow(
            domainEvent.id.toString(),
            personReferenceCrnOverride = "OVERRIDE_CRN",
          ),
        ),
      ),
    )

    val message = snsDomainEventListener.blockForMessage(DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN)

    assertThat(message.personReference.identifiers.first { it.type == "CRN" }.value).isEqualTo("OVERRIDE_CRN")
    assertThat(message.personReference.identifiers.first { it.type == "NOMS" }.value).isEqualTo("theNoms")
  }

  private fun rowsToCsv(rows: List<Cas1DomainEventReplaySeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "domain_event_id",
        "person_reference_crn_override",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.domainEventId)
        .withQuotedField(it.personReferenceCrnOverride ?: "")
        .newRow()
    }

    return builder.build()
  }
}
