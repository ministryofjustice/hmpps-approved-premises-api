package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class Cas1ImportDeliusBookingDataSeedJob(
  private val jdbcTemplate: NamedParameterJdbcTemplate,
  private val cas1DeliusBookingImportRepository: Cas1DeliusBookingImportRepository,
) : SeedJob<Cas1DeliusBookingManagementDataRow>(
  requiredHeaders = setOf(
    "BOOKING_ID",
    "CRN",
    "EVENT_NUMBER",
    "KEY_WORKER_STAFF_CODE",
    "KEY_WORKER_FORENAME",
    "KEY_WORKER_MIDDLE_NAME",
    "KEY_WORKER_SURNAME",
    "DEPARTURE_REASON_CODE",
    "MOVE_ON_CATEGORY_CODE",
    "MOVE_ON_CATEGORY_DESCRIPTION",
    "EXPECTED_ARRIVAL_DATE",
    "ARRIVAL_DATE",
    "EXPECTED_DEPARTURE_DATE",
    "DEPARTURE_DATE",
    "NON_ARRIVAL_DATE",
    "NON_ARRIVAL_CONTACT_DATETIME_LIST",
    "NON_ARRIVAL_REASON_CODE",
    "NON_ARRIVAL_REASON_DESCRIPTION",
    "NON_ARRIVAL_NOTES",
  ),
  runInTransaction = false,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private companion object {
    val DELIUS_IMPORT_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
  }

  override fun preSeed() {
    log.info("Clearing table cas1_delius_booking_import before seeding new data")
    jdbcTemplate.update("TRUNCATE cas1_delius_booking_import", emptyMap<String, String>())
  }

  override fun deserializeRow(columns: Map<String, String>) = Cas1DeliusBookingManagementDataRow(
    bookingId = UUID.fromString(columns["BOOKING_ID"]!!.trim()),
    crn = columns.stringOrNull("CRN")!!,
    eventNumber = columns.stringOrNull("EVENT_NUMBER")!!,
    keyWorkerStaffCode = columns.stringOrNull("KEY_WORKER_STAFF_CODE"),
    keyWorkerForename = columns.stringOrNull("KEY_WORKER_FORENAME"),
    keyWorkerMiddleName = columns.stringOrNull("KEY_WORKER_MIDDLE_NAME"),
    keyWorkerSurname = columns.stringOrNull("KEY_WORKER_SURNAME"),
    departureReasonCode = columns.stringOrNull("DEPARTURE_REASON_CODE"),
    moveOnCategoryCode = columns.stringOrNull("MOVE_ON_CATEGORY_CODE"),
    moveOnCategoryDescription = columns.stringOrNull("MOVE_ON_CATEGORY_DESCRIPTION"),
    expectedArrivalDate = columns.dateFromUtcDateTime("EXPECTED_ARRIVAL_DATE")!!,
    arrivalDate = columns.dateFromUtcDateTime("ARRIVAL_DATE"),
    expectedDepartureDate = columns.dateFromUtcDateTime("EXPECTED_DEPARTURE_DATE"),
    departureDate = columns.dateFromUtcDateTime("DEPARTURE_DATE"),
    nonArrivalDate = columns.dateFromUtcDateTime("NON_ARRIVAL_DATE"),
    nonArrivalContactDateTime = columns.dateTimeFromList("NON_ARRIVAL_CONTACT_DATETIME_LIST"),
    nonArrivalReasonCode = columns.stringOrNull("NON_ARRIVAL_REASON_CODE"),
    nonArrivalReasonDescription = columns.stringOrNull("NON_ARRIVAL_REASON_DESCRIPTION"),
    nonArrivalNotes = columns.stringOrNull("NON_ARRIVAL_NOTES"),
  )

  private fun Map<String, String>.stringOrNull(label: String?) = this[label]?.trim()?.ifBlank { null }

  private fun Map<String, String>.dateTimeFromList(label: String?): LocalDateTime? {
    val dateTimeList = stringOrNull(label)
    if (dateTimeList.isNullOrBlank()) {
      return null
    }

    val lastDateTime = dateTimeList.split(",").last()
    return LocalDateTime.parse(lastDateTime, DELIUS_IMPORT_DATE_TIME_FORMATTER)
  }

  private fun Map<String, String>.dateFromUtcDateTime(label: String?): LocalDate? {
    val dateTime = stringOrNull(label)
    if (dateTime.isNullOrBlank()) {
      return null
    }

    return LocalDate.parse(dateTime.substring(startIndex = 0, endIndex = 10))
  }

  override fun processRow(row: Cas1DeliusBookingManagementDataRow) {
    cas1DeliusBookingImportRepository.save(
      Cas1DeliusBookingImportEntity(
        row.bookingId,
        row.crn,
        row.eventNumber,
        row.keyWorkerStaffCode,
        row.keyWorkerForename,
        row.keyWorkerMiddleName,
        row.keyWorkerSurname,
        row.departureReasonCode,
        row.moveOnCategoryCode,
        row.moveOnCategoryDescription,
        row.expectedArrivalDate,
        row.arrivalDate,
        row.expectedDepartureDate,
        row.departureDate,
        row.nonArrivalDate,
        row.nonArrivalContactDateTime?.let {
          ZonedDateTime.of(it, ZoneId.of("Europe/London")).toOffsetDateTime()
        },
        row.nonArrivalReasonCode,
        row.nonArrivalReasonDescription,
        row.nonArrivalNotes,
      ),
    )
  }

  override fun postSeed() {
    log.info(
      """
        Seeding complete. Note that the reported row count may be lower than the number of lines in the CSV file. 
        This is because some CSV rows have line breaks in 'notes' values
      """.trimIndent(),
    )
  }
}

data class Cas1DeliusBookingManagementDataRow(
  val bookingId: UUID,
  val crn: String,
  val eventNumber: String,
  val keyWorkerStaffCode: String?,
  val keyWorkerForename: String?,
  val keyWorkerMiddleName: String?,
  val keyWorkerSurname: String?,
  val departureReasonCode: String?,
  val moveOnCategoryCode: String?,
  val moveOnCategoryDescription: String?,
  val expectedArrivalDate: LocalDate,
  val arrivalDate: LocalDate?,
  val expectedDepartureDate: LocalDate?,
  val departureDate: LocalDate?,
  val nonArrivalDate: LocalDate?,
  val nonArrivalContactDateTime: LocalDateTime?,
  val nonArrivalReasonCode: String?,
  val nonArrivalReasonDescription: String?,
  val nonArrivalNotes: String?,
)
