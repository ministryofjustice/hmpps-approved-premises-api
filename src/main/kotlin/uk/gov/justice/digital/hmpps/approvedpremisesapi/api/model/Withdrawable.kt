package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1WithdrawableDatePeriodDto
import java.util.UUID

data class Withdrawable(
  val id: UUID,
  val type: WithdrawableType,
  @Schema(description = "0, 1 or more dates can be specified depending upon the WithdrawableType")
  val dates: List<Cas1WithdrawableDatePeriodDto>,
)
