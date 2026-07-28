package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import java.util.UUID

@Repository
interface Cas1RoomRepository : JpaRepository<Cas1RoomEntity, UUID> {
  fun findByCode(roomCode: String): Cas1RoomEntity?

  @Modifying
  @Query("UPDATE Cas1RoomEntity r SET r.code = :code WHERE r.id = :id")
  fun updateCode(id: UUID, code: String)
}

@Entity
@Table(name = "cas1_rooms")
data class Cas1RoomEntity(
  @Id
  val id: UUID,
  var name: String,
  val code: String?,
  var notes: String?,
  @ManyToOne
  @JoinColumn(name = "premises_id")
  val premises: Cas1PremisesBaseEntity,
  @OneToMany(mappedBy = "room")
  val beds: MutableList<Cas1BedEntity>,
  @ManyToMany
  @JoinTable(
    name = "room_characteristics",
    joinColumns = [JoinColumn(name = "room_id")],
    inverseJoinColumns = [JoinColumn(name = "characteristic_id")],
  )
  var characteristics: MutableList<CharacteristicEntity>,
) {

  override fun toString() = "Cas1RoomEntity: $id"
}
