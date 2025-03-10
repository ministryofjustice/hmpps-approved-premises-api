package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.service.annotation.GetExchange
import java.net.URI

@ConditionalOnProperty(prefix = "feature-flags", name = ["cas2-sqs-listener-enabled"], havingValue = "true")
@Component
interface ManagePomCasesClient {

  @GetExchange
  fun getPomAllocation(url: URI): PomAllocation?
}

sealed interface AllocationResponse

data class PomAllocation(
  val prison: Prison,
) : AllocationResponse

data class Prison(
  val code: String,
)
