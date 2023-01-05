package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import org.jetbrains.kotlinx.dataframe.api.CreateDataFrameDsl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BookingsReportGenerator : ReportGenerator<BookingEntity, BookingsReportRow, BookingsReportProperties>() {

  override val convert: CreateDataFrameDsl<BookingEntity>.() -> Unit = {
    BookingsReportRow::probationRegion from { it.premises.probationRegion.name }
    BookingsReportRow::crn from { it.crn }
    BookingsReportRow::offerAccepted from { it.confirmation != null }
    BookingsReportRow::isCancelled from { it.cancellation != null }
    BookingsReportRow::cancellationReason from { it.cancellation?.reason?.name }
    BookingsReportRow::startDate from { it.arrival?.arrivalDate }
    BookingsReportRow::endDate from { it.arrival?.expectedDepartureDate }
    BookingsReportRow::actualEndDate from { it.departure?.dateTime?.toLocalDate() }
    BookingsReportRow::currentNightsStayed from { entity ->
      entity.arrival?.arrivalDate?.let { ChronoUnit.DAYS.between(it, LocalDate.now()).toInt() }
    }
    BookingsReportRow::actualNightsStayed from { entity ->
      val arrivalDate = entity.arrival?.arrivalDate ?: return@from null
      entity.departure?.dateTime?.let { ChronoUnit.DAYS.between(arrivalDate, it.toLocalDate()).toInt() }
    }
    BookingsReportRow::accommodationOutcome from { it.departure?.moveOnCategory?.name }
  }

  override fun filter(properties: BookingsReportProperties): (BookingEntity) -> Boolean = {
    it.service == properties.serviceName.value &&
      (properties.probationRegionId == null || it.premises.probationRegion.id == properties.probationRegionId)
  }
}
