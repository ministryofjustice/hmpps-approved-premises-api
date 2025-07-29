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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import java.time.LocalDate

/**
Some of these tests are duplicated in [PlacementRequestsTest]

Ideally all tests should be via the API, where possible
 */
class PlacementRequestRepositoryTest : IntegrationTestBase() {
  @Autowired
  lateinit var realPlacementRequestRepository: PlacementRequestRepository

  @Nested
  inner class AllForDashboard {
    private lateinit var placementRequestsWithBooking: List<PlacementRequestEntity>
    private lateinit var placementRequestsWithSpaceBooking: List<PlacementRequestEntity>
    private lateinit var placementRequestsWithNoBooking: List<PlacementRequestEntity>
    private lateinit var placementRequestsWithACancelledBooking: List<PlacementRequestEntity>
    private lateinit var placementRequestsWithACancelledSpaceBooking: List<PlacementRequestEntity>
    private lateinit var placementRequestsWithABookingNotMade: List<PlacementRequestEntity>

    private lateinit var expectedNotMatchedPlacementRequests: List<PlacementRequestEntity>

    @BeforeEach
    fun setup() {
      val premises = givenAnApprovedPremises()

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withRoom(room)
      }

      val (user) = givenAUser()

      placementRequestsWithNoBooking = createPlacementRequests(4, isWithdrawn = false, isReallocated = false, isParole = true)

      placementRequestsWithBooking = createPlacementRequests(4, isWithdrawn = false, isReallocated = false, isParole = false)
      placementRequestsWithBooking.forEach {
        it.booking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBed(bed)
        }

        realPlacementRequestRepository.save(it)
      }

      placementRequestsWithSpaceBooking = createPlacementRequests(4, isWithdrawn = false, isReallocated = false, isParole = false)
      placementRequestsWithSpaceBooking.forEach {
        it.spaceBookings.add(
          cas1SpaceBookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withPlacementRequest(it)
            withApplication(it.application)
            withCreatedBy(user)
          },
        )
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

