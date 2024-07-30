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
interface PlacementApplicationAutomaticRepository : JpaRepository<PlacementApplicationAutomaticEntity, UUID>

@Entity
@Table(name = "placement_applications_automatic")
data class PlacementApplicationAutomaticEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: ApplicationEntity,

  val submittedAt: OffsetDateTime,
  val expectedArrivalDate: OffsetDateTime,
)
