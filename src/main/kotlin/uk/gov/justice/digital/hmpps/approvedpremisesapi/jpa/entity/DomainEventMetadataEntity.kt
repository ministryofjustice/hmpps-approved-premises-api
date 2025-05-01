package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "domain_events_metadata")
data class DomainEventMetadataEntity(
  @Id
  val id: UUID,
  @Enumerated(EnumType.STRING)
  val name: MetaDataName,
  val value: String?,
)

enum class MetaDataName {
  CAS1_APP_REASON_FOR_SHORT_NOTICE,
  CAS1_APP_REASON_FOR_SHORT_NOTICE_OTHER,
  CAS1_PLACEMENT_APPLICATION_ID,
  CAS1_REQUESTED_AP_TYPE,
  CAS1_PLACEMENT_REQUEST_ID,
  CAS1_CANCELLATION_ID,
  CAS1_SPACE_BOOKING_ID,
}
