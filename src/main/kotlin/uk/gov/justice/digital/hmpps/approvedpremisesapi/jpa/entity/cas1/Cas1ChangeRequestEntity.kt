package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas1ChangeRequestRepository : JpaRepository<Cas1ChangeRequestEntity, UUID>

@Entity
@Table(name = "cas1_change_requests")
data class Cas1ChangeRequestEntity(
  @Id
  val id: UUID,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cas1_space_booking_id")
  var spaceBookings: Cas1SpaceBookingEntity,
  @Enumerated(EnumType.STRING)
  val type: ChangeRequestType,
  val requesterNotes: String?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_user_id")
  val createdByUser: UserEntity,
  @Enumerated(EnumType.STRING)
  val resolution: ChangeRequestResolution,
  val createdAt: OffsetDateTime,
  var updatedAt: OffsetDateTime,
  var resolvedAt: OffsetDateTime?,
)

enum class ChangeRequestResolution {
  APPROVED,
  REJECTED,
}

enum class ChangeRequestType {
  APPEAL,
  EXTENSION,
  PLANNED_TRANSFER,
}
