package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationOfficerEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationOfficerEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationOfficerRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ProbationOfficerService

class ProbationOfficerServiceTest {
  private val mockHttpAuthService = mockk<HttpAuthService>()
  private val mockProbationOfficerRepository = mockk<ProbationOfficerRepository>()

  private val probationOfficerService = ProbationOfficerService(mockHttpAuthService, mockProbationOfficerRepository)

  @Test
  fun `getProbationOfficerForRequestUser returns existing Probation Officer when exists`() {
    val username = "SOMEPERSON"
    val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

    every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
    every { mockPrincipal.name } returns username

    val probationOfficer = ProbationOfficerEntityFactory().produce()

    every { mockProbationOfficerRepository.findByDistinguishedName(username) } returns probationOfficer

    assertThat(probationOfficerService.getProbationOfficerForRequestUser()).isEqualTo(probationOfficer)
  }

  @Test
  fun `getProbationOfficerForRequestUser returns new Probation Officer when one does not already exist`() {
    val username = "SOMEPERSON"
    val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

    every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
    every { mockPrincipal.name } returns username

    every { mockProbationOfficerRepository.findByDistinguishedName(username) } returns null
    every { mockProbationOfficerRepository.save(any()) } answers { it.invocation.args[0] as ProbationOfficerEntity }

    assertThat(probationOfficerService.getProbationOfficerForRequestUser()).matches {
      it.name == username
    }
  }
}
