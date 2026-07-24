package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

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
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.SqlUtil
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.SqlUtil.getUUID
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas1BedRepository : JpaRepository<Cas1BedEntity, UUID> {
  fun findByCode(bedCode: String): Cas1BedEntity?

  fun findByCodeAndRoomId(bedCode: String, roomId: UUID): Cas1BedEntity?

  fun findByRoomPremisesId(premisesId: UUID): List<Cas1BedEntity>

  fun findByRoomPremisesIdAndEndDateIsNull(premisesId: UUID): List<Cas1BedEntity>

  @Query(nativeQuery = true)
  fun getDetailById(id: UUID): Cas1DomainBedSummary?

  @Modifying
  @Query("UPDATE Cas1BedEntity b SET b.code = :code WHERE b.id = :id")
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
      INNER JOIN cas1_beds b ON b.room_id = r.id
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

@NamedNativeQuery(
  name = "Cas1BedEntity.getDetailById",
  query =
  """
      select b.id as id,
      cast(b.name as text) as name,
      cast(r.name as text) as roomName,
      r.id as roomId,
      (
        select count(booking.id)
        from cas3_bookings booking
          left join cas3_cancellations cancellation
            on booking.id = cancellation.booking_id
          left join cas3_non_arrivals non_arrival 
            on non_arrival.booking_id = booking.id
        where booking.bed_id = b.id
          and booking.arrival_date <= CURRENT_DATE
          and booking.departure_date >= CURRENT_DATE
          and cancellation IS NULL
          and non_arrival IS NULL
      ) > 0 as bedBooked,
      false as bedOutOfService
      from cas1_beds b
           join rooms r on b.room_id = r.id
      where b.id = cast(?1 as UUID)
  """,
  resultSetMapping = "DomainBedSummaryMapping",
)
@SqlResultSetMapping(
  name = "DomainBedSummaryMapping",
  classes = [
    ConstructorResult(
      targetClass = Cas1DomainBedSummary::class,
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
@Table(name = "cas1_beds")
data class Cas1BedEntity(
  @Id
  val id: UUID,
  var name: String,
  val code: String?,
  @ManyToOne
  @JoinColumn(name = "room_id")
  val room: RoomEntity,
  var createdDate: LocalDate?,
  val startDate: LocalDate?,
  /**
   * For CAS1 this is inclusive (i.e. bed is not available on the end date)
   */
  var endDate: LocalDate?,
  @CreationTimestamp
  var createdAt: OffsetDateTime = OffsetDateTime.now(),
) {
  fun isActive(now: LocalDate) = isActive(now, endDate)
  override fun toString() = "Cas1BedEntity: $id"

  companion object {
    fun isActive(now: LocalDate, bedEndDate: LocalDate?): Boolean = bedEndDate == null || bedEndDate.isAfter(now)
  }
}

open class Cas1DomainBedSummary(
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
