package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import java.util.UUID

data class Cas2SuitableApplication(
  val uiUrl: String,
  val application: Cas2ExternalApplicationDto,
)

data class Cas2ExternalApplicationDto(
  val id: UUID,
  val status: String?,
)
