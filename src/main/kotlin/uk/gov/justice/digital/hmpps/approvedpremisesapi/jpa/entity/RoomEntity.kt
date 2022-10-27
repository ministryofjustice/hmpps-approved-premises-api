package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Repository
interface RoomRepository : JpaRepository<RoomEntity, UUID>

@Entity
@Table(name = "rooms")
data class RoomEntity(
  @Id
  val id: UUID,
  val name: String,
  val notes: String?,
  @ManyToOne
  @JoinColumn(name = "premises_id")
  val premises: PremisesEntity,
  @OneToMany(mappedBy = "room")
  val beds: MutableList<BedEntity>,
)
