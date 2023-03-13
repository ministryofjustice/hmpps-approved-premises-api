package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.QueryHint
import javax.persistence.Table
import javax.persistence.Transient

@Repository
interface RoomRepository : JpaRepository<RoomEntity, UUID> {
  @Query("SELECT DISTINCT r FROM RoomEntity r LEFT JOIN FETCH r.beds WHERE r IN (:rooms)")
  @QueryHints(value = [QueryHint(name = org.hibernate.jpa.QueryHints.HINT_PASS_DISTINCT_THROUGH, value = "false")], forCounting = false)
  fun loadRoomsBeds(rooms: List<RoomEntity>): List<RoomEntity>
}

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
  @ManyToMany
  @JoinTable(
    name = "room_characteristics",
    joinColumns = [JoinColumn(name = "room_id")],
    inverseJoinColumns = [JoinColumn(name = "characteristic_id")],
  )
  val characteristics: MutableList<CharacteristicEntity>,
) {
  @Transient
  var bedsLoaded = false
}
