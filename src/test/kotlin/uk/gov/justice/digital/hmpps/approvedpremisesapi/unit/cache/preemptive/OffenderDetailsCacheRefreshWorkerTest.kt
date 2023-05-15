package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.cache.preemptive

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import redis.lock.redlock.RedLock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cache.preemptive.OffenderDetailsCacheRefreshWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository

class OffenderDetailsCacheRefreshWorkerTest {
  private val mockApplicationRepository = mockk<ApplicationRepository>()
  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockCommunityApiClient = mockk<CommunityApiClient>()
  private val mockPrisonsApiClient = mockk<PrisonsApiClient>()
  private val mockRedLock = mockk<RedLock>()

  private val offenderDetailsCacheRefreshWorker = OffenderDetailsCacheRefreshWorker(
    mockApplicationRepository,
    mockBookingRepository,
    mockCommunityApiClient,
    mockPrisonsApiClient,
    false,
    0,
    mockRedLock
  )

  @Test
  fun `work calls Community API Client for each distinct CRN from Bookings and Applications and Prisons API for each that succeeded`() {
    val offenderDetailsWithSuccessfulCommunityApiResponses = listOf("CRN123", "CRN456", "CRN789", "CRN321").map { crn ->
      OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber("NOMS-$crn")
        .produce()
    }
    val crnsWithNotFoundCommunityApiResponses = listOf("CRN777", "CRN999")

    every { mockApplicationRepository.getDistinctCrns() } returns listOf("CRN123", "CRN456", "CRN789")
    every { mockBookingRepository.getDistinctCrns() } returns listOf("CRN321", "CRN456", "CRN999", "CRN777")

    offenderDetailsWithSuccessfulCommunityApiResponses.forEach {
      every { mockCommunityApiClient.getOffenderDetailSummaryWithCall(it.otherIds.crn) } returns ClientResult.Success(
        HttpStatus.OK,
        it
      )

      every { mockPrisonsApiClient.getInmateDetailsWithCall(it.otherIds.nomsNumber!!) } returns ClientResult.Success(
        HttpStatus.OK,
        InmateDetailFactory().produce()
      )
    }

    crnsWithNotFoundCommunityApiResponses.forEach {
      every { mockCommunityApiClient.getOffenderDetailSummaryWithCall(it) } returns ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/secure/offenders/crn/$it",
        HttpStatus.NOT_FOUND,
        body = null
      )
    }

    offenderDetailsCacheRefreshWorker.work { false }

    offenderDetailsWithSuccessfulCommunityApiResponses.forEach {
      verify(exactly = 1) {
        mockCommunityApiClient.getOffenderDetailSummaryWithCall(it.otherIds.crn)
      }

      verify(exactly = 1) {
        mockPrisonsApiClient.getInmateDetailsWithCall(it.otherIds.nomsNumber!!)
      }
    }

    crnsWithNotFoundCommunityApiResponses.forEach {
      verify(exactly = 1) {
        mockCommunityApiClient.getOffenderDetailSummaryWithCall(it)
      }
    }

    verify(exactly = 6) {
      mockCommunityApiClient.getOffenderDetailSummaryWithCall(any())
    }

    verify(exactly = 4) {
      mockPrisonsApiClient.getInmateDetailsWithCall(any())
    }
  }
}
