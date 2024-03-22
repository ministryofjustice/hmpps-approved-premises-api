package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
class CalendarRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
  private val bookingsForPremisesQuery =
    """
    SELECT bed.id AS bed_id, 
           bed.name AS bed_name, 
           b.id AS booking_id, 
           b.arrival_date AS arrival_date, 
           b.departure_date AS departure_date,
           b.crn AS crn,
           ((n.id, c.id) IS NULL) AS active
    FROM premises p 
    JOIN rooms r ON r.premises_id = p.id 
    JOIN beds bed ON bed.room_id = r.id AND (bed.end_date IS NULL OR bed.end_date >= :startDate)
    LEFT JOIN bookings b ON b.bed_id = bed.id AND tsrange(b.arrival_date, b.departure_date, '[]') && tsrange(:startDate, :endDate, '[]')
    LEFT JOIN cancellations c ON c.booking_id = b.id
    LEFT JOIN non_arrivals n ON n.booking_id = b.id
    WHERE p.id = :premisesId
"""

  private val lostBedsForPremisesQuery =
    """
    SELECT bed.id AS bed_id, 
           bed.name AS bed_name, 
           lb.id AS lost_bed_id, 
           lb.start_date AS start_date, 
           lb.end_date AS end_date,
           (c.id IS NULL) AS active
    FROM premises p 
    JOIN rooms r ON r.premises_id = p.id
    JOIN beds bed ON bed.room_id = r.id AND (bed.end_date IS NULL OR bed.end_date >= :startDate)
    LEFT JOIN lost_beds lb ON lb.bed_id = bed.id AND tsrange(lb.start_date, lb.end_date, '[]') && tsrange(:startDate, :endDate, '[]')
    LEFT JOIN lost_bed_cancellations c ON c.lost_bed_id = lb.id
    WHERE p.id = :premisesId
"""

  fun getCalendarInfo(premisesId: UUID, startDate: LocalDate, endDate: LocalDate): Map<CalendarBedInfo, List<CalendarOccupancyInfo>> {
    val params = MapSqlParameterSource().apply {
      addValue("premisesId", premisesId)
      addValue("startDate", startDate)
      addValue("endDate", endDate)
    }

    val resultsMap = mutableMapOf<CalendarBedInfo, MutableList<CalendarOccupancyInfo>>()

    namedParameterJdbcTemplate.query(
      bookingsForPremisesQuery,
      params,
      ResultSetExtractor { resultSet ->
        while (resultSet.next()) {
          val bedKey = CalendarBedInfo(
            bedId = UUID.fromString(resultSet.getString("bed_id")),
            bedName = resultSet.getString("bed_name"),
          )

          resultsMap.putIfAbsent(bedKey, mutableListOf())

          if (resultSet.getString("booking_id") != null && resultSet.getBoolean("active")) {
            resultsMap[bedKey]!! += CalendarBookingInfo(
              startDate = resultSet.getObject("arrival_date", LocalDate::class.java),
              endDate = resultSet.getObject("departure_date", LocalDate::class.java),
              bookingId = UUID.fromString(resultSet.getString("booking_id")),
              crn = resultSet.getString("crn"),
              personName = null,
            )
          }
        }
      },
    )

    namedParameterJdbcTemplate.query(
      lostBedsForPremisesQuery,
      params,
      ResultSetExtractor { resultSet ->
        while (resultSet.next()) {
          val bedKey = CalendarBedInfo(
            bedId = UUID.fromString(resultSet.getString("bed_id")),
            bedName = resultSet.getString("bed_name"),
          )

          resultsMap.putIfAbsent(bedKey, mutableListOf())

          if (resultSet.getString("lost_bed_id") != null && resultSet.getBoolean("active")) {
            resultsMap[bedKey]!! += CalendarLostBedInfo(
              startDate = resultSet.getObject("start_date", LocalDate::class.java),
              endDate = resultSet.getObject("end_date", LocalDate::class.java),
              lostBedId = UUID.fromString(resultSet.getString("lost_bed_id")),
            )
          }
        }
      },
    )

    return resultsMap
  }
}

data class CalendarBedInfo(
  val bedId: UUID,
  val bedName: String,
)

sealed interface CalendarOccupancyInfo {
  val startDate: LocalDate
  val endDate: LocalDate
}

data class CalendarBookingInfo(
  override val startDate: LocalDate,
  override val endDate: LocalDate,
  val bookingId: UUID,
  val crn: String,
  var personName: String? = null,
) : CalendarOccupancyInfo

data class CalendarLostBedInfo(
  override val startDate: LocalDate,
  override val endDate: LocalDate,
  val lostBedId: UUID,
) : CalendarOccupancyInfo
