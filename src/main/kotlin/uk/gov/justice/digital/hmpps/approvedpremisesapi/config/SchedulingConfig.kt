package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.scheduling.annotation.EnableScheduling
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@ConditionalOnProperty(value = ["scheduling.enabled"], havingValue = "true", matchIfMissing = false)
class SchedulingConfig {

  private val log = LoggerFactory.getLogger(this::class.java)

  /**
   * Configures a custom error handler on the task scheduler used for `@Scheduled` jobs.
   *
   * By default, unhandled exceptions thrown from scheduled methods are only logged by Spring's
   * internal error handler and do not propagate through standard web exception handlers.
   * This customizer ensures any uncaught exceptions in scheduled background tasks are logged
   * and forwarded to Sentry for alerting.
   */
  @Bean
  fun customiseTaskScheduler(sentryService: SentryService) = ThreadPoolTaskSchedulerCustomizer { scheduler ->
    scheduler.setThreadNamePrefix("scheduled-task-")
    scheduler.setErrorHandler { throwable ->
      log.error("Error occurred in scheduled task", throwable)
      sentryService.captureException(throwable)
    }
  }

  @Bean
  fun lockProvider(
    connectionFactory: RedisConnectionFactory,
    @Value("\${spring.profiles.active:default}") environment: String,
  ): LockProvider {
    log.info("Creating RedisLockProvider with environment '$environment'")
    return RedisLockProvider(connectionFactory, environment)
  }
}
