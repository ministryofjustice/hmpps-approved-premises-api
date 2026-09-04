package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Details about the most current application")
data class Cas2SuitableApplication(
  val uiUrl: String,
  val application: Cas2ExternalApplicationDto,
)

data class Cas2ExternalApplicationDto(
  val id: UUID,
  val status: String?,
)
