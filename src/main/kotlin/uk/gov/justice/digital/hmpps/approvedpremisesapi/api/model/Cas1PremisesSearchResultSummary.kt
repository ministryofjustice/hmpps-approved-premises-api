package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1PremisesSearchResultSummary(

  val id: java.util.UUID,
  val apType: ApType,
  @field:Schema(example = "Hope House")
  val name: String,
  @field:Schema(description = "Full address, excluding postcode")
  val fullAddress: String,
  val apArea: NamedId,
  @field:Schema(description = "Room and premise characteristics")
  val characteristics: List<Cas1SpaceCharacteristic>,
  @field:Schema(example = "LS1 3AD")
  val postcode: String? = null,
  @field:Schema(
    example = "No hate based offences",
    description = "A list of restrictions that apply specifically to this approved premises.",
  )
  val localRestrictions: List<String> = emptyList(),
)
