package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class Cas1OutOfServiceBedReason(
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  val referenceType: Cas1OutOfServiceBedReasonReferenceType,
)

enum class Cas1OutOfServiceBedReasonReferenceType(@get:JsonValue val value: String) {
  CRN("crn"),
  WORK_ORDER("workOrder"),
}
