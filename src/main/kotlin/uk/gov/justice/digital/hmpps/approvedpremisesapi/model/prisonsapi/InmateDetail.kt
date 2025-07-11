package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

data class InmateDetail(
  val offenderNo: String,
  @JsonProperty("status")
  val custodyStatus: InmateStatus,
  val assignedLivingUnit: AssignedLivingUnit?,
)

data class AssignedLivingUnit(
  val agencyId: String,
  val locationId: Long,
  val description: String?,
  val agencyName: String,
)

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class InmateStatus {
  @JsonProperty("ACTIVE IN")
  IN,

  @JsonProperty("ACTIVE OUT")
  @JsonAlias("INACTIVE OUT")
  OUT,

  @JsonProperty("INACTIVE TRN")
  TRN,
}
