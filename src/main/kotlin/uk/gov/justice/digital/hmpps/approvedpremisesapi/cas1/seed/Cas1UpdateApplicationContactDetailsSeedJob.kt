package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DiffUtils
import java.util.UUID

@Service
class Cas1UpdateApplicationContactDetailsSeedJob(
  val applicationRepository: ApprovedPremisesApplicationRepository,
  val applicationUserDetailsRepository: Cas1ApplicationUserDetailsRepository,
  val cas1ApplicationTimelineNoteService: Cas1ApplicationTimelineNoteService,
) : SeedJob<Cas1UpdateApplicationContactDetailsSeedJobCsvRow>(
  requiredHeaders = setOf(
    "application_id",
    "contact_id",
    "contact_role",
    "name",
    "email",
    "telephone_number",
  ),
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun processRow(row: Cas1UpdateApplicationContactDetailsSeedJobCsvRow) {
    val applicationId = row.applicationId
    val contactRole = row.contactRole
    val contactId = row.contactId

    log.info("Updating contact details of type $contactRole for application $applicationId")

    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: error("Application with identifier '$applicationId' does not exist")

    val contact = when (contactRole) {
      ContactRole.APPLICANT -> application.applicantUserDetails
      ContactRole.CASE_MANAGER -> application.caseManagerUserDetails
    }

    if (contact == null) {
      error("There is no contact of type $contactRole for application $applicationId")
    }

    if (contact.id != contactId) {
      error("Contact ID for application is $contactId but CSV stated ${contact.id}")
    }

    val beforeChange = contact.toSimplifiedContactDetails()

    contact.name = row.name
    contact.email = row.email
    contact.telephoneNumber = row.telephoneNumber

    applicationUserDetailsRepository.save(contact)

    val changes = DiffUtils.simplePrettyPrint(beforeChange, contact.toSimplifiedContactDetails())

    log.info("Changes are $changes")

    if (changes != null) {
      val updateType = when (contactRole) {
        ContactRole.APPLICANT -> "applicant"
        ContactRole.CASE_MANAGER -> "case manager"
      }

      cas1ApplicationTimelineNoteService.saveApplicationTimelineNote(
        applicationId = row.applicationId,
        note = "Application Support have updated the $updateType contact details. $changes",
        user = null,
      )
    }
  }

  override fun deserializeRow(columns: Map<String, String>): Cas1UpdateApplicationContactDetailsSeedJobCsvRow {
    val seedColumns = SeedColumns(columns)
    return Cas1UpdateApplicationContactDetailsSeedJobCsvRow(
      applicationId = seedColumns.getUuidOrNull("application_id")!!,
      contactId = seedColumns.getUuidOrNull("contact_id")!!,
      contactRole = ContactRole.valueOf(seedColumns.getStringOrNull("contact_role")!!),
      name = seedColumns.getStringOrNull("name")!!,
      email = seedColumns.getStringOrNull("email"),
      telephoneNumber = seedColumns.getStringOrNull("telephone_number"),
    )
  }
}

private data class SimplifiedContactDetails(
  val name: String,
  val email: String?,
  val telephoneNumber: String?,
)

private fun Cas1ApplicationUserDetailsEntity.toSimplifiedContactDetails() = SimplifiedContactDetails(
  name = this.name,
  email = this.email,
  telephoneNumber = this.telephoneNumber,
)

data class Cas1UpdateApplicationContactDetailsSeedJobCsvRow(
  val applicationId: UUID,
  val contactId: UUID,
  val contactRole: ContactRole,
  var name: String,
  val email: String?,
  val telephoneNumber: String?,
)

enum class ContactRole {
  APPLICANT,
  CASE_MANAGER,
}
