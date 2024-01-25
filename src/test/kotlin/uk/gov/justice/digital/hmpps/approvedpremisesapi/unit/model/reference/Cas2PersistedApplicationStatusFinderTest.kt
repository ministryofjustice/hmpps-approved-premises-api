package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.model.reference

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusFinder
import java.util.UUID

class Cas2PersistedApplicationStatusFinderTest {

  @Nested
  inner class All {
    @Test
    fun `returns all statuses`() {
      val finder = Cas2PersistedApplicationStatusFinder(statusList())

      Assertions.assertThat(finder.all().map { it.name }).isEqualTo(
        listOf(
          "moreInfoRequested",
          "awaitingDecision",
          "placeOffered",
        ),
      )
    }
  }

  @Nested
  inner class Active {
    @Test
    fun `returns only the ACTIVE statuses`() {
      val finder = Cas2PersistedApplicationStatusFinder(statusList())

      Assertions.assertThat(finder.active().map { it.name }).isEqualTo(
        listOf(
          "moreInfoRequested",
          "placeOffered",
        ),
      )
    }
  }

  private fun statusList(): List<Cas2PersistedApplicationStatus> {
    return listOf(
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905"),
        name = "moreInfoRequested",
        label = "",
        description = "",
      ),
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("ba4d8432-250b-4ab9-81ec-7eb4b16e5dd1"),
        name = "awaitingDecision",
        label = "",
        description = "",
        isActive = false,
      ),
      Cas2PersistedApplicationStatus(
        id = UUID.fromString("176bbda0-0766-4d77-8d56-18ed8f9a4ef2"),
        name = "placeOffered",
        label = "",
        description = "",
      ),
    )
  }
}
