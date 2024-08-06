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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PreemptiveCacheEntryStatus
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
    false,
    0,
    mockRedLock,
    10000,
  )

  @Test
  fun `work calls Community API Client for each distinct CRN from Bookings that requires a refresh`() {
    val offenderDetailsNotCached = listOf("CRN123", "CRN456").map { crn ->
      OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber("NOMS-$crn")
        .produce()
    }

    val offenderDetailsCachedButNeedingRefresh = listOf("CRN789", "CRN321").map { crn ->
      OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber("NOMS-$crn")
        .produce()
    }

    val offenderDetailsCachedNotNeedingRefresh = listOf("CRN777", "CRN999").map { crn ->
      OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber("NOMS-$crn")
        .produce()
    }

    every { mockApplicationRepository.getDistinctCrns() } returns listOf("CRN123", "CRN456", "CRN789")
    every { mockBookingRepository.getDistinctCrns() } returns listOf("CRN321", "CRN456", "CRN999", "CRN777")

    offenderDetailsNotCached.forEach {
      every { mockCommunityApiClient.getOffenderDetailsCacheEntryStatus(it.otherIds.crn) } returns PreemptiveCacheEntryStatus.MISS

      every { mockCommunityApiClient.getOffenderDetailSummaryWithCall(it.otherIds.crn) } returns ClientResult.Success(
        HttpStatus.OK,
        it,
      )
    }

    offenderDetailsCachedButNeedingRefresh.forEach {
      every { mockCommunityApiClient.getOffenderDetailsCacheEntryStatus(it.otherIds.crn) } returns PreemptiveCacheEntryStatus.REQUIRES_REFRESH

      every { mockCommunityApiClient.getOffenderDetailSummaryWithCall(it.otherIds.crn) } returns ClientResult.Success(
        HttpStatus.OK,
        it,
      )
    }

    offenderDetailsCachedNotNeedingRefresh.forEach {
      every { mockCommunityApiClient.getOffenderDetailsCacheEntryStatus(it.otherIds.crn) } returns PreemptiveCacheEntryStatus.EXISTS
    }

    offenderDetailsCacheRefreshWorker.work { null }

    (offenderDetailsNotCached + offenderDetailsCachedButNeedingRefresh).forEach {
      verify(exactly = 1) {
        mockCommunityApiClient.getOffenderDetailSummaryWithCall(it.otherIds.crn)
      }
    }

    offenderDetailsCachedNotNeedingRefresh.forEach {
      verify(exactly = 0) {
        mockCommunityApiClient.getOffenderDetailSummaryWithCall(it.otherIds.crn)
      }
    }

    verify(exactly = 4) {
      mockCommunityApiClient.getOffenderDetailSummaryWithCall(any())
    }
  }
}
