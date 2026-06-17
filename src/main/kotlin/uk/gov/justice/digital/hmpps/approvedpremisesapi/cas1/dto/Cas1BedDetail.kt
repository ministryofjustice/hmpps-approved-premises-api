package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedStatus
import java.util.UUID

data class Cas1BedDetail(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("name", required = true) val name: String,

  @get:JsonProperty("roomName", required = true) val roomName: String,

  @get:JsonProperty("status", required = true) val status: BedStatus,

  @get:JsonProperty("characteristics", required = true) val characteristics: List<Cas1SpaceCharacteristic>,
)
