package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import java.util.UUID

data class Cas3PremisesSummary(

  val id: UUID,

  val name: String,

  val addressLine1: String,

  val postcode: String,

  val pdu: String,

  val bedspaceCount: Int,

  val status: PropertyStatus,

  val addressLine2: String? = null,

  val town: String? = null,

  val localAuthorityAreaName: String? = null,
)
