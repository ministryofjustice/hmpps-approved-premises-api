package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator

/**
* 
* Values: submittedMinusApplications,applicationMinusStatusMinusUpdates,unsubmittedMinusApplications
*/
enum class Cas2v2ReportName(@get:JsonValue val value: String) {

    submittedMinusApplications("submitted-applications"),
    applicationMinusStatusMinusUpdates("application-status-updates"),
    unsubmittedMinusApplications("unsubmitted-applications");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): Cas2v2ReportName {
                return values().first{it -> it.value == value}
        }
    }
}

