package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceUnavailableRanges
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceVoid
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BookingGapReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BookingRecord
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.Cas3BookingGapReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.DateRange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.UnavailablePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BookingGapReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import java.time.LocalDate

class BookingGapReportGenerator(private val workingDayService: WorkingDayService) : ReportGenerator<BookingGapReportData, Cas3BookingGapReportRow, BookingGapReportProperties>(Cas3BookingGapReportRow::class) {
  override fun filter(properties: BookingGapReportProperties): (BookingGapReportData) -> Boolean = {
    true
  }

  override val convert: BookingGapReportData.(properties: BookingGapReportProperties) -> List<Cas3BookingGapReportRow> = { properties ->

    // Step 3: Combine bedspace info with bookings
    val bedspaceBookings = combineBedspacesWithBookings(bedspaces, bookings)

    // Step 4: Create unavailable periods
    val unavailablePeriods = createUnavailablePeriods(bedspaceBookings, bedspaceVoids)

    // Step 5: Find gaps for each bedspace
    val gaps = findGapsForAllBedspaces(bedspaces, unavailablePeriods, properties.startDate, properties.endDate)

    // Step 6: Filter out zero-day gaps and sort
    val result = gaps
      .sortedWith(compareBy({ it.probationRegion }, { it.pduName }, { it.premisesName }, { it.bedName })).toMutableList()
    result
  }

  private fun combineBedspacesWithBookings(
    bedspaces: List<BedspaceInfo>,
    bookings: List<BookingRecord>,
  ): List<BedspaceBooking> = bedspaces.flatMap { bedspace ->
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
          turnaroundDays = booking.turnaroundDays,
        )
      }
  }.sortedWith(compareBy({ it.premisesName }, { it.roomName }, { it.arrivalDate }))

  private fun createUnavailablePeriods(
    bedspaceBookings: List<BedspaceBooking>,
    bedspaceVoids: List<BedspaceVoid>,
  ): List<UnavailablePeriod> {
    val bookingPeriods = bedspaceBookings
      .map { booking ->
        val endDateWithTurnaround = if ((booking.turnaroundDays ?: 0) > 0) workingDayService.addWorkingDays(booking.departureDate, booking.turnaroundDays ?: 0) else booking.departureDate
        UnavailablePeriod(
          bedId = booking.bedId,
          startDate = booking.arrivalDate,
          endDate = endDateWithTurnaround,
        )
      }

    val voidPeriods = bedspaceVoids.map { void ->
      UnavailablePeriod(
        bedId = void.bedId,
        startDate = void.startDate,
        endDate = void.endDate,
      )
    }

    return bookingPeriods + voidPeriods
  }

  private fun findGapsForAllBedspaces(
    bedspaces: List<BedspaceInfo>,
    unavailablePeriods: List<UnavailablePeriod>,
    reportStartDate: LocalDate,
    reportEndDate: LocalDate,
  ): List<Cas3BookingGapReportRow> = bedspaces.flatMap { bedspace ->
    val bedspaceUnavailable = unavailablePeriods.filter { it.bedId == bedspace.id }
    val maxStartDate = maxOf(bedspace.startDate, reportStartDate)
    val maxEndDate = minOf(bedspace.endDate ?: reportEndDate, reportEndDate)
    findGapsForBedspace(bedspace, bedspaceUnavailable, maxStartDate, maxEndDate)
  }

  private fun findGapsForBedspace(
    bedspace: BedspaceInfo,
    unavailablePeriods: List<UnavailablePeriod>,
    reportStartDate: LocalDate,
    reportEndDate: LocalDate,
  ): List<Cas3BookingGapReportRow> {
    if (unavailablePeriods.isEmpty()) {
      return listOf(
        Cas3BookingGapReportRow(
          probationRegion = bedspace.probationRegion,
          pduName = bedspace.pduName,
          premisesName = bedspace.premisesName,
          bedName = bedspace.roomName,
          gap = DateRange(reportStartDate, reportEndDate).toString(),
          gapDays = DateRange(reportStartDate, reportEndDate).daysBetween(),
        ),
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
      dateMax = unavailablePeriods.maxOfOrNull { it.endDate },
    )

    val mergedPeriods = unavailableRanges.getMergedPeriods()

    return findGapsBetweenPeriods(bedspace, mergedPeriods, reportStartDate, reportEndDate)
  }

  private fun findGapsBetweenPeriods(
    bedspace: BedspaceInfo,
    unavailablePeriods: List<UnavailablePeriod>,
    reportStartDate: LocalDate,
    reportEndDate: LocalDate,
  ): List<Cas3BookingGapReportRow> {
    val gaps = mutableListOf<Cas3BookingGapReportRow>()
    if (unavailablePeriods.isEmpty()) return gaps

    val analysisStart = minOf(unavailablePeriods.minOfOrNull { it.startDate } ?: reportStartDate, reportStartDate)
    val analysisEnd = maxOf(unavailablePeriods.maxOfOrNull { it.endDate } ?: reportEndDate, reportEndDate)

    if (analysisStart < unavailablePeriods.first().startDate) {
      gaps.add(createGap(bedspace, analysisStart, unavailablePeriods.first().startDate.minusDays(1)))
    }

    for (i in 0 until unavailablePeriods.size - 1) {
      val prev = unavailablePeriods[i]
      val currentEnd = prev.endDate
      val nextStart = unavailablePeriods[i + 1].startDate

      if (currentEnd < nextStart) {
        gaps.add(
          createGap(
            bedspace,
            currentEnd.plusDays(1),
            nextStart.minusDays(1),
          ),
        )
      }
    }

    if (unavailablePeriods.last().endDate < analysisEnd) {
      gaps.add(
        createGap(
          bedspace,
          unavailablePeriods.last().endDate.plusDays(1),
          analysisEnd,
        ),
      )
    }
    return gaps
  }

  private fun createGap(
    bedspace: BedspaceInfo,
    startDate: LocalDate,
    endDate: LocalDate,
  ): Cas3BookingGapReportRow = Cas3BookingGapReportRow(
    probationRegion = bedspace.probationRegion,
    pduName = bedspace.pduName,
    premisesName = bedspace.premisesName,
    bedName = bedspace.roomName,
    gap = DateRange(startDate, endDate).toString(),
    gapDays = DateRange(startDate, endDate).daysBetween(),
  )
}
