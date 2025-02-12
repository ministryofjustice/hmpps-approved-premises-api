package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.service.annotation.GetExchange
import java.net.URI

@ConditionalOnProperty(prefix = "feature-flags", name = ["domain-events-listener-enabled"], havingValue = "true")
@Component
interface PrisonerSearchClient {

  @GetExchange
  fun getPrisoner(url: URI): Prisoner?
}

data class Prisoner(
  val prisonId: String,
  val lastPrisonId: String,
  val prisonName: String,
)
