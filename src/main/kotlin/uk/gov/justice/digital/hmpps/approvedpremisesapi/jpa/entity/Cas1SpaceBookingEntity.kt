package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

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
  @JoinColumn(name = "placement_request_id")
  val placementRequest: PlacementRequestEntity,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_user_id")
  val createdBy: UserEntity,
  val createdAt: OffsetDateTime,
  val arrivalDate: LocalDate,
  val departureDate: LocalDate,
)
