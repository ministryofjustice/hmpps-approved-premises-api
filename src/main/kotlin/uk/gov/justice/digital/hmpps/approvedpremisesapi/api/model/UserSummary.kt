package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.UUID

data class UserSummary(
  val id: UUID,
  val name: String,
  val emailAddress: String?,
)
