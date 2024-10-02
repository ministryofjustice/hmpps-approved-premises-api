package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class EnvironmentService(
  val environment: Environment
) {
  fun isLocal() = environment.activeProfiles.contains("local")
  fun isDev() = environment.activeProfiles.contains("dev")
}