package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

/**
 *
 * @param includedCharacteristicIds
 * @param excludedCharacteristicIds
 */
data class PremisesFilters(

  @get:JsonProperty("includedCharacteristicIds") val includedCharacteristicIds: List<UUID>? = null,

  @get:JsonProperty("excludedCharacteristicIds") val excludedCharacteristicIds: List<UUID>? = null,
)
