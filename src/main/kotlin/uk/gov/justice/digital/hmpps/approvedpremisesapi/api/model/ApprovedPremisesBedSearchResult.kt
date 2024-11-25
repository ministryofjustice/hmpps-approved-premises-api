package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param distanceMiles how many miles away from the postcode district the Premises this Bed belongs to is
 */
data class ApprovedPremisesBedSearchResult(

  @Schema(example = "null", required = true, description = "how many miles away from the postcode district the Premises this Bed belongs to is")
  @get:JsonProperty("distanceMiles", required = true) val distanceMiles: java.math.BigDecimal,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("serviceName", required = true) override val serviceName: ServiceName,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("premises", required = true) override val premises: BedSearchResultPremisesSummary,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("room", required = true) override val room: BedSearchResultRoomSummary,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bed", required = true) override val bed: BedSearchResultBedSummary,
) : BedSearchResult {
}
