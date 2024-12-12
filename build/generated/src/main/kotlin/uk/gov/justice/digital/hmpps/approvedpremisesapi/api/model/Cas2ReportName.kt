package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: submittedMinusApplications,applicationMinusStatusMinusUpdates,unsubmittedMinusApplications
*/
enum class Cas2ReportName(val value: kotlin.String) {

    @JsonProperty("submitted-applications") submittedMinusApplications("submitted-applications"),
    @JsonProperty("application-status-updates") applicationMinusStatusMinusUpdates("application-status-updates"),
    @JsonProperty("unsubmitted-applications") unsubmittedMinusApplications("unsubmitted-applications")
}

