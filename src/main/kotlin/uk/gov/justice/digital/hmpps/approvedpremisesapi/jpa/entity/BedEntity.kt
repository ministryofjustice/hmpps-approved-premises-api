package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Repository
interface BedRepository : JpaRepository<BedEntity, UUID>

@Entity
@Table(name = "beds")
data class BedEntity(
  @Id
  val id: UUID,
  val name: String,
  @ManyToOne
  @JoinColumn(name = "room_id")
  val room: RoomEntity,
)
