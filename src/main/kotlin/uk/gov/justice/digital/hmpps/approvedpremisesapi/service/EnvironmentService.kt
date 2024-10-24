package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class EnvironmentService(
  val environment: Environment,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @PostConstruct
  fun logProfiles() {
    log.info("Active profiles are ${environment.activeProfiles}")
  }

  fun isLocal() = environment.activeProfiles.any { it.equals("local", ignoreCase = true) }
  fun isDev() = environment.activeProfiles.any { it.equals("dev", ignoreCase = true) }
}
