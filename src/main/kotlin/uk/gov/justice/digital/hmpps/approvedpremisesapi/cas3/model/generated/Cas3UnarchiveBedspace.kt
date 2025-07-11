package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

/**
 *
 * @param restartDate Restart date for the bedspace after unarchiving.
 */
data class Cas3UnarchiveBedspace(

  @Schema(
    example = "Sat Mar 30 00:00:00 GMT 2024",
    required = true,
    description = "Restart date for the bedspace after unarchiving.",
  )
  @get:JsonProperty("restartDate", required = true) val restartDate: LocalDate,
)
