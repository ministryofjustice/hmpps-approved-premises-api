package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultRoomSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
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
      JsonSubTypes.Type(value = TemporaryAccommodationBedSearchResult::class, name = "temporary-accommodation")
)

interface BedSearchResult{
                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val serviceName: ServiceName

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val premises: BedSearchResultPremisesSummary

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val room: BedSearchResultRoomSummary

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val bed: BedSearchResultBedSummary


}

