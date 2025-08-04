package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.ColumnResult
import jakarta.persistence.ConstructorResult
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedNativeQuery
import jakarta.persistence.SqlResultSetMapping
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.SqlUtil.getUUID
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface BedRepository : JpaRepository<BedEntity, UUID> {
  fun findByCode(bedCode: String): BedEntity?

  fun findByCodeAndRoomId(bedCode: String, roomId: UUID): BedEntity?

  fun findByRoomPremisesId(premisesId: UUID): List<BedEntity>

  fun findByRoomPremisesIdAndEndDateIsNull(premisesId: UUID): List<BedEntity>

  @Query(
    """
    SELECT
      b.*
    FROM beds b
    JOIN rooms r on b.room_id = r.id
    WHERE r.premises_Id = :premisesId AND b.id = :bedspaceId
  """,
    nativeQuery = true,
  )
  fun findCas3Bedspace(premisesId: UUID, bedspaceId: UUID): BedEntity?

  @Query(nativeQuery = true)
  fun getDetailById(id: UUID): DomainBedSummary?

  @Query(
    """
    SELECT b 
    FROM BedEntity b 
    WHERE b.id = :bedId AND 
    b.endDate IS NOT NULL AND b.endDate < :endDate
  """,
  )
  fun findArchivedBedByBedIdAndDate(bedId: UUID, endDate: LocalDate): BedEntity?

  @Query(
    """
        SELECT b.*
        FROM beds b
        INNER JOIN rooms r ON b.room_id = r.id
        INNER JOIN premises p ON r.premises_id = p.id
        WHERE p.service = :service
        ORDER BY b.id
    """,
    nativeQuery = true,
  )
  fun findAllByService(service: String, pageable: Pageable): Slice<BedEntity>

  @Modifying
  @Query("UPDATE BedEntity b SET b.code = :code WHERE b.id = :id")
  fun updateCode(id: UUID, code: String)
}

@Repository
class Cas1BedsRepository(
  private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
  fun bedSummary(
    premisesIds: List<UUID>,
    excludeEndedBeds: Boolean,
  ): List<Cas1PlanningBedSummary> {
    val params = mutableMapOf(
      "premisesIds" to premisesIds,
      "excludeEndedBeds" to excludeEndedBeds,
    )

    return jdbcTemplate.query(
      """
      SELECT 
        b.id AS bed_id,
        b."name" AS bed_name,
        b.end_date AS bed_end_date,
        r.id AS room_id,
        r.name AS room_name,
        ARRAY_REMOVE(ARRAY_AGG(c.property_name),null) AS characteristics,
        r.premises_id AS premises_id
      FROM rooms r
      INNER JOIN beds b ON b.room_id = r.id
      LEFT OUTER JOIN room_characteristics room_chars ON room_chars.room_id = r.id 
      LEFT OUTER JOIN "characteristics" c ON c.id = room_chars.characteristic_id 
      WHERE 
      r.premises_id IN (:premisesIds) AND 
      (:excludeEndedBeds = false OR b.end_date is null OR b.end_date >= NOW())
      GROUP BY b.id, b."name", r.id, r."name"
      """.trimIndent(),
      params,
    ) { rs, _ ->
      Cas1PlanningBedSummary(
        bedId = rs.getUUID("bed_id"),
        bedName = rs.getString("bed_name"),
        bedEndDate = rs.getDate("bed_end_date")?.toLocalDate(),
        roomId = rs.getUUID("room_id"),
        roomName = rs.getString("room_name"),
        characteristicsPropertyNames = SqlUtil.toStringList(rs.getArray("characteristics")),
        premisesId = rs.getUUID("premises_id"),
      )
    }
  }
}

