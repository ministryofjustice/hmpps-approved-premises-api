package uk.gov.justice.digital.hmpps.approvedpremisesapi.cache.preemptive

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Component
import redis.lock.redlock.RedLock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository

@Component
class PreemptiveCacheRefresher(
  private val flyway: Flyway,
  private val applicationRepository: ApplicationRepository,
  private val bookingRepository: BookingRepository,
  private val communityApiClient: CommunityApiClient,
  private val prisonsApiClient: PrisonsApiClient,
  redLock: RedLock
) : DisposableBean {
  protected val log = LoggerFactory.getLogger(this::class.java)
  private val preemptiveCacheThreads = mutableListOf<CacheRefreshWorker>()

  @Volatile
  var shuttingDown = false

  init {
    Thread {
      while (!haveFlywayMigrationsFinished()) {
        if (shuttingDown) return@Thread
        interruptableSleep(100)
      }

      preemptiveCacheThreads += OffenderDetailsCacheRefreshWorker(
        applicationRepository,
        bookingRepository,
        communityApiClient,
        prisonsApiClient,
        redLock
      )

      log.info("Starting preemptive cache refresh threads")
      preemptiveCacheThreads.forEach(CacheRefreshWorker::start)
    }.start()
  }

  override fun destroy() {
    shuttingDown = true

    preemptiveCacheThreads.forEach {
      it.shuttingDown = true
      it.interrupt()
    }
  }

  fun haveFlywayMigrationsFinished(): Boolean {
    return try {
      return allPendingNonRepeatableFlywayMigrationsAreComplete()
    } catch (exception: FlywayException) {
      log.error("Failed to validate flyway", exception)
      false
    }
  }

  private fun allPendingNonRepeatableFlywayMigrationsAreComplete() =
    flyway.info().pending().none { !it.script.startsWith("R__") }
}

fun interruptableSleep(millis: Long) {
  try {
    Thread.sleep(millis)
  } catch (_: InterruptedException) {
  }
}

abstract class CacheRefreshWorker(
  private val redLock: RedLock,
  private val cacheName: String
) : Thread() {
  protected val log = LoggerFactory.getLogger(this::class.java)

  private val twoMinutesInMilliseconds = 120_000

  @Volatile
  var shuttingDown = false

  override fun run() {
    while (! shuttingDown) {
      interruptableSleep(500)

      val lock = redLock.lock(cacheName, twoMinutesInMilliseconds) ?: continue

      log.info("Got cache refresh lock for $cacheName")
      val lockExpiresAt = System.currentTimeMillis() + lock.validity

      try {
        work { shuttingDown || System.currentTimeMillis() > lockExpiresAt }
      } catch (exception: Exception) {
        log.error("Unhandled exception refreshing cache $cacheName", exception)
      }

      redLock.release(lock.resource, lock.value)
      log.info("Released cache refresh lock for $cacheName")
    }
  }

  abstract fun work(checkShouldStop: () -> Boolean)
}
