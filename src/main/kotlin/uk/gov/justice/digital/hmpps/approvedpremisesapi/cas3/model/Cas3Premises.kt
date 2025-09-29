package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonInclude
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import java.time.LocalDate
import java.util.UUID

data class Cas3Premises(
  val id: UUID,
  val reference: String,
  val addressLine1: String,
  val addressLine2: String? = null,
  val postcode: String,
  val town: String? = null,
  val localAuthorityArea: LocalAuthorityArea? = null,
  val probationRegion: ProbationRegion,
  val probationDeliveryUnit: ProbationDeliveryUnit,
  val status: Cas3PremisesStatus,
  val totalOnlineBedspaces: Int,
  val totalUpcomingBedspaces: Int,
  val totalArchivedBedspaces: Int,
  @Deprecated("Will be replaced with Cas3PremisesCharacteristics for v2")
  val characteristics: List<Characteristic>? = null,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  val premisesCharacteristics: List<Cas3PremisesCharacteristic>? = null,
  val startDate: LocalDate,
  val endDate: LocalDate? = null,
  val scheduleUnarchiveDate: LocalDate? = null,
  val notes: String? = null,
  val turnaroundWorkingDays: Int? = null,
  val archiveHistory: List<Cas3PremisesArchiveAction>? = emptyList(),
)