const val BED_SUMMARY_QUERY =
  """
      select b.id as id,
      cast(b.name as text) as name,
      cast(r.name as text) as roomName,
      r.id as roomId,
      (
        select count(booking.id)
        from bookings booking
          left join cancellations cancellation
            on booking.id = cancellation.booking_id
          left join non_arrivals non_arrival 
            on non_arrival.booking_id = booking.id
        where booking.bed_id = b.id
          and booking.arrival_date <= CURRENT_DATE
          and booking.departure_date >= CURRENT_DATE
          and cancellation IS NULL
          and non_arrival IS NULL
      ) > 0 as bedBooked,
      (
        select count(void_bedspace.id)
        from cas3_void_bedspaces void_bedspace
          left join cas3_void_bedspace_cancellations cancellation
            on void_bedspace.id = cancellation.cas3_void_bedspace_id
        where void_bedspace.bed_id = b.id
          and void_bedspace.start_date <= CURRENT_DATE
          and void_bedspace.end_date >= CURRENT_DATE
          and cancellation IS NULL
      ) > 0 as bedOutOfService
      from beds b
           join rooms r on b.room_id = r.id
  """

@NamedNativeQuery(
  name = "BedEntity.findAllBedsForPremises",
  query =
  """
    $BED_SUMMARY_QUERY
    where r.premises_id = cast(?1 as UUID) and (b.end_date is null or b.end_date > CURRENT_DATE)
  """,
  resultSetMapping = "DomainBedSummaryMapping",
)
@NamedNativeQuery(
  name = "BedEntity.getDetailById",
  query =
  """
    $BED_SUMMARY_QUERY
    where b.id = cast(?1 as UUID)
  """,
  resultSetMapping = "DomainBedSummaryMapping",
)
@SqlResultSetMapping(
  name = "DomainBedSummaryMapping",
  classes = [
    ConstructorResult(
      targetClass = DomainBedSummary::class,
      columns = [
        ColumnResult(name = "id", type = UUID::class),
        ColumnResult(name = "name"),
        ColumnResult(name = "roomId", type = UUID::class),
        ColumnResult(name = "roomName"),
        ColumnResult(name = "bedBooked"),
        ColumnResult(name = "bedOutOfService"),
      ],
    ),
  ],
)
@Entity
@Table(name = "beds")
data class BedEntity(
  @Id
  val id: UUID,
  var name: String,
  val code: String?,
  @ManyToOne
  @JoinColumn(name = "room_id")
  val room: RoomEntity,
  val startDate: LocalDate?,
  /**
   * For CAS1 this is inclusive (i.e. bed is not available on the end date)
   */
  var endDate: LocalDate?,
  @CreationTimestamp
  var createdAt: OffsetDateTime?,
) {
  fun isActive(now: LocalDate) = Companion.isActive(now, endDate)
  fun isCas3BedspaceOnline() = (this.startDate == null || this.startDate!! <= LocalDate.now()) && (this.endDate == null || this.endDate!! > LocalDate.now())
  fun isCas3BedspaceUpcoming() = this.startDate?.isAfter(LocalDate.now()) == true
  fun isCas3BedspaceArchived() = this.endDate != null && this.endDate!! <= LocalDate.now()
  fun getCas3BedspaceStatus() = when {
    this.isCas3BedspaceUpcoming() -> Cas3BedspaceStatus.upcoming
    this.isCas3BedspaceArchived() -> Cas3BedspaceStatus.archived
    else -> Cas3BedspaceStatus.online
  }
  override fun toString() = "BedEntity: $id"

  companion object {
    fun isActive(now: LocalDate, bedEndDate: LocalDate?): Boolean = bedEndDate == null || bedEndDate.isAfter(now)
  }
}

open class DomainBedSummary(
  val id: UUID,
  val name: String,
  val roomId: UUID,
  val roomName: String,
  val bedBooked: Boolean,
  val bedOutOfService: Boolean,
)

data class Cas1PlanningBedSummary(
  val bedId: UUID,
  val bedName: String,
  val bedEndDate: LocalDate?,
  val roomId: UUID,
  val roomName: String,
  val characteristicsPropertyNames: List<String>,
  val premisesId: UUID,
)
