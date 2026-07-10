package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1NewApplication(

    @get:JsonProperty("crn", required = true)
    val crn: String,

    @Schema(example = "1502724704", required = true)
    @get:JsonProperty("convictionId")
    val convictionId: Long,

    @Schema(example = "7", required = true)
    @get:JsonProperty("deliusEventNumber")
    val deliusEventNumber: String,

    @Schema(example = "M1502750438", required = true)
    @get:JsonProperty("offenceId")
    val offenceId: String,
)
