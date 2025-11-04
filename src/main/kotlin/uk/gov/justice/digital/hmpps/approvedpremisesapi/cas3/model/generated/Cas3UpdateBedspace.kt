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

  val reference: String,

  val characteristicIds: List<UUID>,

  val notes: String? = null,
)
