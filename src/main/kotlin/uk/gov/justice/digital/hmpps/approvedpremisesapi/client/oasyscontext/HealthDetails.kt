package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext

class HealthDetails(
  val health: HealthDetailsInner,
)

data class HealthDetailsInner(
  val generalHealth: Boolean?,
  val generalHealthSpecify: String?,
)
