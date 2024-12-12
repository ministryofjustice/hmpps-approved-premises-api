package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param serviceName 
 * @param startDate The date the Bed will need to be free from
 * @param durationDays The number of days the Bed will need to be free from the start_date until
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "serviceName", visible = true)
@JsonSubTypes(
      JsonSubTypes.Type(value = ApprovedPremisesBedSearchParameters::class, name = "approved-premises"),
      JsonSubTypes.Type(value = TemporaryAccommodationBedSearchParameters::class, name = "temporary-accommodation")
)

interface BedSearchParameters{
                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val serviceName: kotlin.String

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "The date the Bed will need to be free from")
        val startDate: java.time.LocalDate

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "The number of days the Bed will need to be free from the start_date until")
        val durationDays: kotlin.Int


}

