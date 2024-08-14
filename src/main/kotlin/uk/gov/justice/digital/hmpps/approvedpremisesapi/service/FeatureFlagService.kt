package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

interface FeatureFlagService {
  fun getBooleanFlag(key: String): Boolean
  fun isUseApAndDeliusToUpdateUsersEnabled(): Boolean
}

@Configuration
class FeatureFlagConfig {

  @Bean
  @ConfigurationProperties(prefix = "feature-flags")
  fun featureFlags(): Map<String, Boolean?> {
    return mutableMapOf()
  }
}

@Service
class SpringConfigFeatureFlagService(
  val featureFlags: Map<String, Boolean>,
) : FeatureFlagService {
  private val log = LoggerFactory.getLogger(this::class.java)

  @PostConstruct
  fun post() {
    log.info("Feature flags are $featureFlags")
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  override fun getBooleanFlag(key: String): Boolean {
    return featureFlags.getOrDefault(key, false)
  }

  override fun isUseApAndDeliusToUpdateUsersEnabled(): Boolean {
    return getBooleanFlag("use-ap-and-delius-to-update-users")
  }
}
