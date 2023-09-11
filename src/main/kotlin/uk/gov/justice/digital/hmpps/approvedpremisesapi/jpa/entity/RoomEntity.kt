package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Repository
interface RoomRepository : JpaRepository<RoomEntity, UUID> {
  fun findByCode(roomCode: String): RoomEntity?

  @Query("SELECT COUNT(r) = 0 FROM RoomEntity r WHERE r.name = :name AND r.premises.id = :premisesId")
  fun nameIsUniqueForPremises(name: String, premisesId: UUID): Boolean
}

@Entity
@Table(name = "rooms")
data class RoomEntity(
  @Id
  val id: UUID,
  val name: String,
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
  val characteristics: MutableList<CharacteristicEntity>,
) {

  override fun toString() = "RoomEntity: $id"
}
