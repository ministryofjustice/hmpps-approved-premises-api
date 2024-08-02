package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas1SpaceBookingRepository : JpaRepository<Cas1SpaceBookingEntity, UUID> {
  fun findByPremisesIdAndPlacementRequestId(premisesId: UUID, placementRequestId: UUID): Cas1SpaceBookingEntity?
}

@Entity
@Table(name = "cas1_space_bookings")
data class Cas1SpaceBookingEntity(
  @Id
  val id: UUID,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "premises_id")
  val premises: ApprovedPremisesEntity,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_premises_application_id")
  val application: ApprovedPremisesApplicationEntity?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_request_id")
  val placementRequest: PlacementRequestEntity,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_user_id")
  val createdBy: UserEntity,
  val createdAt: OffsetDateTime,
  val expectedArrivalDate: LocalDate,
  val expectedDepartureDate: LocalDate,
  val actualArrivalDateTime: Instant?,
  val actualDepartureDateTime: Instant?,
  val canonicalArrivalDate: LocalDate,
  val canonicalDepartureDate: LocalDate,
  val crn: String,
  val keyWorkerStaffCode: String?,
  val keyWorkerName: String?,
  val keyWorkerAssignedAt: Instant?,
)
