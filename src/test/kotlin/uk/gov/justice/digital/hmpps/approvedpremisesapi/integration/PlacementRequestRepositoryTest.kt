package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import java.time.LocalDate

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
      val nonParoleResults = placementRequestTestRepository.findAllByIsParoleAndReallocatedAtNullAndIsWithdrawnFalse(false, null)
      val paroleResults = placementRequestTestRepository.findAllByIsParoleAndReallocatedAtNullAndIsWithdrawnFalse(true, null)

      assertThat(nonParoleResults.content.map { it.id }).isEqualTo(nonParolePlacementRequests.map { it.id })
      assertThat(paroleResults.content.map { it.id }).isEqualTo(parolePlacementRequests.map { it.id })
    }

    @Test
    fun `findAllByIsParoleAndReallocatedAtNullAndIsWithdrawnFalse returns paginated results when a page is provided`() {
      val pageable = PageRequest.of(1, 2, Sort.by("createdAt"))
      val nonParoleResults = placementRequestTestRepository.findAllByIsParoleAndReallocatedAtNullAndIsWithdrawnFalse(false, pageable)
      val paroleResults = placementRequestTestRepository.findAllByIsParoleAndReallocatedAtNullAndIsWithdrawnFalse(true, pageable)

      assertThat(nonParoleResults.content.map { it.id }).isEqualTo(listOf(nonParolePlacementRequests[2], nonParolePlacementRequests[3]).map { it.id })
      assertThat(paroleResults.content.map { it.id }).isEqualTo(listOf(parolePlacementRequests[2], parolePlacementRequests[3]).map { it.id })
    }
  }

  @Nested
  inner class AllForDashboard {
    private lateinit var placementRequestsWithBooking: List<PlacementRequestEntity>
    private lateinit var placementRequestsWithNoBooking: List<PlacementRequestEntity>
    private lateinit var placementRequestsWithACancelledBooking: List<PlacementRequestEntity>
    private lateinit var placementRequestsWithABookingNotMade: List<PlacementRequestEntity>

    private lateinit var expectedNotMatchedPlacementRequests: List<PlacementRequestEntity>

    @BeforeEach
    fun setup() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withProbationRegion(
          probationRegionEntityFactory.produceAndPersist {
            withApArea(apAreaEntityFactory.produceAndPersist())
          },
        )
        withLocalAuthorityArea(
          localAuthorityEntityFactory.produceAndPersist(),
        )
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withRoom(room)
      }

      placementRequestsWithNoBooking = createPlacementRequests(4, isWithdrawn = false, isReallocated = false, isParole = true)

      placementRequestsWithBooking = createPlacementRequests(4, isWithdrawn = false, isReallocated = false, isParole = false)
      placementRequestsWithBooking.forEach {
        it.booking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBed(bed)
        }

        realPlacementRequestRepository.save(it)
      }

      placementRequestsWithACancelledBooking = createPlacementRequests(2, isWithdrawn = false, isReallocated = false, isParole = false)
      placementRequestsWithACancelledBooking.forEach {
        val booking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBed(bed)
        }

        it.booking = booking
        realPlacementRequestRepository.save(it)

        cancellationEntityFactory.produceAndPersist {
          withBooking(booking)
          withReason(cancellationReasonEntityFactory.produceAndPersist())
        }
      }

      placementRequestsWithABookingNotMade = createPlacementRequests(3, isWithdrawn = false, isReallocated = false, isParole = true)
      placementRequestsWithABookingNotMade.forEach {
        bookingNotMadeFactory.produceAndPersist {
          withPlacementRequest(it)
        }
      }

      expectedNotMatchedPlacementRequests = listOf(placementRequestsWithNoBooking, placementRequestsWithACancelledBooking).flatten()
    }

    @Test
    fun `allForDashboard returns all results when no page is provided`() {
      val matchedPlacementRequests = realPlacementRequestRepository.allForDashboard(PlacementRequestStatus.matched.value)
      val notMatchedPlacementRequests = realPlacementRequestRepository.allForDashboard(PlacementRequestStatus.notMatched.value)
      val unableToMatchPlacementRequests = realPlacementRequestRepository.allForDashboard(PlacementRequestStatus.unableToMatch.value)

      assertThat(matchedPlacementRequests.content.map { it.id }).isEqualTo(placementRequestsWithBooking.map { it.id })
      assertThat(notMatchedPlacementRequests.content.map { it.id }).isEqualTo(expectedNotMatchedPlacementRequests.map { it.id })
      assertThat(unableToMatchPlacementRequests.content.map { it.id }).isEqualTo(placementRequestsWithABookingNotMade.map { it.id })
    }

    @Test
    fun `allForDashboard returns paginated results when a page is provided`() {
      val pageable = PageRequest.of(1, 2, Sort.by("created_at"))
      val matchedPlacementRequests = realPlacementRequestRepository.allForDashboard(PlacementRequestStatus.matched.value, pageable = pageable)
      val notMatchedPlacementRequests = realPlacementRequestRepository.allForDashboard(PlacementRequestStatus.notMatched.value, pageable = pageable)
      val unableToMatchPlacementRequests = realPlacementRequestRepository.allForDashboard(PlacementRequestStatus.unableToMatch.value, pageable = pageable)

      assertThat(matchedPlacementRequests.content.map { it.id }).isEqualTo(listOf(placementRequestsWithBooking[2], placementRequestsWithBooking[3]).map { it.id })
      assertThat(notMatchedPlacementRequests.content.map { it.id }).isEqualTo(listOf(expectedNotMatchedPlacementRequests[2], expectedNotMatchedPlacementRequests[3]).map { it.id })
      assertThat(unableToMatchPlacementRequests.content.map { it.id }).isEqualTo(listOf(placementRequestsWithABookingNotMade[2]).map { it.id })
    }

    @Test
    fun `allForDashboard returns all results when no criteria is provided`() {
      val pageable = PageRequest.of(0, 20, Sort.by("created_at"))
      val allPlacementRequests = realPlacementRequestRepository.allForDashboard(pageable = pageable)

      val allPlacementRequestsToExpect = (placementRequestsWithBooking + placementRequestsWithNoBooking + placementRequestsWithACancelledBooking + placementRequestsWithABookingNotMade)
        .sortedBy { it.createdAt }

      assertThat(allPlacementRequests.content.map { it.id }).isEqualTo(allPlacementRequestsToExpect.map { it.id })
    }

    @Test
    fun `allForDashboard only returns results for CRN when specified `() {
      val crn = "CRN456"
      val requestsForCrn = createPlacementRequests(2, isWithdrawn = false, isReallocated = false, isParole = true, crn = crn)

      val pageable = PageRequest.of(0, 2, Sort.by("created_at"))
      val results = realPlacementRequestRepository.allForDashboard(status = PlacementRequestStatus.notMatched.value, crn = crn, pageable = pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsForCrn.map { it.id })
    }

    @Test
    fun `allForDashboard only returns results for CRN when specified (case insensitive) `() {
      val crn = "CRN456"
      val requestsForCrn = createPlacementRequests(2, isWithdrawn = false, isReallocated = false, isParole = true, crn = crn)

      val pageable = PageRequest.of(0, 2, Sort.by("created_at"))
      val results = realPlacementRequestRepository.allForDashboard(status = PlacementRequestStatus.notMatched.value, crn = "crN456", pageable = pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsForCrn.map { it.id })
    }

    @Test
    fun `allForDashboard only returns results when name is specified in crnOrName`() {
      val name = "John Smith".uppercase()
      val requestsForCrn = createPlacementRequests(2, isWithdrawn = false, isReallocated = false, isParole = true, name = name)

      val pageable = PageRequest.of(0, 2, Sort.by("created_at"))
      val results = realPlacementRequestRepository.allForDashboard(status = PlacementRequestStatus.notMatched.value, crnOrName = "%$name%", pageable = pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsForCrn.map { it.id })
    }

    @Test
    fun `allForDashboard only returns results for tier when specified`() {
      val tier = "A2"
      val requestsForCrn = createPlacementRequests(2, isWithdrawn = false, isReallocated = false, isParole = true, tier = tier)

      val pageable = PageRequest.of(0, 2, Sort.by("created_at"))
      val results = realPlacementRequestRepository.allForDashboard(status = PlacementRequestStatus.notMatched.value, tier = tier, pageable = pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsForCrn.map { it.id })
    }

    @Test
    fun `allForDashboard allows sorting by an application's created_at date`() {
      val pageable = PageRequest.of(0, 20, Sort.by("application_date"))
      val allPlacementRequests = realPlacementRequestRepository.allForDashboard(pageable = pageable)

      val allPlacementRequestsToExpect = (placementRequestsWithNoBooking + placementRequestsWithABookingNotMade + placementRequestsWithBooking + placementRequestsWithACancelledBooking)
        .sortedBy { it.application.createdAt }

      assertThat(allPlacementRequests.content.map { it.id }).isEqualTo(allPlacementRequestsToExpect.map { it.id })
    }

    @Test
    fun `allForDashboard only returns results where start date after arrivalDateFrom when specified`() {
      val expectedArrival = LocalDate.parse("2030-08-08")
      val requestsForDate = createPlacementRequests(2, isWithdrawn = false, isReallocated = false, isParole = true, expectedArrival = expectedArrival)

      val pageable = PageRequest.of(0, 2, Sort.by("created_at"))
      val results = realPlacementRequestRepository.allForDashboard(status = PlacementRequestStatus.notMatched.value, arrivalDateFrom = expectedArrival.minusDays(1), pageable = pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsForDate.map { it.id })
    }

    @Test
    fun `allForDashboard only returns results where start date is before arrivalDateTo when specified`() {
      val expectedArrival = LocalDate.parse("2023-08-08")
      val requestsForDate = createPlacementRequests(2, isWithdrawn = false, isReallocated = false, isParole = true, expectedArrival = expectedArrival)

      val pageable = PageRequest.of(0, 2, Sort.by("created_at"))
      val results = realPlacementRequestRepository.allForDashboard(status = PlacementRequestStatus.notMatched.value, arrivalDateTo = expectedArrival.plusDays(1), pageable = pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsForDate.map { it.id })
    }

    @Test
    fun `allForDashboard only returns parole results where requestType of 'parole' is specified`() {
      createPlacementRequests(1, isWithdrawn = false, isReallocated = false, isParole = false)
      val requestsWithTypeParole = createPlacementRequests(1, isWithdrawn = false, isReallocated = false, isParole = true)

      val pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("created_at")))
      val results = realPlacementRequestRepository.allForDashboard(requestType = PlacementRequestRequestType.parole.value, pageable = pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsWithTypeParole.map { it.id })
    }

    @Test
    fun `allForDashboard only returns non parole results where requestType of 'standardRelease' is specified`() {
      createPlacementRequests(1, isWithdrawn = false, isReallocated = false, isParole = true)
      val requestsWithTypeStandardRelease = createPlacementRequests(1, isWithdrawn = false, isReallocated = false, isParole = false)

      val pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("created_at")))
      val results = realPlacementRequestRepository.allForDashboard(requestType = PlacementRequestRequestType.standardRelease.value, pageable = pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsWithTypeStandardRelease.map { it.id })
    }
  }

  private fun createPlacementRequests(count: Int, isWithdrawn: Boolean, isReallocated: Boolean, isParole: Boolean, crn: String? = null, name: String? = null, expectedArrival: LocalDate? = null, tier: String? = null): List<PlacementRequestEntity> {
    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(
        probationRegionEntityFactory.produceAndPersist {
          withApArea(apAreaEntityFactory.produceAndPersist())
        },
      )
    }

    return List(count) {
      givenAPlacementRequest(user, user, user, reallocated = isReallocated, isWithdrawn = isWithdrawn, isParole = isParole, crn = crn, name = name, expectedArrival = expectedArrival, tier = tier).first
    }
  }
}
