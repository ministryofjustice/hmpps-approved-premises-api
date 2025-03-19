package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import org.springframework.stereotype.Component
import org.springframework.web.service.annotation.GetExchange
import java.net.URI

@Component
interface PrisonerSearchClient {

  @GetExchange
  fun getPrisoner(url: URI): Prisoner?
}

data class Prisoner(
  val prisonId: String,
  val prisonName: String,
)
