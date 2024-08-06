package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.cache.preemptive

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import redis.lock.redlock.RedLock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cache.preemptive.InmateDetailsCacheRefreshWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PreemptiveCacheEntryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CacheRefreshExclusionsInmateDetailsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService

class InmateDetailsCacheRefreshWorkerTest {
  private val mockApplicationRepository = mockk<ApplicationRepository>()
  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockCacheRefreshExclusionsInmateDetailsRepository = mockk<CacheRefreshExclusionsInmateDetailsRepository>()
  private val mockPrisonsApiClient = mockk<PrisonsApiClient>()
  private val mockRedLock = mockk<RedLock>()
  private val mockSentryService = mockk<SentryService>()

  private val offenderDetailsCacheRefreshWorker = InmateDetailsCacheRefreshWorker(
    mockApplicationRepository,
    mockBookingRepository,
    mockCacheRefreshExclusionsInmateDetailsRepository,
    mockPrisonsApiClient,
    mockSentryService,
    false,
    0,
    mockRedLock,
    10000,
  )

  @Test
  fun `work calls Prisons API Client for each distinct NOMS Number from Bookings that requires a refresh`() {
    val inmateDetailsNotCached = listOf("NOMS123", "NOMS456").map { noms ->
      InmateDetailFactory()
        .withOffenderNo(noms)
        .produce()
    }

    val offenderDetailsCachedButNeedingRefresh = listOf("NOMS789", "NOMS321").map { noms ->
      InmateDetailFactory()
        .withOffenderNo(noms)
        .produce()
    }

    val offenderDetailsCachedNotNeedingRefresh = listOf("NOMS777", "NOMS999").map { noms ->
      InmateDetailFactory()
        .withOffenderNo(noms)
        .produce()
    }

    val offenderDetailsExcluded = InmateDetailFactory().withOffenderNo("NOMSIGNORE").produce()

    every { mockApplicationRepository.getDistinctNomsNumbers() } returns listOf("NOMS123", "NOMS456", "NOMS789", "NOMSIGNORE")
    every { mockBookingRepository.getDistinctNomsNumbers() } returns listOf("NOMS321", "NOMS456", "NOMS999", "NOMS777", "NOMSIGNORE")
    every { mockCacheRefreshExclusionsInmateDetailsRepository.getDistinctNomsNumbers() } returns listOf("NOMSIGNORE")

    inmateDetailsNotCached.forEach {
      every { mockPrisonsApiClient.getInmateDetailsCacheEntryStatus(it.offenderNo) } returns PreemptiveCacheEntryStatus.MISS

      every { mockPrisonsApiClient.getInmateDetailsWithCall(it.offenderNo) } returns ClientResult.Success(
        HttpStatus.OK,
        it,
      )
    }

    offenderDetailsCachedButNeedingRefresh.forEach {
      every { mockPrisonsApiClient.getInmateDetailsCacheEntryStatus(it.offenderNo) } returns PreemptiveCacheEntryStatus.REQUIRES_REFRESH

      every { mockPrisonsApiClient.getInmateDetailsWithCall(it.offenderNo) } returns ClientResult.Success(
        HttpStatus.OK,
        it,
      )
    }

    offenderDetailsCachedNotNeedingRefresh.forEach {
      every { mockPrisonsApiClient.getInmateDetailsCacheEntryStatus(it.offenderNo) } returns PreemptiveCacheEntryStatus.EXISTS
    }

    offenderDetailsCacheRefreshWorker.work { false }

    (inmateDetailsNotCached + offenderDetailsCachedButNeedingRefresh).forEach {
      verify(exactly = 1) {
        mockPrisonsApiClient.getInmateDetailsWithCall(it.offenderNo)
      }
    }

    offenderDetailsCachedNotNeedingRefresh.forEach {
      verify(exactly = 0) {
        mockPrisonsApiClient.getInmateDetailsWithCall(it.offenderNo)
      }
    }

    verify(exactly = 0) {
      mockPrisonsApiClient.getInmateDetailsWithCall(offenderDetailsExcluded.offenderNo)
    }

    verify(exactly = 4) {
      mockPrisonsApiClient.getInmateDetailsWithCall(any())
    }
  }
}
