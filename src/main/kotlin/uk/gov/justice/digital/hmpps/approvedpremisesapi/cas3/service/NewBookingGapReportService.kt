package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service

import com.opencsv.CSVWriter
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.BedspaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.BedspaceGap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.BedspaceInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.BedspaceUnavailableRanges
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.BedspaceVoid
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.BookingRecord
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.DateRange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.UnavailablePeriod
import java.io.OutputStream
import java.nio.charset.Charset
import java.time.LocalDate

@Service
class NewBookingGapReportService(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {
    
    companion object Companion {
        private val log = LoggerFactory.getLogger(NewBookingGapReportService::class.java)
    }

  fun generateBookingGapReportToCsv(
    startDate: LocalDate,
    endDate: LocalDate,
    outputStream: OutputStream,
  ) {
    val gapResults = generateBookingGapReport(startDate, endDate)

    CSVWriter(outputStream.writer(Charset.defaultCharset())).use { writer ->

      val headers = arrayOf(
        "probation_region",
        "pdu_name",
        "premises_name",
        "bed_name",
        "gap",
        "gap_days",
        "turnaround_days"
      )
      writer.writeNext(headers)

      gapResults.forEach { gap ->
        val row = arrayOf(
          gap.probationRegion,
          gap.pduName,
          gap.premisesName,
          gap.bedName,
          gap.gap,
          gap.gapDays.toString(),
          gap.turnaroundDays?.toString() ?: ""
        )
        writer.writeNext(row)
      }
    }
  }

    private fun generateBookingGapReport(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<BedspaceGap> {
        log.info("Starting booking gap report calculation for period $startDate to $endDate")
        
        // Step 1: Get all relevant bedspaces
        val bedspaces = getBedspaces(startDate, endDate)
        log.info("Found ${bedspaces.size} bedspaces for gap analysis")
        
        // Step 2: Get all bookings and voids
        val allBookings = getAllBookings(startDate, endDate)
        val allVoids = getBedspaceVoids(startDate, endDate)
        log.info("Found ${allBookings.size} bookings and ${allVoids.size} voids")
        
        // Step 3: Combine bedspace info with bookings
        val bedspaceBookings = combineBedspacesWithBookings(bedspaces, allBookings)
        
        // Step 4: Create unavailable periods
        val unavailablePeriods = createUnavailablePeriods(bedspaceBookings, allVoids)
        
        // Step 5: Find gaps for each bedspace
        val gaps = findGapsForAllBedspaces(bedspaces, unavailablePeriods, startDate, endDate)
        
        // Step 6: Filter out zero-day gaps and sort
        val result = gaps.filter { it.gapDays > 0 }
            .sortedWith(compareBy({ it.probationRegion }, { it.pduName }, { it.premisesName }, { it.bedName }))
        
        log.info("Generated ${result.size} gaps with total ${result.sumOf { it.gapDays }} gap days")
        return result
    }

  private fun getBedspaces(startDate: LocalDate, endDate: LocalDate): List<BedspaceInfo> {
        val query = """
            SELECT beds.id,
                   premises.name as premises_name,
                   rooms.name as room_name,
                   probation_regions.name as probation_region,
                   probation_delivery_units.name as pdu_name,
                   beds.end_date
            FROM beds
            INNER JOIN rooms on beds.room_id = rooms.id
            INNER JOIN premises on rooms.premises_id = premises.id
            INNER JOIN probation_regions on probation_regions.id = premises.probation_region_id
            INNER JOIN temporary_accommodation_premises on temporary_accommodation_premises.premises_id = premises.id
            INNER JOIN probation_delivery_units on probation_delivery_units.id = temporary_accommodation_premises.probation_delivery_unit_id
            WHERE (beds.end_date is null or beds.end_date >= :startDate) 
              AND beds.created_at <= :endDate
        """.trimIndent()
        
        return jdbcTemplate.query(query, mapOf("startDate" to startDate, "endDate" to endDate)) { rs, _ ->
            BedspaceInfo(
                id = rs.getLong("id"),
                premisesName = rs.getString("premises_name"),
                roomName = rs.getString("room_name"),
                probationRegion = rs.getString("probation_region"),
                pduName = rs.getString("pdu_name"),
                endDate = rs.getDate("end_date")?.toLocalDate()
            )
        }
    }

  private fun getAllBookings(startDate: LocalDate, endDate: LocalDate): List<BookingRecord> {
        val query = """
            SELECT bed_id,
                   arrival_date,
                   departure_date,
                   (SELECT working_day_count
                    FROM cas3_turnarounds
                    WHERE cas3_turnarounds.id = (
                        SELECT id
                        FROM cas3_turnarounds
                        WHERE cas3_turnarounds.booking_id = bookings.id
                        ORDER BY cas3_turnarounds.created_at DESC
                        LIMIT 1)) turnaround_days
            FROM bookings
            LEFT JOIN cancellations on cancellations.booking_id = bookings.id
            WHERE bookings.service = 'temporary-accommodation' 
              AND cancellations.id is null 
              AND arrival_date <= :endDate 
              AND departure_date >= :startDate 
              AND arrival_date != departure_date
        """.trimIndent()
        
        return jdbcTemplate.query(query, mapOf("startDate" to startDate, "endDate" to endDate)) { rs, _ ->
            BookingRecord(
                bedId = rs.getLong("bed_id"),
                arrivalDate = rs.getDate("arrival_date").toLocalDate(),
                departureDate = rs.getDate("departure_date").toLocalDate(),
                turnaroundDays = rs.getObject("turnaround_days") as? Int
            )
        }
    }

  private fun getBedspaceVoids(startDate: LocalDate, endDate: LocalDate): List<BedspaceVoid> {
        val query = """
            SELECT voids.bed_id,
                   premises.name as premises_name,
                   rooms.name as room_name,
                   probation_regions.name as probation_region,
                   probation_delivery_units.name as pdu_name,
                   voids.start_date,
                   voids.end_date
            FROM cas3_void_bedspaces voids
            INNER JOIN beds ON voids.bed_id = beds.id
            INNER JOIN rooms on beds.room_id = rooms.id
            INNER JOIN premises on rooms.premises_id = premises.id
            INNER JOIN probation_regions on probation_regions.id = premises.probation_region_id
            INNER JOIN temporary_accommodation_premises on temporary_accommodation_premises.premises_id = premises.id
            INNER JOIN probation_delivery_units on probation_delivery_units.id = temporary_accommodation_premises.probation_delivery_unit_id
            WHERE voids.start_date <= :endDate AND voids.end_date >= :startDate
        """.trimIndent()
        
        return jdbcTemplate.query(query, mapOf("startDate" to startDate, "endDate" to endDate)) { rs, _ ->
            BedspaceVoid(
                bedId = rs.getLong("bed_id"),
                premisesName = rs.getString("premises_name"),
                roomName = rs.getString("room_name"),
                probationRegion = rs.getString("probation_region"),
                pduName = rs.getString("pdu_name"),
                startDate = rs.getDate("start_date").toLocalDate(),
                endDate = rs.getDate("end_date").toLocalDate()
            )
        }
    }

  private fun combineBedspacesWithBookings(
        bedspaces: List<BedspaceInfo>,
        bookings: List<BookingRecord>
    ): List<BedspaceBooking> {
        return bedspaces.flatMap { bedspace ->
            bookings.filter { it.bedId == bedspace.id }
                .map { booking ->
                    BedspaceBooking(
                        bedId = bedspace.id,
                        premisesName = bedspace.premisesName,
                        roomName = bedspace.roomName,
                        probationRegion = bedspace.probationRegion,
                        pduName = bedspace.pduName,
                        arrivalDate = booking.arrivalDate,
                        departureDate = booking.departureDate,
                        turnaroundDays = booking.turnaroundDays
                    )
                }
        }.sortedWith(compareBy({ it.premisesName }, { it.roomName }, { it.arrivalDate }))
    }

  private fun createUnavailablePeriods(
        bedspaceBookings: List<BedspaceBooking>,
        bedspaceVoids: List<BedspaceVoid>
    ): List<UnavailablePeriod> {

        val bookingPeriods = bedspaceBookings
            .filter { it.arrivalDate != it.departureDate }
            .map { booking ->
                UnavailablePeriod(
                    bedId = booking.bedId,
                    premisesName = booking.premisesName,
                    roomName = booking.roomName,
                    probationRegion = booking.probationRegion,
                    pduName = booking.pduName,
                    startDate = booking.arrivalDate,
                    endDate = booking.departureDate,
                    turnaroundDays = booking.turnaroundDays ?: 0
                )
            }

        val voidPeriods = bedspaceVoids.map { void ->
            UnavailablePeriod(
                bedId = void.bedId,
                premisesName = void.premisesName,
                roomName = void.roomName,
                probationRegion = void.probationRegion,
                pduName = void.pduName,
                startDate = void.startDate,
                endDate = void.endDate,
                turnaroundDays = 0
            )
        }
        
        return bookingPeriods + voidPeriods
    }

  private fun findGapsForAllBedspaces(
        bedspaces: List<BedspaceInfo>,
        unavailablePeriods: List<UnavailablePeriod>,
        reportStartDate: LocalDate,
        reportEndDate: LocalDate
    ): List<BedspaceGap> {
        
        return bedspaces.flatMap { bedspace ->
            val bedspaceUnavailable = unavailablePeriods.filter { it.bedId == bedspace.id }
            findGapsForBedspace(bedspace, bedspaceUnavailable, reportStartDate, reportEndDate)
        }
    }


  private fun findGapsForBedspace(
        bedspace: BedspaceInfo,
        unavailablePeriods: List<UnavailablePeriod>,
        reportStartDate: LocalDate,
        reportEndDate: LocalDate
    ): List<BedspaceGap> {
        
        if (unavailablePeriods.isEmpty()) {
            return listOf(
                BedspaceGap.create(
                    probationRegion = bedspace.probationRegion,
                    pduName = bedspace.pduName,
                    premisesName = bedspace.premisesName,
                    bedName = bedspace.roomName,
                    gapRange = DateRange(reportStartDate, reportEndDate),
                    turnaroundDays = null
                )
            )
        }

        val unavailableRanges = BedspaceUnavailableRanges(
          bedId = bedspace.id,
          probationRegion = bedspace.probationRegion,
          pduName = bedspace.pduName,
          premisesName = bedspace.premisesName,
          roomName = bedspace.roomName,
          unavailablePeriods = unavailablePeriods,
          dateMin = unavailablePeriods.minOfOrNull { it.startDate },
          dateMax = unavailablePeriods.maxOfOrNull { it.endDate }
        )

        val mergedPeriods = unavailableRanges.getMergedPeriods()

        return findGapsBetweenPeriods(bedspace, mergedPeriods, reportStartDate, reportEndDate)
    }
    

    private fun findGapsBetweenPeriods(
        bedspace: BedspaceInfo,
        unavailablePeriods: List<UnavailablePeriod>,
        reportStartDate: LocalDate,
        reportEndDate: LocalDate
    ): List<BedspaceGap> {
        
        val gaps = mutableListOf<BedspaceGap>()
        
        if (unavailablePeriods.isEmpty()) {
            return gaps
        }

        val analysisStart = minOf(unavailablePeriods.minOfOrNull { it.startDate } ?: reportStartDate, reportStartDate)
        val analysisEnd = maxOf(unavailablePeriods.maxOfOrNull { it.endDate } ?: reportEndDate, reportEndDate)

        if (analysisStart < unavailablePeriods.first().startDate) {
            gaps.add(createGap(bedspace, analysisStart, unavailablePeriods.first().startDate, null))
        }

        for (i in 0 until unavailablePeriods.size - 1) {
            val currentEnd = unavailablePeriods[i].endDate
            val nextStart = unavailablePeriods[i + 1].startDate
            
            if (currentEnd < nextStart) {
                gaps.add(createGap(bedspace, currentEnd, nextStart, unavailablePeriods[i + 1].turnaroundDays))
            }
        }

        if (unavailablePeriods.last().endDate < analysisEnd) {
            gaps.add(createGap(bedspace, unavailablePeriods.last().endDate, analysisEnd, null))
        }
        
        return gaps
    }
    

    private fun createGap(
        bedspace: BedspaceInfo,
        startDate: LocalDate,
        endDate: LocalDate,
        turnaroundDays: Int?
    ): BedspaceGap {
        return BedspaceGap.create(
            probationRegion = bedspace.probationRegion,
            pduName = bedspace.pduName,
            premisesName = bedspace.premisesName,
            bedName = bedspace.roomName,
            gapRange = DateRange(startDate, endDate),
            turnaroundDays = turnaroundDays
        )
    }
}