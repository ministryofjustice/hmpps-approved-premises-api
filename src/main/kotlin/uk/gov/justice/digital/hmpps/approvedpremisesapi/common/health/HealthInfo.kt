package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.health

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component

/**
 * Adds version data to the /health endpoint. This is called by the UI to display API details
 */
@Component
class HealthInfo(buildProperties: BuildProperties) : HealthIndicator {
  private val version: String = buildProperties.version ?: error("No version available on build properties")

  override fun health(): Health = Health.up().withDetail("version", version).build()
}
