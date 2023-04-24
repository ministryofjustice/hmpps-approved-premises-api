package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.locationtech.jts.geom.Point
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import java.util.UUID
import javax.persistence.DiscriminatorColumn
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
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
  fun findAllByProbationRegion_Id(probationRegionId: UUID): List<PremisesEntity>

  @Query(
    """
        SELECT
          new uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesSummary(p.id, p.name, p.addressLine1, p.addressLine2, p.postcode, pdu.name, p.status, CAST(COUNT(b) as int))
        FROM
          TemporaryAccommodationPremisesEntity p
          LEFT JOIN p.rooms r 
          LEFT JOIN r.beds b
          LEFT JOIN p.probationRegion pr
          LEFT JOIN p.probationDeliveryUnit pdu
        WHERE 
          pr.id = :regionId
        GROUP BY p.id, p.name, p.addressLine1, p.addressLine2, p.postcode, pdu.name, p.status
      """
  )
  fun findAllTemporaryAccommodationSummary(regionId: UUID): List<TemporaryAccommodationPremisesSummary>

  @Query("SELECT new uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesSummary(p.id, p.name, p.addressLine1, p.addressLine2, p.postcode, p.status, CAST(COUNT(b) as int), p.apCode) FROM ApprovedPremisesEntity p LEFT JOIN p.rooms r LEFT JOIN r.beds b GROUP BY p.id, p.name, p.addressLine1, p.addressLine2, p.postcode, p.apCode, p.status")
  fun findAllApprovedPremisesSummary(): List<ApprovedPremisesSummary>

  @Query("SELECT p FROM PremisesEntity p WHERE TYPE(p) = :type")
  fun <T : PremisesEntity> findAllByType(type: Class<T>): List<PremisesEntity>

  @Query("SELECT p FROM PremisesEntity p WHERE p.probationRegion.id = :probationRegionId AND TYPE(p) = :type")
  fun <T : PremisesEntity> findAllByProbationRegion_IdAndType(probationRegionId: UUID, type: Class<T>): List<PremisesEntity>

  @Query("SELECT COUNT(p) = 0 FROM PremisesEntity p WHERE name = :name AND TYPE(p) = :type")
  fun <T : PremisesEntity> nameIsUniqueForType(name: String, type: Class<T>): Boolean

  @Query("SELECT p FROM PremisesEntity p WHERE name = :name AND TYPE(p) = :type")
  fun <T : PremisesEntity> findByName(name: String, type: Class<T>): PremisesEntity?

  @Query("SELECT p FROM PremisesEntity p WHERE ap_code = :apCode AND TYPE(p) = :type")
  fun <T : PremisesEntity> findByApCode(apCode: String, type: Class<T>): PremisesEntity?
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
  var addressLine2: String?,
  var town: String?,
  var postcode: String,
  var longitude: Double?,
  var latitude: Double?,
  var totalBeds: Int,
  var notes: String,
  @ManyToOne
  @JoinColumn(name = "probation_region_id")
  var probationRegion: ProbationRegionEntity,
  @ManyToOne
  @JoinColumn(name = "local_authority_area_id")
  var localAuthorityArea: LocalAuthorityAreaEntity?,
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
  var characteristics: MutableList<CharacteristicEntity>,
  @Enumerated(value = EnumType.STRING)
  var status: PropertyStatus
)

@Entity
@DiscriminatorValue("approved-premises")
@Table(name = "approved_premises")
@PrimaryKeyJoinColumn(name = "premises_id")
class ApprovedPremisesEntity(
  id: UUID,
  name: String,
  addressLine1: String,
  addressLine2: String?,
  town: String?,
  postcode: String,
  longitude: Double?,
  latitude: Double?,
  totalBeds: Int,
  notes: String,
  probationRegion: ProbationRegionEntity,
  localAuthorityArea: LocalAuthorityAreaEntity,
  bookings: MutableList<BookingEntity>,
  lostBeds: MutableList<LostBedsEntity>,
  var apCode: String,
  var qCode: String,
  rooms: MutableList<RoomEntity>,
  characteristics: MutableList<CharacteristicEntity>,
  status: PropertyStatus,
  val point: Point? // TODO: Make not-null once Premises have had point added in all environments
) : PremisesEntity(
  id,
  name,
  addressLine1,
  addressLine2,
  town,
  postcode,
  longitude,
  latitude,
  totalBeds,
  notes,
  probationRegion,
  localAuthorityArea,
  bookings,
  lostBeds,
  rooms,
  characteristics,
  status
)

@Entity
@DiscriminatorValue("temporary-accommodation")
@Table(name = "temporary_accommodation_premises")
@PrimaryKeyJoinColumn(name = "premises_id")
class TemporaryAccommodationPremisesEntity(
  id: UUID,
  name: String,
  addressLine1: String,
  addressLine2: String?,
  town: String?,
  postcode: String,
  longitude: Double?,
  latitude: Double?,
  totalBeds: Int,
  notes: String,
  probationRegion: ProbationRegionEntity,
  localAuthorityArea: LocalAuthorityAreaEntity?,
  bookings: MutableList<BookingEntity>,
  lostBeds: MutableList<LostBedsEntity>,
  rooms: MutableList<RoomEntity>,
  characteristics: MutableList<CharacteristicEntity>,
  status: PropertyStatus,
  @ManyToOne
  @JoinColumn(name = "probation_delivery_unit_id")
  var probationDeliveryUnit: ProbationDeliveryUnitEntity?,
) : PremisesEntity(
  id,
  name,
  addressLine1,
  addressLine2,
  town,
  postcode,
  longitude,
  latitude,
  totalBeds,
  notes,
  probationRegion,
  localAuthorityArea,
  bookings,
  lostBeds,
  rooms,
  characteristics,
  status
)

data class ApprovedPremisesSummary(
  val id: UUID,
  val name: String,
  val addressLine1: String,
  val addressLine2: String?,
  val postcode: String,
  val status: PropertyStatus,
  val bedCount: Int,
  val apCode: String
)

data class TemporaryAccommodationPremisesSummary(
  val id: UUID,
  val name: String,
  val addressLine1: String,
  val addressLine2: String?,
  val postcode: String,
  val pdu: String,
  val status: PropertyStatus,
  val bedCount: Int,
)
