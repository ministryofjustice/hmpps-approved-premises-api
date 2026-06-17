package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.util.UUID

data class Cas1NewOutOfServiceBed(

  @get:JsonProperty("startDate", required = true) val startDate: LocalDate,

  @get:JsonProperty("endDate", required = true) val endDate: LocalDate,

  @get:JsonProperty("reason", required = true) val reason: UUID,

  @get:JsonProperty("bedId", required = true) val bedId: UUID,

  @get:JsonProperty("referenceNumber") val referenceNumber: String? = null,

  @get:JsonProperty("notes") val notes: String? = null,
)
