package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Bed(

  val id: java.util.UUID,

  val name: kotlin.String,

  @Schema(example = "NEABC04", description = "")
  val code: kotlin.String? = null,

  @Schema(example = "Sat Mar 30 00:00:00 GMT 2024", description = "End date of the bed availability, open for availability if not specified")
  val bedEndDate: java.time.LocalDate? = null,
)
