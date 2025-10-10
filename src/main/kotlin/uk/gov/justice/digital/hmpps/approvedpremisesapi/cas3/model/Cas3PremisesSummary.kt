package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonInclude
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import java.util.UUID

data class Cas3PremisesSummary(
  val id: UUID,
  val name: String,
  val addressLine1: String,
  val postcode: String,
  val pdu: String,
  val bedspaceCount: Int,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  val status: PropertyStatus? = null,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  val premisesStatus: Cas3PremisesStatus? = null,
  val addressLine2: String? = null,
  val town: String? = null,
  val localAuthorityAreaName: String? = null,
)
