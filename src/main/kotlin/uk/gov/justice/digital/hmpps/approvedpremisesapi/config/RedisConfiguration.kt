package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate

@Configuration
class RedisConfiguration {
  @Bean
  fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
    val template = RedisTemplate<String, String>()
    template.connectionFactory = connectionFactory
    return template
  }
}
