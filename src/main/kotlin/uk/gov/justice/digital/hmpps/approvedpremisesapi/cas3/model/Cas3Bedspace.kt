package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceCharacteristic
import java.time.LocalDate
import java.util.UUID

data class Cas3Bedspace(
  val id: UUID,
  val reference: String,
  val startDate: LocalDate?,
  val status: Cas3BedspaceStatus,
  val endDate: LocalDate? = null,
  val notes: String? = null,
  val characteristics: List<Characteristic>? = null,
  val bedspaceCharacteristics: List<Cas3BedspaceCharacteristic>? = null,
  val archiveHistory: List<Cas3BedspaceArchiveAction> = emptyList(),
)
