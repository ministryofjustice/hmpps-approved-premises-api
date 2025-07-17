package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService

class SeedCas1UpdateApplicationContactDetailsTest : SeedTestBase() {

  @Autowired
  lateinit var domainEventService: Cas1DomainEventService

  @Test
  fun `Update applicant details, leave manager as-is`() {
    val applicantUserDetails = cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
      withName("Old applicant name")
      withEmailAddress("Old applicant email")
      withTelephoneNumber("Old applicant tel")
    }

    val caseManagerUserDetails = cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
      withName("Old case manager name")
      withEmailAddress("Old case manager email")
      withTelephoneNumber("Old case manager tel")
    }

    val application = givenACas1Application(
      applicant = applicantUserDetails,
      caseManager = caseManagerUserDetails,
    )

    val applicationId = application.id

    seed(
      SeedFileType.approvedPremisesUpdateApplicationContactDetails,
      """
        application_id,contact_id,contact_role,name,email,telephone_number
        $applicationId, ${applicantUserDetails.id}, APPLICANT, New applicant name, New applicant email, New applicant tel
      """.trimIndent(),
    )

    val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
    val updatedApplicantContactDetails = updatedApplication.applicantUserDetails!!
    assertThat(updatedApplicantContactDetails.name).isEqualTo("New applicant name")
    assertThat(updatedApplicantContactDetails.email).isEqualTo("New applicant email")
    assertThat(updatedApplicantContactDetails.telephoneNumber).isEqualTo("New applicant tel")

    val updatedCaseManagerContactDetails = updatedApplication.caseManagerUserDetails!!
    assertThat(updatedCaseManagerContactDetails.name).isEqualTo("Old case manager name")
    assertThat(updatedCaseManagerContactDetails.email).isEqualTo("Old case manager email")
    assertThat(updatedCaseManagerContactDetails.telephoneNumber).isEqualTo("Old case manager tel")

    val notes = applicationTimelineNoteRepository.findApplicationTimelineNoteEntitiesByApplicationIdAndDeletedAtIsNull(application.id)
    assertThat(notes).hasSize(1)
    assertThat(notes)
      .extracting("body")
      .contains(
        "Application Support have updated the applicant contact details." +
          " 'email' changed: 'Old applicant email' -> 'New applicant email'," +
          " 'name' changed: 'Old applicant name' -> 'New applicant name'," +
          " 'telephoneNumber' changed: 'Old applicant tel' -> 'New applicant tel'",
      )
  }

  @Test
  fun `Update case manager, leave applicant as-is`() {
    val applicantUserDetails = cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
      withName("Old applicant name")
      withEmailAddress("Old applicant email")
      withTelephoneNumber("Old applicant tel")
    }

    val caseManagerUserDetails = cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
      withName("Old case manager name")
      withEmailAddress("Old case manager email")
      withTelephoneNumber("Old case manager tel")
    }

    val application = givenACas1Application(
      applicant = applicantUserDetails,
      caseManager = caseManagerUserDetails,
    )

    val applicationId = application.id

    seed(
      SeedFileType.approvedPremisesUpdateApplicationContactDetails,
      """
        application_id,contact_id,contact_role,name,email,telephone_number
        $applicationId, ${caseManagerUserDetails.id}, CASE_MANAGER, New case manager name, New case manager email, New case manager tel
      """.trimIndent(),
    )

    val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
    val updatedApplicantContactDetails = updatedApplication.applicantUserDetails!!
    assertThat(updatedApplicantContactDetails.name).isEqualTo("Old applicant name")
    assertThat(updatedApplicantContactDetails.email).isEqualTo("Old applicant email")
    assertThat(updatedApplicantContactDetails.telephoneNumber).isEqualTo("Old applicant tel")

    val updatedCaseManagerContactDetails = updatedApplication.caseManagerUserDetails!!
    assertThat(updatedCaseManagerContactDetails.name).isEqualTo("New case manager name")
    assertThat(updatedCaseManagerContactDetails.email).isEqualTo("New case manager email")
    assertThat(updatedCaseManagerContactDetails.telephoneNumber).isEqualTo("New case manager tel")

    val notes = applicationTimelineNoteRepository.findApplicationTimelineNoteEntitiesByApplicationIdAndDeletedAtIsNull(application.id)
    assertThat(notes).hasSize(1)
    assertThat(notes)
      .extracting("body")
      .contains(
        "Application Support have updated the case manager contact details." +
          " 'email' changed: 'Old case manager email' -> 'New case manager email'," +
          " 'name' changed: 'Old case manager name' -> 'New case manager name'," +
          " 'telephoneNumber' changed: 'Old case manager tel' -> 'New case manager tel'",
      )
  }

  @Test
  fun `No changes, do nothing`() {
    val applicantUserDetails = cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
      withName("Old applicant name")
      withEmailAddress("Old applicant email")
      withTelephoneNumber("Old applicant tel")
    }

    val caseManagerUserDetails = cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
      withName("Old case manager name")
      withEmailAddress("Old case manager email")
      withTelephoneNumber("Old case manager tel")
    }

    val application = givenACas1Application(
      applicant = applicantUserDetails,
      caseManager = caseManagerUserDetails,
    )

    val applicationId = application.id

    seed(
      SeedFileType.approvedPremisesUpdateApplicationContactDetails,
      """
        application_id,contact_id,contact_role,name,email,telephone_number
        $applicationId, ${applicantUserDetails.id}, APPLICANT, Old applicant name, Old applicant email, Old applicant tel
      """.trimIndent(),
    )

    val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
    val updatedApplicantContactDetails = updatedApplication.applicantUserDetails!!
    assertThat(updatedApplicantContactDetails.name).isEqualTo("Old applicant name")
    assertThat(updatedApplicantContactDetails.email).isEqualTo("Old applicant email")
    assertThat(updatedApplicantContactDetails.telephoneNumber).isEqualTo("Old applicant tel")

    val updatedCaseManagerContactDetails = updatedApplication.caseManagerUserDetails!!
    assertThat(updatedCaseManagerContactDetails.name).isEqualTo("Old case manager name")
    assertThat(updatedCaseManagerContactDetails.email).isEqualTo("Old case manager email")
    assertThat(updatedCaseManagerContactDetails.telephoneNumber).isEqualTo("Old case manager tel")

    val notes = applicationTimelineNoteRepository.findApplicationTimelineNoteEntitiesByApplicationIdAndDeletedAtIsNull(application.id)
    assertThat(notes).isEmpty()
  }
}
