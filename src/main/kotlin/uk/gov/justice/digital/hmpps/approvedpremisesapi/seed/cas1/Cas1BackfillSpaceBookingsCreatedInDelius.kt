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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Before this job is ran, corresponding referrals must be imported from delius into the
 * 'cas1_delius_booking_import' table using the [Cas1ImportDeliusReferralsSeedJob] job
 */
@Service
class Cas1BackfillSpaceBookingsCreatedInDelius(
  private val approvedPremisesRepository: ApprovedPremisesRepository,
  private val cas1DeliusBookingImportRepository: Cas1DeliusBookingImportRepository,
  private val cas1BookingManagementInfoService: Cas1BookingManagementInfoService,
  private val environmentService: EnvironmentService,
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

  override fun preSeed() {
    if (environmentService.isProd()) {
      error("Cannot run seed job in prod")
    }
  }

  override fun processRow(row: Cas1CreateMissingReferralsSeedCsvRow) {
    transactionTemplate.executeWithoutResult {
      migratePremise(row.qCode)
    }
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun migratePremise(qCode: String) {
    val premises = approvedPremisesRepository.findByQCode(qCode) ?: error("Premises with qcode $qCode not found")

    if (!premises.supportsSpaceBookings) {
      error("premise ${premises.name} doesn't support space bookings, can't migrate bookings")
    }

    val referrals = cas1DeliusBookingImportRepository.findByBookingIdIsNullAndPremisesQcode(qCode)

    log.info("Will create ${referrals.size} space bookings for premise ${premises.name} ($qCode)")

    if (referrals.isEmpty()) {
      return
    }

    // TODO: we need a version of this that will segment calls to the backend
    // can use for CAS3 report that does that?
    val crnToName = offenderService.getPersonSummaryInfoResults(
      crns = referrals.map { it.crn }.toSet(),
      limitedAccessStrategy = OffenderService.LimitedAccessStrategy.IgnoreLimitedAccess,
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
        actualDepartureDate = managementInfo.departedAtDate,
        actualDepartureTime = managementInfo.departedAtTime,
        canonicalArrivalDate = managementInfo.arrivedAtDate ?: deliusReferral.expectedArrivalDate,
        canonicalDepartureDate = managementInfo.departedAtDate ?: deliusReferral.expectedDepartureDate!!,
        crn = deliusReferral.crn,
        keyWorkerStaffCode = managementInfo.keyWorkerStaffCode,
        keyWorkerName = managementInfo.keyWorkerName,
        keyWorkerAssignedAt = null,
        cancellationOccurredAt = null,
        cancellationRecordedAt = null,
        cancellationReason = null,
        cancellationReasonNotes = null,
        departureMoveOnCategory = managementInfo.departureMoveOnCategory,
        departureReason = managementInfo.departureReason,
        departureNotes = managementInfo.departureNotes,
        criteria = emptyList<CharacteristicEntity>().toMutableList(),
        nonArrivalReason = managementInfo.nonArrivalReason,
        nonArrivalConfirmedAt = managementInfo.nonArrivalConfirmedAt?.toInstant(),
        nonArrivalNotes = managementInfo.nonArrivalNotes,
        deliusEventNumber = deliusReferral.eventNumber,
        migratedManagementInfoFrom = managementInfo.source,
      ),
    )
  }
}

data class Cas1CreateMissingReferralsSeedCsvRow(
  val qCode: String,
)
