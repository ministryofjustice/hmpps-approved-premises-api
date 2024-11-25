package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param serviceName
 * @param premises
 * @param room
 * @param bed
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "serviceName", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(value = ApprovedPremisesBedSearchResult::class, name = "approved-premises"),
  JsonSubTypes.Type(value = TemporaryAccommodationBedSearchResult::class, name = "temporary-accommodation"),
)
interface BedSearchResult {
  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val serviceName: ServiceName

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val premises: BedSearchResultPremisesSummary

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val room: BedSearchResultRoomSummary

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val bed: BedSearchResultBedSummary
}
