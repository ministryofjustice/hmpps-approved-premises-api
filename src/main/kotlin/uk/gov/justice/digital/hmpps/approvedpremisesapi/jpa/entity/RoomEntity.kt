package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

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
import java.util.UUID

@Repository
interface RoomRepository : JpaRepository<RoomEntity, UUID> {
  fun findByCode(roomCode: String): RoomEntity?

  @Query("SELECT COUNT(r) = 0 FROM RoomEntity r WHERE r.name = :name AND r.premises.id = :premisesId")
  fun nameIsUniqueForPremises(name: String, premisesId: UUID): Boolean

  @Modifying
  @Query("UPDATE RoomEntity r SET r.code = :code WHERE r.id = :id")
  fun updateCode(id: UUID, code: String)
}

@Entity
@Table(name = "rooms")
data class RoomEntity(
  @Id
  val id: UUID,
  var name: String,
  val code: String?,
  var notes: String?,
  @ManyToOne
  @JoinColumn(name = "premises_id")
  val premises: PremisesEntity,
  @OneToMany(mappedBy = "room")
  val beds: MutableList<BedEntity>,
  @ManyToMany
  @JoinTable(
    name = "room_characteristics",
    joinColumns = [JoinColumn(name = "room_id")],
    inverseJoinColumns = [JoinColumn(name = "characteristic_id")],
  )
  var characteristics: MutableList<CharacteristicEntity>,
) {

  override fun toString() = "RoomEntity: $id"
}
