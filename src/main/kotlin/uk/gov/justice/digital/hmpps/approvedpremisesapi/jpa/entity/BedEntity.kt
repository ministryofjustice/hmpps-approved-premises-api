package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Repository
interface BedRepository : JpaRepository<BedEntity, UUID> {
  fun findByCode(bedCode: String): BedEntity?
}

@Entity
@Table(name = "beds")
data class BedEntity(
  @Id
  val id: UUID,
  val name: String,
  val code: String?,
  @ManyToOne
  @JoinColumn(name = "room_id")
  val room: RoomEntity,
) {
  override fun toString() = "BedEntity:$id"
}
