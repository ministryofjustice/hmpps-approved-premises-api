package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Repository
interface BookingNotMadeRepository : JpaRepository<BookingNotMadeEntity, UUID>

@Entity
@Table(name = "booking_not_mades")
data class BookingNotMadeEntity(
  @Id
  val id: UUID,
  @ManyToOne
  @JoinColumn(name = "placement_request_id")
  val placementRequest: PlacementRequestEntity,
  val createdAt: OffsetDateTime,
  val notes: String?,
) {

  override fun toString() = "BookingNotMadeEntity: $id"
}
