package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: normal,pipe,esap,rfap,mhapStJosephs,mhapElliottHouse
*/
enum class ApType(val value: kotlin.String) {

    @JsonProperty("normal") normal("normal"),
    @JsonProperty("pipe") pipe("pipe"),
    @JsonProperty("esap") esap("esap"),
    @JsonProperty("rfap") rfap("rfap"),
    @JsonProperty("mhapStJosephs") mhapStJosephs("mhapStJosephs"),
    @JsonProperty("mhapElliottHouse") mhapElliottHouse("mhapElliottHouse")
}

