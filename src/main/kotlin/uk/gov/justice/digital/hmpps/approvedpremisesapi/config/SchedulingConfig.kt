package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@ConditionalOnProperty(value = ["scheduling.enabled"], havingValue = "true", matchIfMissing = false)
class SchedulingConfig {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Bean
  fun lockProvider(
    connectionFactory: RedisConnectionFactory,
    @Value("\${spring.profiles.active:default}") environment: String,
  ): LockProvider {
    log.info("Creating RedisLockProvider with environment '$environment'")
    return RedisLockProvider(connectionFactory, environment)
  }
}
