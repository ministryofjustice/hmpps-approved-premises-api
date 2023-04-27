package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.cache.preemptive

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import redis.lock.redlock.RedLock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cache.preemptive.OffenderDetailsCacheRefreshWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository

class OffenderDetailsCacheRefreshWorkerTest {
  private val mockApplicationRepository = mockk<ApplicationRepository>()
  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockCommunityApiClient = mockk<CommunityApiClient>()
  private val mockRedLock = mockk<RedLock>()

  private val offenderDetailsCacheRefreshWorker = OffenderDetailsCacheRefreshWorker(
    mockApplicationRepository,
    mockBookingRepository,
    mockCommunityApiClient,
    mockRedLock
  )

  @Test
  fun `work calls Community API Client for each distinct CRN from Bookings and Applications`() {
    every { mockApplicationRepository.getDistinctCrns() } returns listOf("CRN123", "CRN456", "CRN789")
    every { mockBookingRepository.getDistinctCrns() } returns listOf("CRN321", "CRN456", "CRN999")

    every { mockCommunityApiClient.getOffenderDetailSummaryWithCall(any()) } returns ClientResult.Success(
      HttpStatus.OK,
      OffenderDetailsSummaryFactory().produce()
    )

    offenderDetailsCacheRefreshWorker.work { false }

    listOf("CRN123", "CRN456", "CRN789", "CRN321", "CRN999").forEach {
      verify(exactly = 1) {
        mockCommunityApiClient.getOffenderDetailSummaryWithCall(it)
      }
    }

    verify(exactly = 5) {
      mockCommunityApiClient.getOffenderDetailSummaryWithCall(any())
    }
  }
}
