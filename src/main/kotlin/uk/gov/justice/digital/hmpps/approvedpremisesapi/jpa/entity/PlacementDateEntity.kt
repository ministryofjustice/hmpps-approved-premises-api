package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface PlacementDateRepository : JpaRepository<PlacementDateEntity, UUID> {

  @Query("SELECT p FROM PlacementDateEntity p WHERE p.placementApplication = :placementApplication")
  fun findAllByPlacementApplication(placementApplication: PlacementApplicationEntity): List<PlacementDateEntity>

  @Query(
    """select pad.placement_application_id 
       from placement_application_dates pad 
       inner join placement_requests pr on pr.id = pad.placement_request_id 
       where pr.reallocated_at is not null""",
    nativeQuery = true,
  )
  fun findPlacementAppIdsWithDatesLinkedToReallocatedPlacementRequest(): List<UUID>
}

@Deprecated("This is being replaced by date fields on the [PlacementApplicationEntity]")
@Entity
@Table(name = "placement_application_dates")
data class PlacementDateEntity(
  @Id
  val id: UUID,

  val createdAt: OffsetDateTime,

  @OneToOne
  @JoinColumn(name = "placement_application_id")
  var placementApplication: PlacementApplicationEntity,

  @OneToOne
  @JoinColumn(name = "placement_request_id")
  var placementRequest: PlacementRequestEntity? = null,

  val expectedArrival: LocalDate,

  val duration: Int,
) {
  fun expectedDeparture() = expectedArrival.plusDays(duration.toLong())
  override fun toString() = "PlacementDateEntity: $id"
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PlacementDateEntity

    if (id != other.id) return false
    if (createdAt != other.createdAt) return false
    if (placementApplication.id != other.placementApplication.id) return false
    if (placementRequest != other.placementRequest) return false
    if (expectedArrival != other.expectedArrival) return false
    if (duration != other.duration) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + createdAt.hashCode()
    result = 31 * result + placementApplication.id.hashCode()
    result = 31 * result + (placementRequest?.hashCode() ?: 0)
    result = 31 * result + expectedArrival.hashCode()
    result = 31 * result + duration
    return result
  }
}
