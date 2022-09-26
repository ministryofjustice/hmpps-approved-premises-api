package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationOfficerEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationOfficerRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import java.util.UUID

class ApplicationServiceTest {
  private val mockProbationOfficerRepository = mockk<ProbationOfficerRepository>()
  private val mockApplicationRepository = mockk<ApplicationRepository>()
  private val mockJsonSchemaService = mockk<JsonSchemaService>()

  private val applicationService = ApplicationService(
    mockProbationOfficerRepository,
    mockApplicationRepository,
    mockJsonSchemaService
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
}
