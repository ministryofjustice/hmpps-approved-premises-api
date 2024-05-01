package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import io.flipt.api.FliptClient
import io.flipt.api.evaluation.models.EvaluationRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class FeatureFlagService(
  private val client: FliptClient?,
  private val sentryService: SentryService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @PostConstruct
  fun logFliptStatus() {
    if (client == null) {
      log.warn("Flipt client not enabled, will use default values.")
    } else {
      try {
        val testFlag = getBooleanFlag("test-flag")
        log.info("Flipt enabled. Value of test-flag is $testFlag")
      } catch (e: FeatureFlagException) {
        log.error("Error retrieving test flag", e)
      }
    }
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  fun getBooleanFlag(key: String, default: Boolean = true) = try {
    if (client == null) {
      default
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

  class FeatureFlagException(val key: String, e: Exception) : RuntimeException("Unable to retrieve '$key' flag", e)
}
