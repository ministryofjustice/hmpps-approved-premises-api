package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsRepository
import java.util.UUID
import javax.persistence.EntityManager

class Cas1UserDetailsMigrationJob(
  private val applicationRepository: ApplicationRepository,
  private val cas1UserDetailsRepository: Cas1ApplicationUserDetailsRepository,
  private val entityManager: EntityManager,
  private val pageSize: Int,
  private val transactionTemplate: TransactionTemplate,
) : MigrationJob() {
  override val shouldRunInTransaction = false

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun process() {
    var page = 1
    var hasNext = true
    var slice: Slice<ApprovedPremisesApplicationEntity>

    while (hasNext) {
      log.info("Getting page $page for max page size $pageSize")
      slice = applicationRepository.getSubmittedApprovedPremisesApplicationsWithoutApplicantUserDetails(PageRequest.of(0, pageSize))
      slice.content.forEach { application ->
        transactionTemplate.executeWithoutResult {
          try {
            populateUserDetails(application)
          } catch (e: IllegalStateException) {
            log.error(e.message)
          }
        }
      }
      entityManager.clear()
      hasNext = slice.hasNext()
      page += 1
    }
  }

  private fun populateUserDetails(application: ApprovedPremisesApplicationEntity) {
    log.info("Update contact details for application ${application.id}")

    val dataJson = application.data
    val jsonObj = JSONObject(dataJson)
    val dataMap = jsonObj.toMap()

    val basicInformationMap = dataMap["basic-information"] as Map<*, *>?
    val confirmYourDetailsMap = basicInformationMap?.get("confirm-your-details") as Map<*, *>?

    application.applicantUserDetails = cas1UserDetailsRepository.save(
      createApplicantUserDetailsEntity(application, confirmYourDetailsMap, application.applicantUserDetails?.id),
    )

    val applicantIsCaseManager = confirmYourDetailsMap == null ||
      !confirmYourDetailsMap.containsKey("caseManagementResponsibility") ||
      confirmYourDetailsMap["caseManagementResponsibility"] == "yes"

    application.caseManagerIsNotApplicant = !applicantIsCaseManager

    if (!applicantIsCaseManager) {
      application.caseManagerUserDetails = cas1UserDetailsRepository.save(
        createCaseManagerUserDetails(basicInformationMap, application, application.caseManagerUserDetails?.id),
      )
    } else {
      application.caseManagerUserDetails = null
    }

    applicationRepository.save(application)

    log.info("Contact details updated for application ${application.id}. Applicant is case manager? $applicantIsCaseManager")
  }

  private fun createCaseManagerUserDetails(
    basicInformationMap: Map<*, *>?,
    application: ApprovedPremisesApplicationEntity,
    existingId: UUID?,
  ): Cas1ApplicationUserDetailsEntity {
    val caseManagerDetails = basicInformationMap?.get("case-manager-information") as Map<*, *>?

    val name = caseManagerDetails?.get("name") as String?
      ?: error("caseManagementResponsibility is 'no', but no value defined for 'case-manager-information/name' for application ${application.id}")

    val emailAddress = caseManagerDetails?.get("emailAddress") as String?
      ?: error("caseManagementResponsibility is 'no', but no value defined for 'case-manager-information/emailAddress' for application ${application.id}")

    val telephoneNumber = caseManagerDetails?.get("phoneNumber") as String?
      ?: error("caseManagementResponsibility is 'no', but no value defined for 'case-manager-information/phoneNumber' for application ${application.id}")

    return Cas1ApplicationUserDetailsEntity(
      existingId ?: UUID.randomUUID(),
      name = name,
      email = emailAddress,
      telephoneNumber = telephoneNumber,
    )
  }

  private fun createApplicantUserDetailsEntity(
    application: ApprovedPremisesApplicationEntity,
    confirmYourDetailsMap: Map<*, *>?,
    existingId: UUID?,
  ): Cas1ApplicationUserDetailsEntity {
    val applicantUserEntity = application.createdByUser
    val detailsUptoDateMap = confirmYourDetailsMap?.get("detailsToUpdate") as List<*>?

    val applicantNameOverridden = detailsUptoDateMap?.contains("name") ?: false
    val applicantEmailOverridden = detailsUptoDateMap?.contains("emailAddress") ?: false
    val applicantPhoneOverridden = detailsUptoDateMap?.contains("phoneNumber") ?: false

    val applicantName = if (applicantNameOverridden) {
      if (confirmYourDetailsMap!!.contains("name")) {
        confirmYourDetailsMap["name"]!! as String
      } else {
        error("applicant name marked as overridden, but no value defined for 'confirm-your-details/name' for application ${application.id}")
      }
    } else {
      applicantUserEntity.name
    }

    val applicantEmail = if (applicantEmailOverridden && confirmYourDetailsMap!!.contains("emailAddress")) {
      confirmYourDetailsMap["emailAddress"]!! as String
    } else {
      applicantUserEntity.email
    }

    val applicantPhoneNumber = if (applicantPhoneOverridden && confirmYourDetailsMap!!.contains("phoneNumber")) {
      confirmYourDetailsMap["phoneNumber"]!! as String
    } else {
      applicantUserEntity.telephoneNumber
    }

    return Cas1ApplicationUserDetailsEntity(
      existingId ?: UUID.randomUUID(),
      applicantName,
      applicantEmail,
      applicantPhoneNumber,
    )
  }
}
