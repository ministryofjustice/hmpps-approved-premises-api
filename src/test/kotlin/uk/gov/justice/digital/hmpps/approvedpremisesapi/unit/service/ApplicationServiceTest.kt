package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationOfficerEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationOfficerRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import java.util.UUID

class ApplicationServiceTest {
  private val mockProbationOfficerRepository = mockk<ProbationOfficerRepository>()
  private val mockApplicationRepository = mockk<ApplicationRepository>()

  private val applicationService = ApplicationService(mockProbationOfficerRepository, mockApplicationRepository)

  @Test
  fun `Get all applications where Probation Officer with provided distinguished name does not exist returns empty list`() {
    val distinguishedName = "SOMEPERSON"

    every { mockProbationOfficerRepository.findByDistinguishedName(distinguishedName) } returns null

    assertThat(applicationService.getAllApplicationsForUsername(distinguishedName)).isEmpty()
  }

  @Test
  fun `Get all applications where Probation Officer exists returns applications returned from repository`() {
    val probationOfficerId = UUID.fromString("8a0624b8-8e92-47ce-b645-b65ea5a197d0")
    val distinguishedName = "SOMEPERSON"
    val probationOfficerEntity = ProbationOfficerEntityFactory()
      .withId(probationOfficerId)
      .withDistinguishedName(distinguishedName)
      .produce()
    val applicationEntities = listOf(
      ApplicationEntityFactory()
        .withCreatedByProbationOfficer(probationOfficerEntity)
        .produce(),
      ApplicationEntityFactory()
        .withCreatedByProbationOfficer(probationOfficerEntity)
        .produce(),
      ApplicationEntityFactory()
        .withCreatedByProbationOfficer(probationOfficerEntity)
        .produce()
    )

    every { mockProbationOfficerRepository.findByDistinguishedName(distinguishedName) } returns probationOfficerEntity

    every { mockApplicationRepository.findAllByCreatedByProbationOfficer_Id(probationOfficerId) } returns applicationEntities

    assertThat(applicationService.getAllApplicationsForUsername(distinguishedName)).containsAll(applicationEntities)
  }
}
