package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.mocks

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.test.context.event.annotation.BeforeTestMethod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService

/**
 * Provides a method to set a feature flag for a given test's execution
 *
 * Will fall back to application.yml configuration if no override found
 */
@Primary
@Service
class MockFeatureFlagService(
  val featureFlags: Map<String, Boolean>,
) : FeatureFlagService {
  private val log = LoggerFactory.getLogger(this::class.java)

  private val testOverrides = mutableMapOf<String, Boolean>()

  fun setFlag(name: String, value: Boolean) {
    log.info("Override feature flag $name to $value for test")
    testOverrides[name] = value
  }

  @BeforeTestMethod
  fun reset() {
    testOverrides.clear()
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  override fun getBooleanFlag(key: String): Boolean {
    return testOverrides.getOrDefault(key, featureFlags.getOrDefault(key, false))
  }
}
