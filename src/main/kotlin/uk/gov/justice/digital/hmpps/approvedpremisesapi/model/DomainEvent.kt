package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import java.time.Instant
import java.util.UUID

data class DomainEvent<T>(
  val id: UUID,
  val applicationId: UUID? = null,
  val assessmentId: UUID? = null,
  val bookingId: UUID? = null,
  val cas1SpaceBookingId: UUID? = null,
  val cas1PlacementRequestId: UUID? = null,
  val crn: String,
  val nomsNumber: String?,
  val occurredAt: Instant,
  val data: T,
  val metadata: Map<MetaDataName, String?> = emptyMap(),
  val schemaVersion: Int? = null,
  val triggerSource: TriggerSourceType? = null,
  val emit: Boolean = true,
)
