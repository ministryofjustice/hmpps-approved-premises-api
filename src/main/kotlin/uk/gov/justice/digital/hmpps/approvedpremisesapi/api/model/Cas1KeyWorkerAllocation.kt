package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param keyWorker
 * @param allocatedAt
 */
data class Cas1KeyWorkerAllocation(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("keyWorker", required = true) val keyWorker: StaffMember,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("allocatedAt", required = true) val allocatedAt: java.time.LocalDate,
)
