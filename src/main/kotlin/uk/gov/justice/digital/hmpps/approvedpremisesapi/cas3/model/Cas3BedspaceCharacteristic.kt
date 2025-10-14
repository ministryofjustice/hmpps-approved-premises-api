package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import java.util.UUID

data class Cas3BedspaceCharacteristic(
  val id: UUID,
  val description: String,
  val name: String? = null,
)
