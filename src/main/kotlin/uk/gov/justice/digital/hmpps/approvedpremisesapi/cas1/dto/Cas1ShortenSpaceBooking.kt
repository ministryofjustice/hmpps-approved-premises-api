package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class Cas1ShortenSpaceBooking(

  @Schema(example = "Fri Sep 30 01:00:00 BST 2022", required = true, description = "Updated departure date")
  @get:JsonProperty("departureDate", required = true) val departureDate: LocalDate,
)
