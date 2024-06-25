package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredBySeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromNestedAuthorisableValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractResultFromClientResultOrThrow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ApprovedPremisesBookingSeedJob(
  fileName: String,
  private val bookingRepository: BookingRepository,
  private val bookingService: BookingService,
  private val communityApiClient: CommunityApiClient,
  private val bedRepository: BedRepository,
  private val departureReasonRepository: DepartureReasonRepository,
  private val moveOnCategoryRepository: MoveOnCategoryRepository,
  private val destinationProviderRepository: DestinationProviderRepository,
  private val nonArrivalReasonRepository: NonArrivalReasonRepository,
  private val cancellationReasonRepository: CancellationReasonRepository,
) : SeedJob<ApprovedPremisesBookingSeedCsvRow>(
  fileName = fileName,
  requiredHeaders = setOf(
    "id",
    "crn",
    "plannedArrivalDate",
    "plannedDepartureDate",
    "keyWorkerDeliusUsername",
    "bedCode",
    "arrivalDate",
    "arrivalNotes",
    "departureDateTime",
    "departureReason",
    "departureDestinationProvider",
    "departureMoveOnCategory",
    "departureNotes",
    "nonArrivalDate",
    "nonArrivalReason",
    "nonArrivalNotes",
    "cancellationDate",
    "cancellationReason",
    "cancellationNotes",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = ApprovedPremisesBookingSeedCsvRow(
    id = UUID.fromString(columns["id"]),
    crn = columns["crn"]!!,
    plannedArrivalDate = LocalDate.parse(columns["plannedArrivalDate"]),
    plannedDepartureDate = LocalDate.parse(columns["plannedDepartureDate"]),
    keyWorkerDeliusUsername = emptyToNull(columns["keyWorkerDeliusUsername"]),
    bedCode = columns["bedCode"]!!,
    arrivalDate = parseDateIfNotNull(emptyToNull(columns["arrivalDate"])),
    arrivalNotes = emptyToNull(columns["arrivalNotes"]),
    departureDateTime = parseDateTimeIfNotNull(emptyToNull(columns["departureDateTime"])),
    departureReason = emptyToNull(columns["departureReason"]),
    departureDestinationProvider = emptyToNull(columns["departureDestinationProvider"]),
    departureMoveOnCategory = emptyToNull(columns["departureMoveOnCategory"]),
    departureNotes = emptyToNull(columns["departureNotes"]),
    nonArrivalDate = parseDateIfNotNull(emptyToNull(columns["nonArrivalDate"])),
    nonArrivalReason = emptyToNull(columns["nonArrivalReason"]),
    nonArrivalNotes = emptyToNull(columns["nonArrivalNotes"]),
    cancellationDate = parseDateIfNotNull(emptyToNull(columns["cancellationDate"])),
    cancellationReason = emptyToNull(columns["cancellationReason"]),
    cancellationNotes = emptyToNull(columns["cancellationNotes"]),
  )

  override fun processRow(row: ApprovedPremisesBookingSeedCsvRow) {
    Thread.sleep(1_000)

    val existingBooking = bookingRepository.findByIdOrNull(row.id)

    if (existingBooking == null) {
      log.info("Creating new Booking: ${row.id}")
      createBooking(row)
    } else {
      log.info("Booking already exists - must cancel and recreate with new ID: ${row.id}")
    }
  }

  private fun createBooking(
    row: ApprovedPremisesBookingSeedCsvRow,
  ) {
    val offenderResult = communityApiClient.getOffenderDetailSummaryWithCall(row.crn)

    if (offenderResult is ClientResult.Failure) {
      offenderResult.throwException()
    }

    val offender = (offenderResult as ClientResult.Success).body

    if (offender.otherIds.nomsNumber == null) {
      throw RuntimeException("Offender does not have a NOMS number")
    }

    val bed = bedRepository.findByCode(row.bedCode)
      ?: throw RuntimeException("Bed with code ${row.bedCode} does not exist")

    val keyWorker = row.keyWorkerDeliusUsername?.let {
      extractResultFromClientResultOrThrow(
        communityApiClient.getStaffUserDetails(row.keyWorkerDeliusUsername),
      )
    }

    val departureReason = row.departureReason?.let {
      departureReasonRepository.findByNameAndServiceScope(row.departureReason, ServiceName.approvedPremises.value)
        ?: throw RuntimeException("Could not find Departure Reason with name '${row.departureReason}'")
    }

    val moveOnCategory = row.departureMoveOnCategory?.let {
      moveOnCategoryRepository.findByNameAndServiceScope(row.departureMoveOnCategory, ServiceName.approvedPremises.value)
        ?: throw RuntimeException("Could not find Move on Category with name '${row.departureMoveOnCategory}'")
    }

    val destinationProvider = row.departureDestinationProvider?.let {
      destinationProviderRepository.findByName(row.departureDestinationProvider)
        ?: throw RuntimeException("Could not find Destination Provider with name '${row.departureDestinationProvider}'")
    }

    val nonArrivalReason = row.nonArrivalReason?.let {
      nonArrivalReasonRepository.findByName(row.nonArrivalReason)
        ?: throw RuntimeException("Could not find Non Arrival Reason with name '${row.nonArrivalReason}'")
    }

    val cancellationReason = row.cancellationReason?.let {
      cancellationReasonRepository.findByNameAndServiceScope(row.cancellationReason, ServiceName.approvedPremises.value)
        ?: throw RuntimeException("Could not find Cancellation Reason with name '${row.cancellationReason}'")
    }

    val createdBooking = extractEntityFromNestedAuthorisableValidatableActionResult(
      result = bookingService.createApprovedPremisesAdHocBooking(
        user = null,
        crn = row.crn,
        nomsNumber = offender.otherIds.nomsNumber ?: "Unknown NOMS Number",
        arrivalDate = row.plannedArrivalDate,
        departureDate = row.plannedDepartureDate,
        premises = bed.room.premises,
        bedId = bed.id,
        bookingId = row.id,
        eventNumber = null,
      ),
    )

    log.info("Created Booking: ${createdBooking.id}")

    if (row.arrivalDate != null) {
      if (keyWorker == null) throw RuntimeException("If arrivalDate is provided, keyWorkerDeliusUsername must also be provided.")

      val createdArrival = extractEntityFromValidatableActionResult(
        when (createdBooking.premises) {
          is ApprovedPremisesEntity -> bookingService.createCas1Arrival(
            user = null,
            booking = createdBooking,
            arrivalDateTime = row.arrivalDate.toLocalDateTime().toInstant(),
            expectedDepartureDate = row.plannedDepartureDate,
            notes = row.arrivalNotes,
            keyWorkerStaffCode = keyWorker.staffCode,
          )
          is TemporaryAccommodationPremisesEntity -> bookingService.createCas3Arrival(
            user = null,
            booking = createdBooking,
            arrivalDate = row.arrivalDate,
            expectedDepartureDate = row.plannedDepartureDate,
            notes = row.arrivalNotes,
            keyWorkerStaffCode = keyWorker.staffCode,
          )
          else -> bookingService.createArrival(
            user = null,
            booking = createdBooking,
            arrivalDate = row.arrivalDate,
            expectedDepartureDate = row.plannedDepartureDate,
            notes = row.arrivalNotes,
            keyWorkerStaffCode = keyWorker.staffCode,
          )
        },
      )
      log.info("Created Arrival: ${createdArrival.id}")
    }

    if (row.departureDateTime != null) {
      if (row.arrivalDate == null) throw RuntimeException("If departureDateTime is provided, arrivalDate must also be provided.")
      if (departureReason == null) throw RuntimeException("If departureDateTime is provided, departureReason must also be provided")
      if (moveOnCategory == null) throw RuntimeException("If departureDateTime is provided, moveOnCategory must also be provided")
      if (destinationProvider == null) throw RuntimeException("If departureDateTime is provided, destinationProvider must also be provided")

      val createdDeparture = extractEntityFromValidatableActionResult(
        bookingService.createDeparture(
          user = null,
          booking = createdBooking,
          dateTime = row.departureDateTime,
          reasonId = departureReason.id,
          moveOnCategoryId = moveOnCategory.id,
          destinationProviderId = destinationProvider.id,
          notes = row.departureNotes,
        ),
      )

      log.info("Created Departure ${createdDeparture.id}")
    }

    if (row.nonArrivalDate != null) {
      if (nonArrivalReason == null) throw RuntimeException("If nonArrivalDate is provided, nonArrivalReason must also be provided")

      val createdNonArrival = extractEntityFromValidatableActionResult(
        bookingService.createNonArrival(
          user = null,
          booking = createdBooking,
          date = row.nonArrivalDate,
          reasonId = nonArrivalReason.id,
          notes = row.nonArrivalNotes,
        ),
      )

      log.info("Created Non Arrival ${createdNonArrival.id}")
    }

    if (row.cancellationDate != null) {
      if (cancellationReason == null) throw RuntimeException("If cancellationDate is provided, cancellationReason must also be provided")

      val createdCancellation = extractEntityFromCasResult(
        bookingService.createCas1Cancellation(
          booking = createdBooking,
          cancelledAt = row.cancellationDate,
          userProvidedReason = cancellationReason.id,
          notes = row.cancellationNotes,
          otherReason = null,
          withdrawalContext = WithdrawalContext(
            withdrawalTriggeredBy = WithdrawalTriggeredBySeedJob,
            triggeringEntityType = WithdrawableEntityType.Booking,
            triggeringEntityId = createdBooking.id,
          ),
        ),
      )

      log.info("Created Cancellation ${createdCancellation.id}")
    }
  }

  private fun emptyToNull(value: String?) = value?.ifBlank { null }
  private fun parseDateIfNotNull(date: String?) = date?.let { LocalDate.parse(it) }
  private fun parseDateTimeIfNotNull(date: String?) = date?.let { OffsetDateTime.parse(it) }
}

data class ApprovedPremisesBookingSeedCsvRow(
  val id: UUID,
  val crn: String,
  val plannedArrivalDate: LocalDate,
  val plannedDepartureDate: LocalDate,
  val keyWorkerDeliusUsername: String?,
  val bedCode: String,
  val arrivalDate: LocalDate?,
  val arrivalNotes: String?,
  val departureDateTime: OffsetDateTime?,
  val departureReason: String?,
  val departureDestinationProvider: String?,
  val departureMoveOnCategory: String?,
  val departureNotes: String?,
  val nonArrivalDate: LocalDate?,
  val nonArrivalReason: String?,
  val nonArrivalNotes: String?,
  val cancellationDate: LocalDate?,
  val cancellationReason: String?,
  val cancellationNotes: String?,
)
