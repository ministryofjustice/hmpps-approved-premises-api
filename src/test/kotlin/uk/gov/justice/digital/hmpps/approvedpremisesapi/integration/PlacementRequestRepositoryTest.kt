package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Request`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository

class PlacementRequestRepositoryTest : IntegrationTestBase() {
  @Autowired
  lateinit var realPlacementRequestRepository: PlacementRequestRepository

  @Nested
  inner class FindAllByIsParoleAndReallocatedAtNullAndIsWithdrawnFalse {
    private lateinit var nonParolePlacementRequests: List<PlacementRequestEntity>
    private lateinit var parolePlacementRequests: List<PlacementRequestEntity>

    @BeforeEach
    fun setup() {
      nonParolePlacementRequests = createPlacementRequests(4, isWithdrawn = false, isReallocated = false, isParole = false)
      parolePlacementRequests = createPlacementRequests(4, isWithdrawn = false, isReallocated = false, isParole = true)

      createPlacementRequests(1, isWithdrawn = false, isReallocated = true, isParole = true)
      createPlacementRequests(1, isWithdrawn = false, isReallocated = true, isParole = false)

      createPlacementRequests(1, isWithdrawn = true, isReallocated = false, isParole = true)
      createPlacementRequests(1, isWithdrawn = true, isReallocated = false, isParole = false)

      createPlacementRequests(1, isWithdrawn = true, isReallocated = true, isParole = true)
      createPlacementRequests(1, isWithdrawn = true, isReallocated = true, isParole = false)
    }

    @Test
    fun `findAllByIsParoleAndReallocatedAtNullAndIsWithdrawnFalse returns all results when no page is provided`() {
      val nonParoleResults = realPlacementRequestRepository.findAllByIsParoleAndReallocatedAtNullAndIsWithdrawnFalse(false, null)
      val paroleResults = realPlacementRequestRepository.findAllByIsParoleAndReallocatedAtNullAndIsWithdrawnFalse(true, null)

      assertThat(nonParoleResults.content.map { it.id }).isEqualTo(nonParolePlacementRequests.map { it.id })
      assertThat(paroleResults.content.map { it.id }).isEqualTo(parolePlacementRequests.map { it.id })
    }

    @Test
    fun `findAllByIsParoleAndReallocatedAtNullAndIsWithdrawnFalse returns paginated results when a page is provided`() {
      val pageable = PageRequest.of(1, 2, Sort.by("createdAt"))
      val nonParoleResults = realPlacementRequestRepository.findAllByIsParoleAndReallocatedAtNullAndIsWithdrawnFalse(false, pageable)
      val paroleResults = realPlacementRequestRepository.findAllByIsParoleAndReallocatedAtNullAndIsWithdrawnFalse(true, pageable)

      assertThat(nonParoleResults.content.map { it.id }).isEqualTo(listOf(nonParolePlacementRequests[2], nonParolePlacementRequests[3]).map { it.id })
      assertThat(paroleResults.content.map { it.id }).isEqualTo(listOf(parolePlacementRequests[2], parolePlacementRequests[3]).map { it.id })
    }

    private fun createPlacementRequests(count: Int, isWithdrawn: Boolean, isReallocated: Boolean, isParole: Boolean): List<PlacementRequestEntity> {
      val user = userEntityFactory.produceAndPersist {
        withProbationRegion(
          probationRegionEntityFactory.produceAndPersist {
            withApArea(apAreaEntityFactory.produceAndPersist())
          },
        )
      }

      return List(count) {
        `Given a Placement Request`(user, user, user, reallocated = isReallocated, isWithdrawn = isWithdrawn, isParole = isParole).first
      }
    }
  }
}
