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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1PremiseServiceTest {

  @MockK
  lateinit var premisesRepository: PremisesRepository

  @MockK
  lateinit var premisesService: PremisesService

  @InjectMockKs
  lateinit var service: Cas1PremisesService

  companion object CONSTANTS {
    val PREMISES_ID = UUID.randomUUID()
  }

  @Nested
  inner class GetPremisesSummary {

    @Test
    fun `premises not found return error`() {
      every { premisesRepository.findByIdOrNull(PREMISES_ID) } returns null

      val result = service.getPremisesSummary(PREMISES_ID)

      assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
    }

    @Test
    fun `premises not cas1 return error`() {
      every { premisesRepository.findByIdOrNull(PREMISES_ID) } returns
        TemporaryAccommodationPremisesEntityFactory()
          .withDefaults()
          .produce()

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

      every { premisesRepository.findByIdOrNull(PREMISES_ID) } returns premises

      every { premisesService.getBedCount(premises) } returns 56

      val result = service.getPremisesSummary(PREMISES_ID)

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      val premisesSummary = result.value
      assertThat(premisesSummary.id).isEqualTo(premises.id)
      assertThat(premisesSummary.name).isEqualTo("the name")
      assertThat(premisesSummary.apCode).isEqualTo("the ap code")
      assertThat(premisesSummary.postcode).isEqualTo("LE11 1PO")
      assertThat(premisesSummary.bedCount).isEqualTo(56)
    }
  }
}
