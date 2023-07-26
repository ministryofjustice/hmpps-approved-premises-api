package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

@Repository
interface PlacementDateRepository : JpaRepository<PlacementDateEntity, UUID> {

  @Query("SELECT p FROM PlacementDateEntity p WHERE p.placementApplication = :placementApplication")
  fun findAllByPlacementApplication(placementApplication: PlacementApplicationEntity): List<PlacementDateEntity>
}

@Entity
@Table(name = "placement_application_dates")
data class PlacementDateEntity(
  @Id
  val id: UUID,

  val createdAt: OffsetDateTime,

  @OneToOne
  @JoinColumn(name = "placement_application_id")
  val placementApplication: PlacementApplicationEntity,

  val expectedArrival: LocalDate,

  val duration: Int,
)
