package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

/**
 *
 * @param includedCharacteristicIds
 * @param excludedCharacteristicIds
 */
data class PremisesFilters(

  val includedCharacteristicIds: List<UUID>? = null,

  val excludedCharacteristicIds: List<UUID>? = null,
)
