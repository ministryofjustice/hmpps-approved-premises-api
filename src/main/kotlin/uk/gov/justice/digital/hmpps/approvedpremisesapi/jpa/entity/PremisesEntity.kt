package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.locationtech.jts.geom.Point
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity.Companion.resolveFullAddress
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface PremisesRepository : JpaRepository<PremisesEntity, UUID> {
  @Query("SELECT p FROM PremisesEntity p WHERE p.name = :name AND TYPE(p) = :type")
  fun <T : PremisesEntity> findByName(name: String, type: Class<T>): PremisesEntity?

  @Query("SELECT p FROM ApprovedPremisesEntity p WHERE p.apCode = :apCode")
  fun findByApCode(apCode: String): ApprovedPremisesEntity?

  @Query("SELECT CAST(COUNT(b) as int) FROM PremisesEntity p JOIN p.rooms r JOIN r.beds b on (b.endDate IS NULL OR b.endDate >= CURRENT_DATE) WHERE r.premises = :premises")
  fun getBedCount(premises: PremisesEntity): Int
}

@Repository
class ApprovedPremisesJdbcRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) {
  fun findAllCharacteristicPropertyNames(
    premisesId: UUID,
  ): List<String> {
    val params = mutableMapOf("premisesId" to premisesId)

    val result = jdbcTemplate.query(
      """
      SELECT 
        ARRAY_REMOVE(ARRAY_AGG (DISTINCT premises_chars_resolved.property_name), null) as premises_characteristics,
        ARRAY_REMOVE(ARRAY_AGG (DISTINCT room_chars_resolved.property_name), null) as room_characteristics
      FROM approved_premises ap
      LEFT OUTER JOIN rooms ON rooms.premises_id = ap.premises_id
      LEFT OUTER JOIN premises_characteristics premises_chars ON premises_chars.premises_id = ap.premises_id
      LEFT OUTER JOIN characteristics premises_chars_resolved ON premises_chars_resolved.id = premises_chars.characteristic_id
      LEFT OUTER JOIN room_characteristics room_chars ON room_chars.room_id = rooms.id
      LEFT OUTER JOIN characteristics room_chars_resolved ON room_chars_resolved.id = room_chars.characteristic_id
      WHERE 
      ap.premises_id = :premisesId
      GROUP BY ap.premises_id
      """.trimIndent(),
      params,
    ) { rs, _ ->
      SqlUtil.toStringList(rs.getArray("premises_characteristics")) +
        SqlUtil.toStringList(rs.getArray("room_characteristics"))
    }

    return result.firstOrNull() ?: emptyList()
  }
}

@Repository
interface ApprovedPremisesRepository : JpaRepository<ApprovedPremisesEntity, UUID> {
  @Query(
    """
        SELECT
          new uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesBasicSummary(
              p.id,
              p.name,
              p.apCode,
              apArea.id,
              apArea.name,
              CAST(COUNT(b) as int),
              p.supportsSpaceBookings,
              p.fullAddress,
              p.addressLine1,
              p.addressLine2,
              p.town,
              p.postcode
              )
        FROM 
          ApprovedPremisesEntity p
          LEFT JOIN p.rooms r 
          LEFT JOIN r.beds b on (b.endDate IS NULL OR b.endDate > CURRENT_DATE) 
          LEFT JOIN p.probationRegion region
          LEFT JOIN region.apArea apArea
          LEFT JOIN p.cruManagementArea cruManagementArea
        WHERE 
          status != 'archived' AND 
          (:gender IS NULL OR p.gender = :gender)
          AND(cast(:apAreaId as text) IS NULL OR apArea.id = :apAreaId) 
          AND(cast(:cruManagementAreaId as text) IS NULL OR cruManagementArea.id = :cruManagementAreaId)
          GROUP BY p.id, p.name, p.apCode, apArea.id, apArea.name, p.fullAddress, p.addressLine1, p.addressLine2, p.town, p.postcode
      """,
  )
  fun findForSummaries(gender: ApprovedPremisesGender?, apAreaId: UUID?, cruManagementAreaId: UUID?): List<ApprovedPremisesBasicSummary>

  fun findByQCode(qcode: String): ApprovedPremisesEntity?

  @Query("SELECT id FROM ApprovedPremisesEntity")
  fun findAllIds(): List<UUID>

  fun findByName(name: String): ApprovedPremisesEntity
}