      placementRequestsWithACancelledSpaceBooking = createPlacementRequests(2, isWithdrawn = false, isReallocated = false, isParole = false)
      placementRequestsWithACancelledSpaceBooking.forEach {
        it.spaceBookings.add(
          cas1SpaceBookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withPlacementRequest(it)
            withApplication(it.application)
            withCreatedBy(user)
            withCancellationOccurredAt(LocalDate.now())
          },
        )
      }

      placementRequestsWithABookingNotMade = createPlacementRequests(3, isWithdrawn = false, isReallocated = false, isParole = true)
      placementRequestsWithABookingNotMade.forEach {
        bookingNotMadeFactory.produceAndPersist {
          withPlacementRequest(it)
        }
      }

      expectedNotMatchedPlacementRequests = listOf(placementRequestsWithNoBooking, placementRequestsWithACancelledBooking, placementRequestsWithACancelledSpaceBooking).flatten()
    }

    @Test
    fun `Returns all results when no page is provided`() {
      val matchedPlacementRequests = realPlacementRequestRepository.allForDashboard(PlacementRequestStatus.matched.name)
      val notMatchedPlacementRequests = realPlacementRequestRepository.allForDashboard(PlacementRequestStatus.notMatched.name)
      val unableToMatchPlacementRequests = realPlacementRequestRepository.allForDashboard(PlacementRequestStatus.unableToMatch.name)

      assertThat(matchedPlacementRequests.content.map { it.id }).isEqualTo(
        (placementRequestsWithBooking + placementRequestsWithSpaceBooking).map { it.id },
      )
      assertThat(notMatchedPlacementRequests.content.map { it.id }).isEqualTo(expectedNotMatchedPlacementRequests.map { it.id })
      assertThat(unableToMatchPlacementRequests.content.map { it.id }).isEqualTo(placementRequestsWithABookingNotMade.map { it.id })
    }

    @Test
    fun `Returns paginated results when a page is provided`() {
      val pageable = PageRequest.of(1, 2, Sort.by("created_at"))
      val matchedPlacementRequests = realPlacementRequestRepository.allForDashboard(PlacementRequestStatus.matched.name, pageable = pageable)
      val notMatchedPlacementRequests = realPlacementRequestRepository.allForDashboard(PlacementRequestStatus.notMatched.name, pageable = pageable)
      val unableToMatchPlacementRequests = realPlacementRequestRepository.allForDashboard(PlacementRequestStatus.unableToMatch.name, pageable = pageable)

      assertThat(matchedPlacementRequests.content.map { it.id }).isEqualTo(listOf(placementRequestsWithBooking[2], placementRequestsWithBooking[3]).map { it.id })
      assertThat(notMatchedPlacementRequests.content.map { it.id }).isEqualTo(listOf(expectedNotMatchedPlacementRequests[2], expectedNotMatchedPlacementRequests[3]).map { it.id })
      assertThat(unableToMatchPlacementRequests.content.map { it.id }).isEqualTo(listOf(placementRequestsWithABookingNotMade[2]).map { it.id })
    }

    @Test
    fun `Returns all results when no criteria is provided`() {
      val pageable = PageRequest.of(0, 20, Sort.by("created_at"))
      val allPlacementRequests = realPlacementRequestRepository.allForDashboard(pageable = pageable)

      val allPlacementRequestsToExpect = (
        placementRequestsWithBooking +
          placementRequestsWithSpaceBooking +
          placementRequestsWithNoBooking +
          placementRequestsWithACancelledBooking +
          placementRequestsWithACancelledSpaceBooking +
          placementRequestsWithABookingNotMade
        )
        .sortedBy { it.createdAt }

      assertThat(allPlacementRequests.content.map { it.id }).isEqualTo(allPlacementRequestsToExpect.map { it.id })
    }

    @Test
    fun `Only returns results for CRN when specified `() {
      val crn = "CRN456"
      val requestsForCrn = createPlacementRequests(2, isWithdrawn = false, isReallocated = false, isParole = true, crn = crn)

      val pageable = PageRequest.of(0, 2, Sort.by("created_at"))
      val results = realPlacementRequestRepository.allForDashboard(status = PlacementRequestStatus.notMatched.name, crn = crn, pageable = pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsForCrn.map { it.id })
    }

    @Test
    fun `Only returns results for CRN when specified (case insensitive) `() {
      val crn = "CRN456"
      val requestsForCrn = createPlacementRequests(2, isWithdrawn = false, isReallocated = false, isParole = true, crn = crn)

      val pageable = PageRequest.of(0, 2, Sort.by("created_at"))
      val results = realPlacementRequestRepository.allForDashboard(status = PlacementRequestStatus.notMatched.name, crn = "crN456", pageable = pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsForCrn.map { it.id })
    }

    @Test
    fun `Only returns results when name is specified in crnOrName`() {
      val name = "John Smith".uppercase()
      val requestsForCrn = createPlacementRequests(2, isWithdrawn = false, isReallocated = false, isParole = true, name = name)

      val pageable = PageRequest.of(0, 2, Sort.by("created_at"))
      val results = realPlacementRequestRepository.allForDashboard(status = PlacementRequestStatus.notMatched.name, crnOrName = "%$name%", pageable = pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsForCrn.map { it.id })
    }

    @Test
    fun `Only returns results for tier when specified`() {
      val tier = "A2"
      val requestsForCrn = createPlacementRequests(2, isWithdrawn = false, isReallocated = false, isParole = true, tier = tier)

      val pageable = PageRequest.of(0, 2, Sort.by("created_at"))
      val results = realPlacementRequestRepository.allForDashboard(status = PlacementRequestStatus.notMatched.name, tier = tier, pageable = pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsForCrn.map { it.id })
    }

    @Test
    fun `Allows sorting by an application's created_at date`() {
      val pageable = PageRequest.of(0, 20, Sort.by("application_date"))
      val allPlacementRequests = realPlacementRequestRepository.allForDashboard(pageable = pageable)

      val allPlacementRequestsToExpect = (
        placementRequestsWithNoBooking +
          placementRequestsWithABookingNotMade +
          placementRequestsWithBooking +
          placementRequestsWithSpaceBooking +
          placementRequestsWithACancelledBooking +
          placementRequestsWithACancelledSpaceBooking
        )
        .sortedBy { it.application.createdAt }

      assertThat(allPlacementRequests.content.map { it.id }).isEqualTo(allPlacementRequestsToExpect.map { it.id })
    }

    @Test
    fun `Only returns results where start date after arrivalDateFrom when specified`() {
      val expectedArrival = LocalDate.parse("2030-08-08")
      val requestsForDate = createPlacementRequests(2, isWithdrawn = false, isReallocated = false, isParole = true, expectedArrival = expectedArrival)

      val pageable = PageRequest.of(0, 2, Sort.by("created_at"))
      val results = realPlacementRequestRepository.allForDashboard(status = PlacementRequestStatus.notMatched.name, arrivalDateFrom = expectedArrival.minusDays(1), pageable = pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsForDate.map { it.id })
    }

    @Test
    fun `Only returns results where start date is before arrivalDateTo when specified`() {
      val expectedArrival = LocalDate.parse("2023-08-08")
      val requestsForDate = createPlacementRequests(2, isWithdrawn = false, isReallocated = false, isParole = true, expectedArrival = expectedArrival)

      val pageable = PageRequest.of(0, 2, Sort.by("created_at"))
      val results = realPlacementRequestRepository.allForDashboard(status = PlacementRequestStatus.notMatched.name, arrivalDateTo = expectedArrival.plusDays(1), pageable = pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsForDate.map { it.id })
    }

    @Test
    fun `Only returns parole results where requestType of 'parole' is specified`() {
      createPlacementRequests(1, isWithdrawn = false, isReallocated = false, isParole = false)
      val requestsWithTypeParole = createPlacementRequests(1, isWithdrawn = false, isReallocated = false, isParole = true)

      val pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("created_at")))
      val results = realPlacementRequestRepository.allForDashboard(requestType = PlacementRequestRequestType.parole.name, pageable = pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsWithTypeParole.map { it.id })
    }

    @Test
    fun `Only returns non parole results where requestType of 'standardRelease' is specified`() {
      createPlacementRequests(1, isWithdrawn = false, isReallocated = false, isParole = true)
      val requestsWithTypeStandardRelease = createPlacementRequests(1, isWithdrawn = false, isReallocated = false, isParole = false)

      val pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("created_at")))
      val results = realPlacementRequestRepository.allForDashboard(requestType = PlacementRequestRequestType.standardRelease.name, pageable = pageable)

      assertThat(results.content.map { it.id }).isEqualTo(requestsWithTypeStandardRelease.map { it.id })
    }
  }

  private fun createPlacementRequests(count: Int, isWithdrawn: Boolean, isReallocated: Boolean, isParole: Boolean, crn: String? = null, name: String? = null, expectedArrival: LocalDate? = null, tier: String? = null): List<PlacementRequestEntity> {
    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(givenAProbationRegion())
    }

    return List(count) {
      givenAPlacementRequest(
        user, user, reallocated = isReallocated, isWithdrawn = isWithdrawn,
        isParole = isParole, crn = crn, name = name, expectedArrival = expectedArrival, tier = tier,
      ).first
    }
  }
}
