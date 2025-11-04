package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import java.util.UUID

/**
 *
 * @param includedCharacteristicIds
 * @param excludedCharacteristicIds
 */
data class BedspaceFilters(

  val includedCharacteristicIds: List<UUID>? = null,

  val excludedCharacteristicIds: List<UUID>? = null,
)
