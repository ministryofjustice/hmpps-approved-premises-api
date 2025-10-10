package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import java.util.UUID

data class Cas3PremisesSummary(
  override val id: UUID,
  override val name: String,
  override val addressLine1: String,
  override val postcode: String,
  override val pdu: String,
  override val bedspaceCount: Int,
  val status: PropertyStatus? = null,
  override val addressLine2: String? = null,
  override val town: String? = null,
  override val localAuthorityAreaName: String? = null,
) : Cas3PremisesSummaryMain

data class Cas3PremisesSummaryV2(
  override val id: UUID,
  override val name: String,
  override val addressLine1: String,
  override val postcode: String,
  override val pdu: String,
  override val bedspaceCount: Int,
  val status: Cas3PremisesStatus? = null,
  override val addressLine2: String? = null,
  override val town: String? = null,
  override val localAuthorityAreaName: String? = null,
) : Cas3PremisesSummaryMain

interface Cas3PremisesSummaryMain {
  val id: UUID
  val name: String
  val addressLine1: String
  val postcode: String
  val pdu: String
  val bedspaceCount: Int
  val addressLine2: String?
  val town: String?
  val localAuthorityAreaName: String?
}
