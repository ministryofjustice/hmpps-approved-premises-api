package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CaseService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ManagerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class CaseServiceTest {
  private val caseRepository = mockk<CaseRepository>()

  private val service = CaseService(caseRepository)

  @Test
  fun `should return existing offender when exist`() {
    val caseEntity = CaseEntityFactory()
      .withCrn("CRN123")
      .withName("NAME")
      .withNomsNumber("NOMS123")
      .withTier("tier")
      .produce()

    val caseSummary = CaseSummary(
      crn = "CRN123",
      nomsId = "NOMS123",
      name = Name(
        forename = "John",
        surname = "Smith",
        middleNames = emptyList(),
      ),
      pnc = "PNC123",
      dateOfBirth = LocalDate.parse("2023-06-26"),
      gender = "male",
      profile = null,
      manager = ManagerFactory().produce(),
      currentExclusion = false,
      currentRestriction = false,
    )

    val risk = PersonRisksFactory()
      .withTier(
        RiskWithStatus(
          RiskTier(
            level = "M1",
            lastUpdated = LocalDate.parse("2023-06-26"),
          ),
        ),
      )
      .produce()

    every { caseRepository.findByCrn("CRN123") } returns caseEntity
    every { caseRepository.saveAndFlush(any()) } returns caseEntity

    val result = service.ensureCaseExists(caseSummary, risk)

    assertThat(result).isEqualTo(caseEntity)
  }

  @Test
  fun `should create new offender when not exist`() {
    val caseEntity = CaseEntityFactory()
      .withCrn("CRN123")
      .withName("NAME")
      .withNomsNumber("NOMS123")
      .withTier("tier")
      .produce()

    val caseSummary = CaseSummary(
      crn = "CRN123",
      nomsId = "NOMS123",
      name = Name(
        forename = "John",
        surname = "Smith",
        middleNames = emptyList(),
      ),
      pnc = "PNC123",
      dateOfBirth = LocalDate.parse("2023-06-26"),
      gender = "male",
      profile = null,
      manager = ManagerFactory().produce(),
      currentExclusion = false,
      currentRestriction = false,
    )

    val risk = PersonRisksFactory()
      .withTier(
        RiskWithStatus(
          RiskTier(
            level = "M1",
            lastUpdated = LocalDate.parse("2023-06-26"),
          ),
        ),
      )
      .produce()

    every { caseRepository.findByCrn("CRN123") } returns null
    every { caseRepository.saveAndFlush(any()) } returns caseEntity

    val result = service.ensureCaseExists(caseSummary, risk)

    assertThat(result).isEqualTo(caseEntity)
  }
}
