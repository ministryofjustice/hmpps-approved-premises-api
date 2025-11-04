package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param resultsRoomCount How many distinct Rooms the Beds in the results belong to
 * @param resultsPremisesCount How many distinct Premises the Beds in the results belong to
 * @param resultsBedCount How many Beds are in the results
 * @param results
 */
data class Cas3BedspaceSearchResults(

  @Schema(
    example = "null",
    required = true,
    description = "How many distinct Rooms the Beds in the results belong to",
  )
  val resultsRoomCount: Int,

  @Schema(
    example = "null",
    required = true,
    description = "How many distinct Premises the Beds in the results belong to",
  )
  val resultsPremisesCount: Int,

  @Schema(example = "null", required = true, description = "How many Beds are in the results")
  val resultsBedCount: Int,

  val results: List<Cas3BedspaceSearchResult>,
)