@SuppressWarnings("LongParameterList")
@Entity
@Table(name = "premises")
@DiscriminatorColumn(name = "service")
@Inheritance(strategy = InheritanceType.JOINED)
abstract class PremisesEntity(
  @Id
  val id: UUID,
  var name: String,
  /**
   * For CAS1 fullAddress should be used instead, where defined
   */
  var addressLine1: String,
  /**
   * For CAS1 fullAddress should be used instead, where defined
   */
  var addressLine2: String?,
  /**
   * For CAS1 fullAddress should be used instead, where defined
   */
  var town: String?,
  var postcode: String,
  var longitude: Double?,
  var latitude: Double?,
  var notes: String,
  var emailAddress: String?,
  @ManyToOne
  @JoinColumn(name = "probation_region_id")
  var probationRegion: ProbationRegionEntity,
  @ManyToOne
  @JoinColumn(name = "local_authority_area_id")
  var localAuthorityArea: LocalAuthorityAreaEntity?,
  @OneToMany(mappedBy = "premises")
  val bookings: MutableList<BookingEntity>,
  @OneToMany(mappedBy = "premises")
  val voidBedspaces: MutableList<Cas3VoidBedspaceEntity>,
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
  var status: PropertyStatus,
  @CreationTimestamp
  var createdAt: OffsetDateTime = OffsetDateTime.now(),
)

@SuppressWarnings("LongParameterList")
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
  notes: String,
  emailAddress: String?,
  probationRegion: ProbationRegionEntity,
  localAuthorityArea: LocalAuthorityAreaEntity,
  bookings: MutableList<BookingEntity>,
  lostBeds: MutableList<Cas3VoidBedspaceEntity>,
  var apCode: String,
  var qCode: String,
  rooms: MutableList<RoomEntity>,
  characteristics: MutableList<CharacteristicEntity>,
  status: PropertyStatus,
  // TODO: Make not-null once Premises have had point added in all environments
  var point: Point?,
  @Enumerated(value = EnumType.STRING)
  var gender: ApprovedPremisesGender,
  var supportsSpaceBookings: Boolean,
  var managerDetails: String?,
  /**
   * Full Address, excluding postcode. When defined this should be used instead of [addressLine1], [addressLine2] and [town]
   *
   * Whilst currently nullable, once all site surveys have been imported this can be made non-null
   *
   * It's recommended that [resolveFullAddress] is used in the meantime when address is required
   */
  var fullAddress: String?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cas1_cru_management_area_id")
  var cruManagementArea: Cas1CruManagementAreaEntity,
  var allowNewSpaceBookings: Boolean,
) : PremisesEntity(
  id,
  name,
  addressLine1,
  addressLine2,
  town,
  postcode,
  longitude,
  latitude,
  notes,
  emailAddress,
  probationRegion,
  localAuthorityArea,
  bookings,
  lostBeds,
  rooms,
  characteristics,
  status,
) {

  fun resolveFullAddress() = resolveFullAddress(
    fullAddress = fullAddress,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    town = town,
  )

  companion object {
    fun resolveFullAddress(
      fullAddress: String?,
      addressLine1: String,
      addressLine2: String?,
      town: String?,
    ) = fullAddress
      ?: listOf(addressLine1, addressLine2, town)
        .filter { !it.isNullOrBlank() }
        .joinToString(separator = ", ")
  }
}

enum class ApprovedPremisesGender {
  MAN,
  WOMAN,
}

@SuppressWarnings("LongParameterList")
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
  notes: String,
  emailAddress: String?,
  probationRegion: ProbationRegionEntity,
  localAuthorityArea: LocalAuthorityAreaEntity?,
  bookings: MutableList<BookingEntity>,
  lostBeds: MutableList<Cas3VoidBedspaceEntity>,
  rooms: MutableList<RoomEntity>,
  characteristics: MutableList<CharacteristicEntity>,
  status: PropertyStatus,
  var startDate: LocalDate,
  var endDate: LocalDate?,
  @ManyToOne
  @JoinColumn(name = "probation_delivery_unit_id")
  var probationDeliveryUnit: ProbationDeliveryUnitEntity?,
  @Column(name = "turnaround_working_day_count")
  var turnaroundWorkingDays: Int,
) : PremisesEntity(
  id,
  name,
  addressLine1,
  addressLine2,
  town,
  postcode,
  longitude,
  latitude,
  notes,
  emailAddress,
  probationRegion,
  localAuthorityArea,
  bookings,
  lostBeds,
  rooms,
  characteristics,
  status,
) {
  val cas3PremisesStatus: Cas3PremisesStatus
    get() = when (this.status) {
      PropertyStatus.active -> Cas3PremisesStatus.online
      PropertyStatus.archived -> Cas3PremisesStatus.archived
    }
}

data class ApprovedPremisesBasicSummary(
  val id: UUID,
  val name: String,
  val apCode: String,
  val apAreaId: UUID,
  val apAreaName: String,
  val bedCount: Int,
  val supportsSpaceBookings: Boolean,
  val fullAddress: String?,
  val addressLine1: String,
  val addressLine2: String?,
  val town: String?,
  val postcode: String,
)
