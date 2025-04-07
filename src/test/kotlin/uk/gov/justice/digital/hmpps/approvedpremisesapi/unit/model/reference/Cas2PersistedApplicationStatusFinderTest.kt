package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.model.reference

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2ApplicationStatusSeeding
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

  @Nested
  inner class GetById {
    @Test
    fun `returns the matching status regardless of _isActive_ flag`() {
      val finder = Cas2PersistedApplicationStatusFinder(statusList())

      Assertions.assertThat(
        finder.getById(UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905")).name,
      ).isEqualTo(
        "moreInfoRequested",
      )

      Assertions.assertThat(
        finder.getById(UUID.fromString("ba4d8432-250b-4ab9-81ec-7eb4b16e5dd1")).name,
      ).isEqualTo(
        "awaitingDecision",
      )
    }

    @Test
    fun `throws an exception if the matching status is not found`() {
      val finder = Cas2PersistedApplicationStatusFinder(statusList())

      val exception = assertThrows<RuntimeException> {
        finder.getById(UUID.fromString("9887f81e-1a81-49b8-b0a6-5a17b3c9d7d1"))
      }

      Assertions.assertThat(exception.message).isEqualTo(
        "Status with id 9887f81e-1a81-49b8-b0a6-5a17b3c9d7d1 not found",
      )
    }
  }

  @Nested
  inner class ServiceDifferentiation {
    @Test
    fun `Requests for CAS2 service returns only CAS2 detail updates`() {
      val statusList = Cas2ApplicationStatusSeeding.statusList(ServiceName.cas2)
      val finder = Cas2PersistedApplicationStatusFinder(statusList)

      val statuses = finder.all()

      assertEquals(11, statuses.size, "Should return exactly 11 status updates")

      val expectedDetailsSizes = mapOf(
        0 to 9,
        7 to 7,
        8 to 7,
        9 to 10,
      )

      statuses.forEachIndexed { index, status ->
        if (index in expectedDetailsSizes.keys) {
          assertNotNull(status.statusDetails, "Status at index $index should have details")
          assertEquals(
            expectedDetailsSizes[index],
            status.statusDetails?.size,
            "Unexpected details size at index $index",
          )
        } else {
          assertNull(
            status.statusDetails,
            "Status at index $index should not have details",
          )
        }
      }

      // Verify all details are applicable to CAS2 service
      statuses
        .flatMap { it.statusDetails.orEmpty() }
        .forEach { detail ->
          assertTrue(
            detail.applicableToServices.contains(ServiceName.cas2),
            "All details should be applicable to CAS2 service",
          )
        }
    }

    @Test
    fun `Requests for CAS2v2 service returns only CAS2v2 detail updates`() {
      val statusList = Cas2ApplicationStatusSeeding.statusList(ServiceName.cas2v2)
      val finder = Cas2PersistedApplicationStatusFinder(statusList)

      val statuses = finder.all()

      assertEquals(11, statuses.size, "Should return exactly 11 status updates")

      val expectedDetailsSizes = mapOf(
        0 to 10,
        7 to 9,
        8 to 10,
        9 to 9,
      )

      statuses.forEachIndexed { index, status ->
        if (index in expectedDetailsSizes.keys) {
          assertNotNull(status.statusDetails, "Status at index $index should have details")
          assertEquals(
            expectedDetailsSizes[index],
            status.statusDetails?.size,
            "Unexpected details size at index $index",
          )
        } else {
          assertNull(
            status.statusDetails,
            "Status at index $index should not have details",
          )
        }
      }

      // Verify all details are applicable to CAS2 service
      statuses
        .flatMap { it.statusDetails.orEmpty() }
        .forEach { detail ->
          assertTrue(
            detail.applicableToServices.contains(ServiceName.cas2v2),
            "All details should be applicable to CAS2 service",
          )
        }
    }
  }

  private fun statusList(): List<Cas2PersistedApplicationStatus> = listOf(
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
