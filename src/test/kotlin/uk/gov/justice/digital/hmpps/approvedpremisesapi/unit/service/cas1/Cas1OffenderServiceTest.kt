package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ManagerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1OffenderEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1OffenderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OffenderService
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class Cas1OffenderServiceTest {
  private val cas1OffenderRepository = mockk<Cas1OffenderRepository>()

  private val service = Cas1OffenderService(cas1OffenderRepository)

  @Test
  fun `should return existing offender when exist`() {
    val cas1OffenderEntity = Cas1OffenderEntityFactory()
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

    every { cas1OffenderRepository.findByCrn("CRN123") } returns cas1OffenderEntity
    every { cas1OffenderRepository.saveAndFlush(any()) } returns cas1OffenderEntity

    val result = service.getOrCreateOffender(caseSummary, risk)

    assertThat(result).isEqualTo(cas1OffenderEntity)
  }

  @Test
  fun `should create new offender when not exist`() {
    val cas1OffenderEntity = Cas1OffenderEntityFactory()
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

    every { cas1OffenderRepository.findByCrn("CRN123") } returns null
    every { cas1OffenderRepository.saveAndFlush(any()) } returns cas1OffenderEntity

    val result = service.getOrCreateOffender(caseSummary, risk)

    assertThat(result).isEqualTo(cas1OffenderEntity)
  }
}
