package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import com.fasterxml.jackson.annotation.JsonAlias
import org.springframework.stereotype.Component
import org.springframework.web.service.annotation.GetExchange
import java.net.URI
import java.time.LocalDate

@ConditionalOnProperty(prefix = "feature-flags", name = ["cas2-sqs-listener-enabled"], havingValue = "true")
@Component
interface ManagePomCasesClient {
  @GetExchange
  fun getDetails(url: URI): Handover?

  @GetExchange
  fun getPomAllocation(url: URI): PomAllocation?
}

data class Handover(
  @JsonAlias("nomsNumber") val nomsId: String,
  @JsonAlias("handoverDate") val date: LocalDate?,
  @JsonAlias("handoverStartDate") val startDate: LocalDate?,
)

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
