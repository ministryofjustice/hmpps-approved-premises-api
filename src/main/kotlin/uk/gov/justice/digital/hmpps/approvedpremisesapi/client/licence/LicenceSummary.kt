package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class LicenceSummary(
  val id: Long?,
  val kind: String?,
  val licenceType: LicenceType?,
  val policyVersion: String?,
  val version: String?,
  val statusCode: LicenceStatus?,
  val prisonNumber: String?,
  val bookingId: Long?,
  val crn: String?,
  val approvedByUsername: String?,
  val approvedDateTime: LocalDateTime?,
  val createdByUsername: String?,
  val createdDateTime: LocalDateTime?,
  val updatedByUsername: String?,
  val updatedDateTime: LocalDateTime?,
  val isInPssPeriod: Boolean?,
)
