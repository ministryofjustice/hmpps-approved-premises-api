package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import org.springframework.stereotype.Component
import org.springframework.web.service.annotation.GetExchange
import java.net.URI

@Component
interface ManagePomCasesClient {

  @GetExchange
  fun getPomAllocation(url: URI): PomAllocation?
}

sealed interface AllocationResponse

data class PomAllocation(
  val manager: Manager,
  val prison: Prison,
) : AllocationResponse

data object PomDeallocated : AllocationResponse

data object PomNotAllocated : AllocationResponse

data class Manager(
  val code: Long,
)

data class Prison(
  val code: String,
)
