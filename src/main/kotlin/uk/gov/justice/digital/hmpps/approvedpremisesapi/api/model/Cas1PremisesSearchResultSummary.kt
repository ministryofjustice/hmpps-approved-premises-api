package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1PremisesSearchResultSummary(

  val id: java.util.UUID,
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
)
