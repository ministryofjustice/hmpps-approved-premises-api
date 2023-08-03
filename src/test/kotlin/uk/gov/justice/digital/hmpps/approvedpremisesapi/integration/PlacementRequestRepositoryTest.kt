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
  inner class findNonWithdrawnNonReallocatedPlacementRequests {
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
    fun `findNonWithdrawnNonReallocatedPlacementRequests returns all results when no page is provided`() {
      val nonParoleResults = realPlacementRequestRepository.findNonWithdrawnNonReallocatedPlacementRequests(false, null, null)
      val paroleResults = realPlacementRequestRepository.findNonWithdrawnNonReallocatedPlacementRequests(true, null, null)

      assertThat(nonParoleResults.content.map { it.id }).isEqualTo(nonParolePlacementRequests.map { it.id })
      assertThat(paroleResults.content.map { it.id }).isEqualTo(parolePlacementRequests.map { it.id })
    }

    @Test
    fun `findNonWithdrawnNonReallocatedPlacementRequests returns paginated results when a page is provided`() {
      val pageable = PageRequest.of(1, 2, Sort.by("createdAt"))
      val nonParoleResults = realPlacementRequestRepository.findNonWithdrawnNonReallocatedPlacementRequests(false, null, pageable)
      val paroleResults = realPlacementRequestRepository.findNonWithdrawnNonReallocatedPlacementRequests(true, null, pageable)

      assertThat(nonParoleResults.content.map { it.id }).isEqualTo(listOf(nonParolePlacementRequests[2], nonParolePlacementRequests[3]).map { it.id })
      assertThat(paroleResults.content.map { it.id }).isEqualTo(listOf(parolePlacementRequests[2], parolePlacementRequests[3]).map { it.id })
    }

    @Test
    fun `findNonWithdrawnNonReallocatedPlacementRequests returns only Placement Requests for CRN when specified`() {
      val crn = "CRN456"
      val requestsForCrn = createPlacementRequests(2, isWithdrawn = false, isReallocated = false, isParole = true, crn = crn)

      val pageable = PageRequest.of(0, 2, Sort.by("createdAt"))
      val results = realPlacementRequestRepository.findNonWithdrawnNonReallocatedPlacementRequests(true, crn, pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsForCrn.map { it.id })
    }

    @Test
    fun `findNonWithdrawnNonReallocatedPlacementRequests is case insensitive when searching for CRN`() {
      val crn = "CRN456"
      val requestsForCrn = createPlacementRequests(2, isWithdrawn = false, isReallocated = false, isParole = true, crn = crn)

      val pageable = PageRequest.of(0, 2, Sort.by("createdAt"))
      val results = realPlacementRequestRepository.findNonWithdrawnNonReallocatedPlacementRequests(true, "crN456", pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsForCrn.map { it.id })
    }

    private fun createPlacementRequests(count: Int, isWithdrawn: Boolean, isReallocated: Boolean, isParole: Boolean, crn: String? = null): List<PlacementRequestEntity> {
      val user = userEntityFactory.produceAndPersist {
        withProbationRegion(
          probationRegionEntityFactory.produceAndPersist {
            withApArea(apAreaEntityFactory.produceAndPersist())
          },
        )
      }

      return List(count) {
        if (crn == null) {
          `Given a Placement Request`(user, user, user, reallocated = isReallocated, isWithdrawn = isWithdrawn, isParole = isParole).first
        } else {
          `Given a Placement Request`(user, user, user, reallocated = isReallocated, isWithdrawn = isWithdrawn, isParole = isParole, crn = crn).first
        }
      }
    }
  }
}
