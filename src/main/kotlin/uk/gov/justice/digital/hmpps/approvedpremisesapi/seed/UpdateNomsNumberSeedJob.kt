package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import java.util.UUID

class Cas1UpdateNomsNumberSeedJob(
  fileName: String,
  private val applicationRepository: ApplicationRepository,
  private val applicationTimelineNoteService: ApplicationTimelineNoteService,
  private val bookingRepository: BookingRepository,
) : SeedJob<UpdateNomsNumberSeedRow>(
  id = UUID.randomUUID(),
  fileName = fileName,
  requiredHeaders = setOf(
    "crn",
    "newNomsNumber",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = UpdateNomsNumberSeedRow(
    crn = columns["crn"]!!.trim(),
    newNomsNumber = columns["newNomsNumber"]!!.trim(),
  )

  override fun processRow(row: UpdateNomsNumberSeedRow) {
    val crn = row.crn
    val newNomsNumber = row.newNomsNumber
    log.info("Updating NOMS number for all applications with CRN $crn to $newNomsNumber")

    val applications = applicationRepository.findByCrn(crn)

    applications.forEach { application ->
      val applicationId = UUID.fromString(application.getId())
      val previousNomsNumber = application.getNomsNumber()

      log.info("Updating NOMS number on application $applicationId from $previousNomsNumber to $newNomsNumber")
      applicationRepository.updateNomsNumber(applicationId, newNomsNumber)
      applicationTimelineNoteService.saveApplicationTimelineNote(
        applicationId = applicationId,
        note = "NOMS Number for application updated from '$previousNomsNumber' to '$newNomsNumber' by Application Support",
        user = null,
      )
    }
    log.info("Have updated ${applications.size} applications")

    log.info("Updating NOMS number for all bookings with CRN $crn to $newNomsNumber")
    val bookingUpdatedCount = bookingRepository.updateNomsByCrn(crn, newNomsNumber)
    log.info("Have updated $bookingUpdatedCount bookings")
  }
}

data class UpdateNomsNumberSeedRow(
  val crn: String,
  val newNomsNumber: String,
)
