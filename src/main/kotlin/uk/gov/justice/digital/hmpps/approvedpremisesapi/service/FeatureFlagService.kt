package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service

interface FeatureFlagService {
  fun getBooleanFlag(key: String): Boolean

  companion object {
    const val FEATURE_FLAG_INCLUDE_TIER_V3: String = "include-tier-v3"
    const val FEATURE_FLAG_USE_TIER_V3: String = "use-tier-v3"
  }
}

@Configuration
class FeatureFlagConfig {

  @Bean
  @ConfigurationProperties(prefix = "feature-flags")
  fun featureFlags(): Map<String, Boolean?> = mutableMapOf()
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
  override fun getBooleanFlag(key: String): Boolean = featureFlags.getOrDefault(key, false)
}
