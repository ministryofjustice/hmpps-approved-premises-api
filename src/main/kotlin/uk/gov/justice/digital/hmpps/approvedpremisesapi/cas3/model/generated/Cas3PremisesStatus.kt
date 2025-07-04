package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@SuppressWarnings("EnumNaming", "ExplicitItLambdaParameter")
enum class Cas3PremisesStatus(@get:JsonValue val value: String) {

    online("online"),
    archived("archived");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): Cas3PremisesStatus {
            return values().first { it -> it.value == value }
        }
    }
}

