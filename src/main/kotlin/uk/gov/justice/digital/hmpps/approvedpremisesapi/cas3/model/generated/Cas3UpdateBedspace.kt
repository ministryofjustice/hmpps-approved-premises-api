package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

/**
 *
 * @param reference
 * @param characteristicIds
 * @param notes
 */
data class Cas3UpdateBedspace(

  @get:JsonProperty("reference", required = true) val reference: String,

  @get:JsonProperty("characteristicIds", required = true) val characteristicIds: List<UUID>,

  @get:JsonProperty("notes") val notes: String? = null,
)
