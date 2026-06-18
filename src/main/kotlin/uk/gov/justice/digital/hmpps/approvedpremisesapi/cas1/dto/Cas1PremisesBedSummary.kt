package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class Cas1PremisesBedSummary(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("roomName", required = true) val roomName: String,

  @get:JsonProperty("bedName", required = true) val bedName: String,

  @get:JsonProperty("characteristics", required = true) val characteristics: List<Cas1SpaceCharacteristic>,
)
