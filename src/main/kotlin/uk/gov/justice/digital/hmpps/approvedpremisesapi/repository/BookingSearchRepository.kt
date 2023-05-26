package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class BookingSearchRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
  private val bookingSearchQuery =
    """
      SELECT
        b.crn AS person_crn,
        b.id AS booking_id,
        s.booking_status AS booking_status,
        b.arrival_date AS booking_start_date,
        b.departure_date AS booking_end_date,
        b.created_at AS booking_created_at,
        p.id AS premises_id,
        p.name AS premises_name,
        p.address_line1 AS premises_address_line1,
        p.address_line2 AS premises_address_line2,
        p.town AS premises_town,
        p.postcode AS premises_postcode,
        r.id AS room_id,
        r.name AS room_name,
        b2.id AS bed_id,
        b2.name AS bed_name
      FROM bookings b
      LEFT JOIN (
        SELECT
          b.id,
          (
            CASE
              WHEN (SELECT COUNT(1) FROM cancellations c WHERE c.booking_id = b.id) > 0 THEN 'cancelled'
              WHEN (SELECT COUNT(1) FROM departures d WHERE d.booking_id = b.id) > 0 THEN 'departed'
              WHEN (SELECT COUNT(1) FROM arrivals a WHERE a.booking_id = b.id) > 0 THEN 'arrived'
              WHEN (SELECT COUNT(1) FROM confirmations c2 WHERE c2.booking_id = b.id) > 0 THEN 'confirmed'
              WHEN (SELECT COUNT(1) FROM non_arrivals n WHERE n.booking_id = n.id) > 0 THEN 'not-arrived'
              WHEN :service = 'approved-premises' THEN 'awaiting-arrival'
              ELSE 'provisional'
            END
          ) AS booking_status
        FROM bookings b
      ) as s ON b.id = s.id
      LEFT JOIN beds b2 ON b.bed_id = b2.id
      LEFT JOIN rooms r ON b2.room_id = r.id
      LEFT JOIN premises p ON r.premises_id = p.id
      WHERE b.service = :service
      #OPTIONAL_FILTERS;
    """.trimIndent()

  private val bookingStatusFilter = """
    s.booking_status = :booking_status
  """.trimIndent()

  private val probationRegionFilter = """
    p.probation_region_id = :probation_region
  """.trimIndent()

  fun findBookings(
    serviceName: ServiceName,
    status: BookingStatus?,
    probationRegionId: UUID?,
  ): List<BookingSearchResult> {
    val params = MapSqlParameterSource().apply {
      addValue("service", serviceName.value)
      addValue("booking_status", status?.value)
      addValue("probation_region", probationRegionId)
    }

    var optionalFilters = ""
    if (status != null) {
      optionalFilters += "AND $bookingStatusFilter\n"
    }
    if (probationRegionId != null) {
      optionalFilters += "AND $probationRegionFilter\n"
    }

    val query = bookingSearchQuery.replace("#OPTIONAL_FILTERS", optionalFilters)

    val result = namedParameterJdbcTemplate.query(
      query,
      params,
      ResultSetExtractor { resultSet ->
        val bookings = mutableListOf<BookingSearchResult>()

        while (resultSet.next()) {
          val personCrn = resultSet.getString("person_crn")
          val bookingId = UUID.fromString(resultSet.getString("booking_id"))
          val bookingStatus = resultSet.getString("booking_status")
          val bookingStartDate = resultSet.getObject("booking_start_date", LocalDate::class.java)
          val bookingEndDate = resultSet.getObject("booking_end_date", LocalDate::class.java)
          val bookingCreatedAt = resultSet.getObject("booking_created_at", OffsetDateTime::class.java)
          val premisesId = UUID.fromString(resultSet.getString("premises_id"))
          val premisesName = resultSet.getString("premises_name")
          val premisesAddressLine1 = resultSet.getString("premises_address_line1")
          val premisesAddressLine2 = resultSet.getString("premises_address_line2")
          val premisesTown = resultSet.getString("premises_town")
          val premisesPostcode = resultSet.getString("premises_postcode")
          val roomId = UUID.fromString(resultSet.getString("room_id"))
          val roomName = resultSet.getString("room_name")
          val bedId = UUID.fromString(resultSet.getString("bed_id"))
          val bedName = resultSet.getString("bed_name")

          bookings += BookingSearchResult(
            personName = null,
            personCrn,
            bookingId,
            bookingStatus,
            bookingStartDate,
            bookingEndDate,
            bookingCreatedAt,
            premisesId,
            premisesName,
            premisesAddressLine1,
            premisesAddressLine2,
            premisesTown,
            premisesPostcode,
            roomId,
            roomName,
            bedId,
            bedName,
          )
        }

        bookings
      },
    )

    return result ?: emptyList()
  }
}

data class BookingSearchResult(
  var personName: String?,
  val personCrn: String,
  val bookingId: UUID,
  val bookingStatus: String,
  val bookingStartDate: LocalDate,
  val bookingEndDate: LocalDate,
  val bookingCreatedAt: OffsetDateTime,
  val premisesId: UUID,
  val premisesName: String,
  val premisesAddressLine1: String,
  val premisesAddressLine2: String?,
  val premisesTown: String?,
  val premisesPostcode: String,
  val roomId: UUID,
  val roomName: String,
  val bedId: UUID,
  val bedName: String,
)
