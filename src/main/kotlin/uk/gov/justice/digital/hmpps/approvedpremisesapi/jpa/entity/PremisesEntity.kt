package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.DiscriminatorColumn
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Inheritance
import javax.persistence.InheritanceType
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.PrimaryKeyJoinColumn
import javax.persistence.Table

@Repository
interface PremisesRepository : JpaRepository<PremisesEntity, UUID> {
  @Query("SELECT p FROM PremisesEntity p WHERE TYPE(p) = :type")
  fun <T : PremisesEntity> findAllByType(type: Class<T>): List<PremisesEntity>

  @Query("SELECT COUNT(p) = 0 FROM PremisesEntity p WHERE name = :name AND TYPE(p) = :type")
  fun <T : PremisesEntity> nameIsUniqueForType(name: String, type: Class<T>): Boolean
}

@Entity
@Table(name = "premises")
@DiscriminatorColumn(name = "service")
@Inheritance(strategy = InheritanceType.JOINED)
abstract class PremisesEntity(
  @Id
  val id: UUID,
  var name: String,
  var addressLine1: String,
  var postcode: String,
  var totalBeds: Int,
  var notes: String,
  @ManyToOne
  @JoinColumn(name = "probation_region_id")
  val probationRegion: ProbationRegionEntity,
  @ManyToOne
  @JoinColumn(name = "local_authority_area_id")
  val localAuthorityArea: LocalAuthorityAreaEntity,
  @OneToMany(mappedBy = "premises")
  val bookings: MutableList<BookingEntity>,
  @OneToMany(mappedBy = "premises")
  val lostBeds: MutableList<LostBedsEntity>,
  @OneToMany(mappedBy = "premises")
  val rooms: MutableList<RoomEntity>,
  @ManyToMany
  @JoinTable(
    name = "premises_characteristics",
    joinColumns = [JoinColumn(name = "premises_id")],
    inverseJoinColumns = [JoinColumn(name = "characteristic_id")],
  )
  val characteristics: MutableList<CharacteristicEntity>,
)

@Entity
@DiscriminatorValue("approved-premises")
@Table(name = "approved_premises")
@PrimaryKeyJoinColumn(name = "premises_id")
class ApprovedPremisesEntity(
  id: UUID,
  name: String,
  addressLine1: String,
  postcode: String,
  totalBeds: Int,
  notes: String,
  probationRegion: ProbationRegionEntity,
  localAuthorityArea: LocalAuthorityAreaEntity,
  bookings: MutableList<BookingEntity>,
  lostBeds: MutableList<LostBedsEntity>,
  val apCode: String,
  val deliusTeamCode: String,
  val qCode: String,
  rooms: MutableList<RoomEntity>,
  characteristics: MutableList<CharacteristicEntity>,
) : PremisesEntity(
  id,
  name,
  addressLine1,
  postcode,
  totalBeds,
  notes,
  probationRegion,
  localAuthorityArea,
  bookings,
  lostBeds,
  rooms,
  characteristics
)

@Entity
@DiscriminatorValue("temporary-accommodation")
@Table(name = "temporary_accommodation_premises")
@PrimaryKeyJoinColumn(name = "premises_id")
class TemporaryAccommodationPremisesEntity(
  id: UUID,
  name: String,
  addressLine1: String,
  postcode: String,
  totalBeds: Int,
  notes: String,
  probationRegion: ProbationRegionEntity,
  localAuthorityArea: LocalAuthorityAreaEntity,
  bookings: MutableList<BookingEntity>,
  lostBeds: MutableList<LostBedsEntity>,
  rooms: MutableList<RoomEntity>,
  characteristics: MutableList<CharacteristicEntity>,
) : PremisesEntity(
  id,
  name,
  addressLine1,
  postcode,
  totalBeds,
  notes,
  probationRegion,
  localAuthorityArea,
  bookings,
  lostBeds,
  rooms,
  characteristics
)
