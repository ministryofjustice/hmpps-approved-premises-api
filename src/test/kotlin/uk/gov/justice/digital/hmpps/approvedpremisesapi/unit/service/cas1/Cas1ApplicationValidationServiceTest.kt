package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.TierService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationValidationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1ApplicationValidationServiceTest {

  @MockK
  private lateinit var applicationRepository: ApprovedPremisesApplicationRepository

  @MockK
  private lateinit var tierService: TierService

  @InjectMockKs
  private lateinit var service: Cas1ApplicationValidationService

  @Nested
  inner class ValidateSubmission {
    val applicationId: UUID = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"
    val apArea = ApAreaEntityFactory().produce()
    val user = UserEntityFactory()
      .withDefaults()
      .withDeliusUsername(this.username)
      .withApArea(apArea)
      .produce()

    private var defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
      translatedDocument = {},
      isWomensApplication = false,
      isEmergencyApplication = false,
      targetLocation = "SW1A 1AA",
      releaseType = ReleaseTypeOption.licence,
      type = "CAS1",
      sentenceType = SentenceTypeOption.nonStatutory,
      applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
      caseManagerIsNotApplicant = false,
      apType = ApType.normal,
      requestedPlacementDuration = 10,
      requestedPlacementPeriod = null,
    )

    @Test
    fun `Returns NotFound when application doesn't exist`() {
      every { applicationRepository.findByIdOrNull(applicationId) } returns null

      assertThatCasResult(
        service.validateApplicationSubmission(
          applicationId,
          user,
          defaultSubmitApprovedPremisesApplication,
        ),
      ).isNotFound(expectedEntityType = "ApprovedPremisesApplicationEntity", expectedId = applicationId)
    }

    @Test
    fun `Returns Unauthorised when application doesn't belong to request user`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withYieldedCreatedByUser { UserEntityFactory().withDefaultProbationRegion().produce() }
        .produce()

      every { applicationRepository.findByIdOrNull(applicationId) } returns application

      assertThatCasResult(
        service.validateApplicationSubmission(
          applicationId,
          user,
          defaultSubmitApprovedPremisesApplication,
        ),
      ).isUnauthorised()
    }

    @Test
    fun `Returns GeneralValidationError when application has already been submitted`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      every { applicationRepository.findByIdOrNull(applicationId) } returns application

      val result = service.validateApplicationSubmission(
        applicationId,
        user,
        defaultSubmitApprovedPremisesApplication.copy(
          requestedPlacementDuration = 10,
          requestedPlacementPeriod = null,
        ),
      )

      assertThatCasResult(result).isGeneralValidationError("This application has already been submitted")
    }

    @EnumSource(
      value = ApprovedPremisesApplicationStatus::class,
      mode = EnumSource.Mode.EXCLUDE,
      names = [ "STARTED" ],
    )
    @ParameterizedTest
    fun `Returns GeneralValidationError when application doesn't have status 'STARTED'`(state: ApprovedPremisesApplicationStatus) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withStatus(state)
        .produce()

      every { applicationRepository.findByIdOrNull(applicationId) } returns application

      val result = service.validateApplicationSubmission(
        applicationId,
        user,
        defaultSubmitApprovedPremisesApplication.copy(
          requestedPlacementDuration = 10,
          requestedPlacementPeriod = null,
        ),
      )

      assertThatCasResult(result).isGeneralValidationError("Only an application with the 'STARTED' status can be submitted")
    }

    @Test
    fun `Returns field validation error when data is not defined`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withData(null)
        .produce()

      every { applicationRepository.findByIdOrNull(applicationId) } returns application

      val submitApplication = defaultSubmitApprovedPremisesApplication.copy(
        duration = null,
        requestedPlacementDuration = null,
        requestedPlacementPeriod = Cas1RequestedPlacementPeriod(
          arrival = LocalDate.now(),
          duration = 11,
          arrivalFlexible = null,
        ),
      )

      val result = service.validateApplicationSubmission(
        applicationId,
        user,
        submitApplication,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.data", "empty")
    }

    @Test
    fun `Returns GeneralValidationError when tier v2 is in use and duration and requestedPlacementDuration are not populated`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()

      every { applicationRepository.findByIdOrNull(applicationId) } returns application
      every { tierService.useTierV2() } returns true

      val submitApplication = defaultSubmitApprovedPremisesApplication.copy(
        duration = null,
        requestedPlacementDuration = null,
        requestedPlacementPeriod = Cas1RequestedPlacementPeriod(
          arrival = LocalDate.now(),
          duration = 11,
          arrivalFlexible = null,
        ),
      )

      val result = service.validateApplicationSubmission(
        applicationId,
        user,
        submitApplication,
      )

      assertThatCasResult(result).isGeneralValidationError("Either duration or requestedPlacementDuration should be provided")
    }

    @Test
    fun `Returns GeneralValidationError when duration does not match requestedPlacementPeriod duration`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()

      every { applicationRepository.findByIdOrNull(applicationId) } returns application
      every { tierService.useTierV2() } returns true

      val submitApplication = defaultSubmitApprovedPremisesApplication.copy(
        duration = 10,
        requestedPlacementPeriod = Cas1RequestedPlacementPeriod(
          arrival = LocalDate.now(),
          duration = 11,
          arrivalFlexible = null,
        ),
      )

      val result = service.validateApplicationSubmission(
        applicationId,
        user,
        submitApplication,
      )

      assertThatCasResult(result).isGeneralValidationError("The requested placement period duration must match the duration specified in the application.")
    }

    @Test
    fun `Returns GeneralValidationError when requestedPlacementDuration does not match requestedPlacementPeriod duration`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()

      every { applicationRepository.findByIdOrNull(applicationId) } returns application
      every { tierService.useTierV2() } returns true

      val submitApplication = defaultSubmitApprovedPremisesApplication.copy(
        requestedPlacementDuration = 10,
        requestedPlacementPeriod = Cas1RequestedPlacementPeriod(
          arrival = LocalDate.now(),
          duration = 11,
          arrivalFlexible = null,
        ),
      )

      val result = service.validateApplicationSubmission(
        applicationId,
        user,
        submitApplication,
      )

      assertThatCasResult(result).isGeneralValidationError("The requested placement period duration must match the duration specified in the application.")
    }

    @Test
    fun `Returns GeneralValidationError when applicantIsNotCaseManager is true and no case manager details are provided`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()

      every { applicationRepository.findByIdOrNull(applicationId) } returns application

      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        apType = ApType.normal,
        isWomensApplication = false,
        isEmergencyApplication = false,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
        caseManagerIsNotApplicant = true,
        requestedPlacementDuration = 10,
      )

      val result = service.validateApplicationSubmission(
        applicationId,
        user,
        defaultSubmitApprovedPremisesApplication.copy(
          requestedPlacementDuration = 10,
          requestedPlacementPeriod = null,
        ),
      )

      assertThatCasResult(result).isGeneralValidationError("caseManagerUserDetails must be provided if caseManagerIsNotApplicant is true")
    }

    @Test
    fun `Returns Success with the application if no validation issues, tier v2`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()

      every { applicationRepository.findByIdOrNull(applicationId) } returns application
      every { tierService.useTierV2() } returns true

      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        apType = ApType.normal,
        isWomensApplication = false,
        isEmergencyApplication = false,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
        caseManagerIsNotApplicant = false,
        requestedPlacementDuration = 10,
      )

      val result = service.validateApplicationSubmission(
        applicationId,
        user,
        defaultSubmitApprovedPremisesApplication.copy(
          requestedPlacementDuration = 10,
          requestedPlacementPeriod = null,
        ),
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isEqualTo(application)
      }
    }

    @Test
    fun `Returns Success with the application if no validation issues, tier v3 with non-null duration`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()

      every { applicationRepository.findByIdOrNull(applicationId) } returns application
      every { tierService.useTierV2() } returns false

      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        apType = ApType.normal,
        isWomensApplication = false,
        isEmergencyApplication = false,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
        caseManagerIsNotApplicant = false,
        requestedPlacementDuration = 10,
        requestedPlacementPeriod = Cas1RequestedPlacementPeriod(
          arrival = LocalDate.now(),
          arrivalFlexible = null,
          duration = 10,
        ),
      )

      val result = service.validateApplicationSubmission(
        applicationId,
        user,
        defaultSubmitApprovedPremisesApplication.copy(
          requestedPlacementDuration = 10,
          requestedPlacementPeriod = null,
        ),
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isEqualTo(application)
      }
    }

    @Test
    fun `Returns Success with the application if no validation issues, tier v3 with null duration`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()

      every { applicationRepository.findByIdOrNull(applicationId) } returns application
      every { tierService.useTierV2() } returns false

      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        apType = ApType.normal,
        isWomensApplication = false,
        isEmergencyApplication = false,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
        caseManagerIsNotApplicant = false,
        requestedPlacementDuration = null,
        requestedPlacementPeriod = Cas1RequestedPlacementPeriod(
          arrival = LocalDate.now(),
          arrivalFlexible = null,
          duration = null,
        ),
      )

      val result = service.validateApplicationSubmission(
        applicationId,
        user,
        defaultSubmitApprovedPremisesApplication.copy(
          requestedPlacementDuration = 10,
          requestedPlacementPeriod = null,
        ),
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isEqualTo(application)
      }
    }
  }
}
