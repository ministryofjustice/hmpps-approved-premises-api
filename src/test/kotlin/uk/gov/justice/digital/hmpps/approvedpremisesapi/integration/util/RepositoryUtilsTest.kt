package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.util

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApprovedPremisesTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.findAllByIdOrdered
import java.util.UUID

class RepositoryUtilsTest : IntegrationTestBase() {
  @Nested
  inner class FindAllByIdOrdered {
    @Test
    fun `Guarantees that results are returned in the same order as the supplied list of IDs`() {
      val repository = mockk<ApprovedPremisesTestRepository>()

      val id1 = UUID(1L, 1L)
      val id2 = UUID(2L, 2L)
      val id3 = UUID(3L, 3L)
      val id4 = UUID(4L, 4L)

      val expectedIds = listOf(
        id1,
        id2,
        id3,
        id4,
      )

      val premises1 = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withId(id1)
        .produce()

      val premises2 = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withId(id2)
        .produce()

      val premises3 = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withId(id3)
        .produce()

      val premises4 = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withId(id4)
        .produce()

      every { repository.findAllById(any()) } returns listOf(
        premises2,
        premises4,
        premises3,
        premises1,
      )

      val result = repository.findAllByIdOrdered(expectedIds)

      assertThat(result).containsExactly(premises1, premises2, premises3, premises4)

      val reversedResult = repository.findAllByIdOrdered(expectedIds.reversed())

      assertThat(reversedResult).containsExactly(premises4, premises3, premises2, premises1)
    }
  }
}
