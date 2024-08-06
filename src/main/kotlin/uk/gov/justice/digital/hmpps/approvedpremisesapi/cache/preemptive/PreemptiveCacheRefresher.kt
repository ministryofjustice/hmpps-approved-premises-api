package uk.gov.justice.digital.hmpps.approvedpremisesapi.cache.preemptive

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import redis.lock.redlock.LockResult
import redis.lock.redlock.RedLock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CacheRefreshExclusionsInmateDetailsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService

@Component
class PreemptiveCacheRefresher(
  private val flyway: Flyway,
  private val applicationRepository: ApplicationRepository,
  private val bookingRepository: BookingRepository,
  private val cacheRefreshExclusionsInmateDetailsRepository: CacheRefreshExclusionsInmateDetailsRepository,
  private val communityApiClient: CommunityApiClient,
  private val prisonsApiClient: PrisonsApiClient,
  private val sentryService: SentryService,
  @Value("\${preemptive-cache-logging-enabled}") private val loggingEnabled: Boolean,
  @Value("\${preemptive-cache-delay-ms}") private val delayMs: Long,
  @Value("\${preemptive-cache-lock-duration-ms}") private val lockDurationMs: Int,
  redLock: RedLock,
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
        loggingEnabled,
        delayMs,
        redLock,
        lockDurationMs,
      )

      preemptiveCacheThreads += InmateDetailsCacheRefreshWorker(
        applicationRepository,
        bookingRepository,
        cacheRefreshExclusionsInmateDetailsRepository,
        prisonsApiClient,
        sentryService,
        loggingEnabled,
        delayMs,
        redLock,
        lockDurationMs,
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
  private val cacheName: String,
  private val lockDurationMs: Int,
) : Thread() {
  protected val log = LoggerFactory.getLogger(this::class.java)

  @Volatile
  var shuttingDown = false

  override fun run() {
    while (!shuttingDown) {
      var lock: LockResult? = null
      try {
        lock = redLock.lock(cacheName, lockDurationMs) ?: continue

        log.info("Got cache refresh lock for $cacheName")
        val lockExpiresAt = System.currentTimeMillis() + lock.validity

        try {
          work { shouldStop(lockExpiresAt) }
        } catch (exception: Exception) {
          log.error("Unhandled exception refreshing cache $cacheName", exception)
        }

        while (!shouldStop(lockExpiresAt)) {
          interruptableSleep(1000)
        }

        attemptToReleaseLock(lock)
      } catch (exception: Exception) {
        log.error("Unhandled exception locking/unlocking cache $cacheName", exception)
        attemptToReleaseLock(lock)
      }

      interruptableSleep(10000)
    }
  }

  private fun shouldStop(lockExpiresAt: Double) = shuttingDown || System.currentTimeMillis() > lockExpiresAt

  private fun attemptToReleaseLock(lock: LockResult?) {
    if (lock == null) return

    try {
      redLock.release(lock.resource, lock.value)
      log.info("Released cache refresh lock for $cacheName")
    } catch (exception: Exception) {
      log.error("Failed to release lock", exception)
    }
  }

  abstract fun work(checkShouldStop: () -> Boolean)
}
