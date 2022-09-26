package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationOfficerEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationOfficerRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ProbationOfficerService
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationServiceTest {
  private val mockProbationOfficerRepository = mockk<ProbationOfficerRepository>()
  private val mockApplicationRepository = mockk<ApplicationRepository>()
  private val mockJsonSchemaService = mockk<JsonSchemaService>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockProbationOfficerService = mockk<ProbationOfficerService>()

  private val applicationService = ApplicationService(
    mockProbationOfficerRepository,
    mockApplicationRepository,
    mockJsonSchemaService,
    mockOffenderService,
    mockProbationOfficerService
  )

  @Test
  fun `Get all applications where Probation Officer with provided distinguished name does not exist returns empty list`() {
    val distinguishedName = "SOMEPERSON"

    every { mockProbationOfficerRepository.findByDistinguishedName(distinguishedName) } returns null

    assertThat(applicationService.getAllApplicationsForUsername(distinguishedName)).isEmpty()
  }

  @Test
  fun `Get all applications where Probation Officer exists returns applications returned from repository`() {
    val newestJsonSchema = ApplicationSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    val probationOfficerId = UUID.fromString("8a0624b8-8e92-47ce-b645-b65ea5a197d0")
    val distinguishedName = "SOMEPERSON"
    val probationOfficerEntity = ProbationOfficerEntityFactory()
      .withId(probationOfficerId)
      .withDistinguishedName(distinguishedName)
      .produce()
    val applicationEntities = listOf(
      ApplicationEntityFactory()
        .withCreatedByProbationOfficer(probationOfficerEntity)
        .withApplicationSchema(newestJsonSchema)
        .produce(),
      ApplicationEntityFactory()
        .withCreatedByProbationOfficer(probationOfficerEntity)
        .withApplicationSchema(newestJsonSchema)
        .produce(),
      ApplicationEntityFactory()
        .withCreatedByProbationOfficer(probationOfficerEntity)
        .withApplicationSchema(newestJsonSchema)
        .produce()
    )

    every { mockProbationOfficerRepository.findByDistinguishedName(distinguishedName) } returns probationOfficerEntity
    every { mockApplicationRepository.findAllByCreatedByProbationOfficer_Id(probationOfficerId) } returns applicationEntities
    every { mockJsonSchemaService.attemptSchemaUpgrade(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    assertThat(applicationService.getAllApplicationsForUsername(distinguishedName)).containsAll(applicationEntities)
  }

  @Test
  fun `getApplicationForUsername where application does not exist returns NotFound result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

    assertThat(applicationService.getApplicationForUsername(applicationId, distinguishedName) is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getApplicationForUsername where application does not belong to user returns Unauthorised result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    every { mockProbationOfficerRepository.findByDistinguishedName(distinguishedName) } returns ProbationOfficerEntityFactory().produce()
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns ApplicationEntityFactory()
      .withCreatedByProbationOfficer(ProbationOfficerEntityFactory().produce())
      .produce()

    assertThat(applicationService.getApplicationForUsername(applicationId, distinguishedName) is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getApplicationForUsername where application belongs to user returns Success result with entity from db`() {
    val distinguishedName = "SOMEPERSON"
    val probationOfficerId = UUID.fromString("239b5e41-f83e-409e-8fc0-8f1e058d417e")
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    val newestJsonSchema = ApplicationSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    val probationOfficerEntity = ProbationOfficerEntityFactory()
      .withId(probationOfficerId)
      .withDistinguishedName(distinguishedName)
      .produce()

    val applicationEntity = ApplicationEntityFactory()
      .withCreatedByProbationOfficer(probationOfficerEntity)
      .withApplicationSchema(newestJsonSchema)
      .produce()

    every { mockJsonSchemaService.attemptSchemaUpgrade(any()) } answers { it.invocation.args[0] as ApplicationEntity }
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns applicationEntity
    every { mockProbationOfficerRepository.findByDistinguishedName(distinguishedName) } returns probationOfficerEntity

    val result = applicationService.getApplicationForUsername(applicationId, distinguishedName)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity).isEqualTo(applicationEntity)
  }

  @Test
  fun `createApplication returns FieldValidationError when CRN does not exist`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.NotFound()

    val result = applicationService.createApplication(crn, username)

    assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
    result as ValidatableActionResult.FieldValidationError
    assertThat(result.validationMessages).containsEntry("$.crn", "This CRN does not exist")
  }

  @Test
  fun `createApplication returns FieldValidationError when CRN is LAO restricted`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Unauthorised()

    val result = applicationService.createApplication(crn, username)

    assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
    result as ValidatableActionResult.FieldValidationError
    assertThat(result.validationMessages).containsEntry("$.crn", "You do not have permission to access this CRN")
  }

  @Test
  fun `createApplication returns Success with created Application`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    val probationOfficer = ProbationOfficerEntityFactory().produce()
    val schema = ApplicationSchemaEntityFactory().produce()

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce()
    )
    every { mockProbationOfficerService.getProbationOfficerForRequestUser() } returns probationOfficer
    every { mockJsonSchemaService.getNewestSchema() } returns schema
    every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    val result = applicationService.createApplication(crn, username)

    assertThat(result is ValidatableActionResult.Success).isTrue
    result as ValidatableActionResult.Success
    assertThat(result.entity).matches {
      it.crn == crn &&
        it.createdByProbationOfficer == probationOfficer
    }
  }

  @Test
  fun `updateApplication returns NotFound when application doesn't exist`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

    assertThat(applicationService.updateApplication(applicationId, "{}", null, username) is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `updateApplication returns Unauthorised when application doesn't belong to request user`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    every { mockProbationOfficerService.getProbationOfficerForRequestUser() } returns ProbationOfficerEntityFactory()
      .withDistinguishedName(username)
      .produce()
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns ApplicationEntityFactory()
      .withId(applicationId)
      .withYieldedCreatedByProbationOfficer { ProbationOfficerEntityFactory().produce() }
      .produce()

    assertThat(applicationService.updateApplication(applicationId, "{}", null, username) is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `updateApplication returns GeneralValidationError when application has already been submitted`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val probationOfficer = ProbationOfficerEntityFactory()
      .withDistinguishedName(username)
      .produce()

    every { mockProbationOfficerService.getProbationOfficerForRequestUser() } returns probationOfficer
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns ApplicationEntityFactory()
      .withId(applicationId)
      .withCreatedByProbationOfficer(probationOfficer)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    val result = applicationService.updateApplication(applicationId, "{}", null, username)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
    val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

    assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")
  }

  @Test
  fun `updateApplication returns FieldValidationError when data does not conform to newest schema, submitted at is in future`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val probationOfficer = ProbationOfficerEntityFactory()
      .withDistinguishedName(username)
      .produce()

    val newestSchema = ApplicationSchemaEntityFactory().produce()

    every { mockProbationOfficerService.getProbationOfficerForRequestUser() } returns probationOfficer
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns ApplicationEntityFactory()
      .withId(applicationId)
      .withCreatedByProbationOfficer(probationOfficer)
      .produce()
    every { mockJsonSchemaService.getNewestSchema() } returns newestSchema
    every { mockJsonSchemaService.validate(newestSchema, "{}") } returns false

    val result = applicationService.updateApplication(applicationId, "{}", OffsetDateTime.now().plusMinutes(1), username)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity is ValidatableActionResult.FieldValidationError).isTrue
    val validatableActionResult = result.entity as ValidatableActionResult.FieldValidationError

    assertThat(validatableActionResult.validationMessages).containsEntry("$.data", "This data does not conform to the newest application schema")
    assertThat(validatableActionResult.validationMessages).containsEntry("$.submittedAt", "Submitted at must be in the past")
  }

  @Test
  fun `updateApplication returns Success with updated Application`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val probationOfficer = ProbationOfficerEntityFactory()
      .withDistinguishedName(username)
      .produce()

    val newestSchema = ApplicationSchemaEntityFactory().produce()
    val submittedAt = OffsetDateTime.now().minusMinutes(1)
    val updatedData = """
      {
        "aProperty": "value"
      }
    """

    every { mockProbationOfficerService.getProbationOfficerForRequestUser() } returns probationOfficer
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns ApplicationEntityFactory()
      .withId(applicationId)
      .withCreatedByProbationOfficer(probationOfficer)
      .produce()
    every { mockJsonSchemaService.getNewestSchema() } returns newestSchema
    every { mockJsonSchemaService.validate(newestSchema, updatedData) } returns true
    every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    val result = applicationService.updateApplication(applicationId, updatedData, submittedAt, username)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validatableActionResult = result.entity as ValidatableActionResult.Success

    assertThat(validatableActionResult.entity.submittedAt).isEqualTo(submittedAt)
    assertThat(validatableActionResult.entity.data).isEqualTo(updatedData)
  }
}
