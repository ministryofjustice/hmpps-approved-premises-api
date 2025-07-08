package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.generated

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator

/**
* 
* Values: submittedMinusApplications,applicationMinusStatusMinusUpdates,unsubmittedMinusApplications
*/
enum class Cas2ReportName(@get:JsonValue val value: String) {

    submittedMinusApplications("submitted-applications"),
    applicationMinusStatusMinusUpdates("application-status-updates"),
    unsubmittedMinusApplications("unsubmitted-applications");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): Cas2ReportName {
                return values().first{it -> it.value == value}
        }
    }
}

