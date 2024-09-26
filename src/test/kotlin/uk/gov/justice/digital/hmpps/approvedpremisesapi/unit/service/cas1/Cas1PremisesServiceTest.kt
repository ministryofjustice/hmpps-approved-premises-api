package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1PremisesServiceTest {

  @MockK
  lateinit var approvedPremisesRepository: ApprovedPremisesRepository

  @MockK
  lateinit var premisesService: PremisesService

  @MockK
  lateinit var cas1OutOfServiceBedService: Cas1OutOfServiceBedService

  @InjectMockKs
  lateinit var service: Cas1PremisesService

  companion object CONSTANTS {
    val PREMISES_ID: UUID = UUID.randomUUID()
  }

  @Nested
  inner class GetPremisesSummary {

    @Test
    fun `premises not found return error`() {
      every { approvedPremisesRepository.findByIdOrNull(PREMISES_ID) } returns null

      val result = service.getPremisesSummary(PREMISES_ID)

      assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
    }

    @Test
    fun `success`() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withId(PREMISES_ID)
        .withName("the name")
        .withApCode("the ap code")
        .withPostcode("LE11 1PO")
        .produce()

      every { approvedPremisesRepository.findByIdOrNull(PREMISES_ID) } returns premises

      every { premisesService.getBedCount(premises) } returns 56
      every { cas1OutOfServiceBedService.getActiveOutOfServiceBedsCountForPremisesId(PREMISES_ID) } returns 4

      val result = service.getPremisesSummary(PREMISES_ID)

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      val premisesSummaryInfo = result.value
      assertThat(premisesSummaryInfo.entity).isEqualTo(premises)
      assertThat(premisesSummaryInfo.bedCount).isEqualTo(56)
      assertThat(premisesSummaryInfo.outOfServiceBeds).isEqualTo(4)
      assertThat(premisesSummaryInfo.availableBeds).isEqualTo(52)
    }
  }
}
