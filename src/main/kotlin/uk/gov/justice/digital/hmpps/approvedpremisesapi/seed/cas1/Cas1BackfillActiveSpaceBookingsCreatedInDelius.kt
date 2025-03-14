package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Before this job is ran, corresponding referrals must be imported from delius into the
 * 'cas1_delius_booking_import' table using the [Cas1ImportDeliusReferralsSeedJob] job
 */
@Service
class Cas1BackfillActiveSpaceBookingsCreatedInDelius(
  private val approvedPremisesRepository: ApprovedPremisesRepository,
  private val cas1DeliusBookingImportRepository: Cas1DeliusBookingImportRepository,
  private val cas1BookingManagementInfoService: Cas1BookingManagementInfoService,
  private val offenderService: OffenderService,
  private val offlineApplicationRepository: OfflineApplicationRepository,
  private val spaceBookingRepository: Cas1SpaceBookingRepository,
  private val transactionTemplate: TransactionTemplate,
) : SeedJob<Cas1CreateMissingReferralsSeedCsvRow>(
  requiredHeaders = setOf(
    "q_code",
  ),
  runInTransaction = false,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1CreateMissingReferralsSeedCsvRow(
    qCode = columns["q_code"]!!.trim(),
  )

  override fun processRow(row: Cas1CreateMissingReferralsSeedCsvRow) {
    transactionTemplate.executeWithoutResult {
      migratePremise(row.qCode)
    }
  }

  @SuppressWarnings("TooGenericExceptionCaught", "MagicNumber")
  private fun migratePremise(qCode: String) {
    val premises = approvedPremisesRepository.findByQCode(qCode) ?: error("Premises with qcode $qCode not found")

    if (!premises.supportsSpaceBookings) {
      error("premise ${premises.name} doesn't support space bookings, can't migrate bookings")
    }

    val referrals = cas1DeliusBookingImportRepository.findActiveBookingsCreatedInDelius(
      qCode = qCode,
      minExpectedDepartureDate = LocalDate.of(2025, 1, 1),
      maxExpectedDepartureDate = LocalDate.of(2035, 1, 1),
    )

    log.info("Will create ${referrals.size} space bookings for premise ${premises.name} ($qCode)")

    if (referrals.isEmpty()) {
      return
    }

    val crnToName = offenderService.getPersonSummaryInfoResultsInBatches(
      crns = referrals.map { it.crn }.toSet(),
      laoStrategy = LaoStrategy.NeverRestricted,
    ).associate { personSummaryInfoResult ->
      val crn = personSummaryInfoResult.crn
      when (personSummaryInfoResult) {
        is PersonSummaryInfoResult.Success.Full -> {
          val name = personSummaryInfoResult.summary.name
          crn to "${name.forename} ${name.surname}"
        }
        is PersonSummaryInfoResult.NotFound,
        is PersonSummaryInfoResult.Success.Restricted,
        is PersonSummaryInfoResult.Unknown,
        -> {
          log.warn(
            "Could not find offender for CRN $crn, " +
              "result was ${personSummaryInfoResult::class}. Will not populate name",
          )
          crn to null
        }
      }
    }

    referrals.forEach {
      createSpaceBooking(
        premises = premises,
        deliusReferral = it,
        crnToName = crnToName,
      )
    }

    log.info("Have created ${referrals.size} space bookings for premise ${premises.name} ($qCode)")
  }

  private fun createSpaceBooking(
    premises: ApprovedPremisesEntity,
    deliusReferral: Cas1DeliusBookingImportEntity,
    crnToName: Map<String, String?>,
  ) {
    val crn = deliusReferral.crn

    if (deliusReferral.expectedDepartureDate == null) {
      log.warn("No expected departure date defined for crn $crn with arrival ${deliusReferral.arrivalDate}")
      return
    }

    val offlineApplication = offlineApplicationRepository.save(
      OfflineApplicationEntity(
        id = UUID.randomUUID(),
        crn = crn,
        service = ServiceName.approvedPremises.value,
        createdAt = OffsetDateTime.now(),
        eventNumber = deliusReferral.eventNumber,
        name = crnToName[crn],
      ),
    )

    val managementInfo = cas1BookingManagementInfoService.fromDeliusBookingImport(deliusReferral)

    spaceBookingRepository.save(
      Cas1SpaceBookingEntity(
        id = UUID.randomUUID(),
        premises = premises,
        application = null,
        offlineApplication = offlineApplication,
        placementRequest = null,
        createdBy = null,
        createdAt = OffsetDateTime.now(),
        expectedArrivalDate = deliusReferral.expectedArrivalDate,
        expectedDepartureDate = deliusReferral.expectedDepartureDate!!,
        actualArrivalDate = managementInfo.arrivedAtDate,
        actualArrivalTime = managementInfo.arrivedAtTime,
        actualDepartureDate = null,
        actualDepartureTime = null,
        canonicalArrivalDate = managementInfo.arrivedAtDate ?: deliusReferral.expectedArrivalDate,
        canonicalDepartureDate = deliusReferral.expectedDepartureDate!!,
        crn = deliusReferral.crn,
        keyWorkerStaffCode = managementInfo.keyWorkerStaffCode,
        keyWorkerName = managementInfo.keyWorkerName,
        keyWorkerAssignedAt = null,
        cancellationOccurredAt = null,
        cancellationRecordedAt = null,
        cancellationReason = null,
        cancellationReasonNotes = null,
        departureMoveOnCategory = null,
        departureReason = null,
        departureNotes = null,
        criteria = emptyList<CharacteristicEntity>().toMutableList(),
        nonArrivalReason = null,
        nonArrivalConfirmedAt = null,
        nonArrivalNotes = null,
        deliusEventNumber = deliusReferral.eventNumber,
        migratedManagementInfoFrom = managementInfo.source,
        deliusId = deliusReferral.approvedPremisesReferralId,
      ),
    )
  }
}

data class Cas1CreateMissingReferralsSeedCsvRow(
  val qCode: String,
)
