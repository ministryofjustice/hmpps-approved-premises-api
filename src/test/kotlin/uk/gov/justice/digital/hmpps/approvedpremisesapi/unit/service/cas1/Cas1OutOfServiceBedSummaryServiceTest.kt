package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedSummaryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1OutOfServiceBedSummaryServiceTest {

  private val cas1PremisesService = mockk<Cas1PremisesService>()
  private val cas1OutOfServiceBedService = mockk<Cas1OutOfServiceBedService>()

  private val service = Cas1OutOfServiceBedSummaryService(
    cas1PremisesService,
    cas1OutOfServiceBedService,
  )

  @Nested
  inner class GetOutOfServiceBedSummaries {

    @Test
    fun `returns not found error if premises with the given Id does not exist`() {
      every { cas1PremisesService.findPremiseById(any()) } returns null

      val result = service.getOutOfServiceBedSummaries(
        UUID.randomUUID(),
        UUID.randomUUID(),
        LocalDate.now(),
      )

      assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
      assertThat((result as CasResult.NotFound).entityType).isEqualTo("premises")
    }
  }

  @Test
  fun `returns out of service bed summaries when out of service beds exist`() {
    val apArea = ApAreaEntityFactory()
      .produce()
    val probationRegion = ProbationRegionEntityFactory()
      .withApArea(apArea)
      .produce()
    val premises = ApprovedPremisesEntityFactory()
      .withLocalAuthorityArea(LocalAuthorityAreaEntityFactory().produce())
      .withProbationRegion(probationRegion)
      .produce()
    val date = LocalDate.now()

    val outOfServiceBed1 = Cas1OutOfServiceBedEntityFactory()
      .withBed {
        withRoom {
          withPremises(
            ApprovedPremisesEntityFactory()
              .withDefaults()
              .produce(),
          )
        }
      }
      .produce()

    every { cas1PremisesService.findPremiseById(premises.id) } returns premises
    every { cas1OutOfServiceBedService.getOutOfServiceBedsForDate(any(), premises.id, apArea.id, date, any()) } returns
      Pair(listOf(outOfServiceBed1), PaginationMetadata(0, 0, 0, 0))

    val result = service.getOutOfServiceBedSummaries(
      premisesId = premises.id,
      apAreaId = apArea.id,
      date = LocalDate.now(),
    )

    assertThat(result).isInstanceOf(CasResult.Success::class.java)
    result as CasResult.Success
    val outOfServiceBedSummaries = result.value
    assertThat(outOfServiceBedSummaries.size).isEqualTo(1)
    assertThat(result.value[0]).isInstanceOf(Cas1OutOfServiceBedEntity::class.java)
  }
}
