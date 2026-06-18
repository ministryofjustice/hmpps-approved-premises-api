package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import java.util.UUID

data class Cas1PremisesSearchResultSummary(

  val id: UUID,
  val apType: ApType,
  @Schema(example = "Hope House")
  val name: String,
  @Schema(description = "Full address, excluding postcode")
  val fullAddress: String,
  val apArea: NamedId,
  @Schema(description = "Room and premise characteristics")
  val characteristics: List<Cas1SpaceCharacteristic>,
  @Schema(example = "LS1 3AD")
  val postcode: String? = null,
  @Schema(
    example = "No hate based offences",
    description = "A list of restrictions that apply specifically to this approved premises.",
  )
  val localRestrictions: List<String> = emptyList(),
)
