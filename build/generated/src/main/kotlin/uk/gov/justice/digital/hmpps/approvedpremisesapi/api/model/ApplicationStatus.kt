package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: inProgress,submitted,requestedFurtherInformation,pending,rejected,awaitingPlacement,placed,inapplicable,withdrawn
*/
enum class ApplicationStatus(val value: kotlin.String) {

    @JsonProperty("inProgress") inProgress("inProgress"),
    @JsonProperty("submitted") submitted("submitted"),
    @JsonProperty("requestedFurtherInformation") requestedFurtherInformation("requestedFurtherInformation"),
    @JsonProperty("pending") pending("pending"),
    @JsonProperty("rejected") rejected("rejected"),
    @JsonProperty("awaitingPlacement") awaitingPlacement("awaitingPlacement"),
    @JsonProperty("placed") placed("placed"),
    @JsonProperty("inapplicable") inapplicable("inapplicable"),
    @JsonProperty("withdrawn") withdrawn("withdrawn")
}

