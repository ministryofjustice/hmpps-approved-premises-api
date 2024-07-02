package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import io.flipt.api.FliptClient
import io.flipt.api.evaluation.models.EvaluationRequest
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

interface FeatureFlagService {
  fun getBooleanFlag(key: String, default: Boolean): Boolean
}

@Component
@ConfigurationProperties(prefix = "feature-flags")
data class FeatureFlagsLocalOverrideConfig(
  var localOverrides: Map<String, Boolean> = mapOf(),
)

@Service
class FliptFeatureFlagService(
  private val client: FliptClient?,
  private val sentryService: SentryService,
  private val localOverridesConfig: FeatureFlagsLocalOverrideConfig,
) : FeatureFlagService {
  private val log = LoggerFactory.getLogger(this::class.java)

  @PostConstruct
  fun logFliptStatus() {
    if (client == null) {
      log.warn("Flipt client not enabled, will use default values.")
    } else {
      val testFlag = getBooleanFlag("test-flag", false)
      log.info("Flipt enabled. Value of test-flag is $testFlag")
    }
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  override fun getBooleanFlag(key: String, default: Boolean) = try {
    if (client == null) {
      localOverridesConfig.localOverrides.getOrDefault(key, default)
    } else {
      client.evaluation()
        .evaluateBoolean(EvaluationRequest.builder().namespaceKey("community-accommodation").flagKey(key).build())
        .isEnabled
    }
  } catch (e: Exception) {
    sentryService.captureException(e)
    log.error("Could not retrieve feature flag $key. Will return default value $default")
    default
  }
}
