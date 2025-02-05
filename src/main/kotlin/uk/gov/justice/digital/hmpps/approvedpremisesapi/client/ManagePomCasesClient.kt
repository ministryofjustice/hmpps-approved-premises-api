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
  val manager: PomDetail,
  val prison: Prison,
) : AllocationResponse

data object PomDeallocated : AllocationResponse

data object PomNotAllocated : AllocationResponse

data class PomDetail(
  val forename: String,
  val surname: String,
  val email: String?,
) {
  val name = Name(forename = forename, surname = surname)
}

data class Prison(
  val code: String,
)

data class Name(
  val forename: String,
  val middleName: String? = null,
  val surname: String,
)
