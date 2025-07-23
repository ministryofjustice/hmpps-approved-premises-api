package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.datetime.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1PremisesLocalRestrictionSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesBasicSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremisesTransformer
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1PremisesTransformerTest {

  @MockK
  lateinit var apAreaTransformer: ApAreaTransformer

  @InjectMockKs
  lateinit var transformer: Cas1PremisesTransformer

  companion object CONSTANTS {
    val AP_AREA_ID: UUID = UUID.randomUUID()
    val PREMISES_ID: UUID = UUID.randomUUID()
  }

  @Nested
  inner class ToPremisesSummary {

    @Test
    fun success() {
      val apArea = ApAreaEntityFactory().produce()

      val probationRegion = ProbationRegionEntityFactory()
        .withDefaults()
        .withApArea(apArea)
        .produce()

      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withId(PREMISES_ID)
        .withName("the name")
        .withApCode("the ap code")
        .withFullAddress("the full address")
        .withPostcode("LE11 1PO")
        .withProbationRegion(probationRegion)
        .withSupportsSpaceBookings(true)
        .withManagerDetails("manager details")
        .produce()

      val restriction1 = Cas1PremisesLocalRestrictionSummary(UUID.randomUUID(), "restrictions 1", java.time.LocalDate.now())
      val restriction2 = Cas1PremisesLocalRestrictionSummary(UUID.randomUUID(), "restrictions 2", java.time.LocalDate.now().minusDays(1))

      val expectedApArea = ApArea(UUID.randomUUID(), "id", "name")
      every { apAreaTransformer.transformJpaToApi(apArea) } returns expectedApArea

      val result = transformer.toPremises(
        Cas1PremisesService.Cas1PremisesInfo(
          entity = premises,
          bedCount = 10,
          outOfServiceBeds = 2,
          availableBeds = 8,
          overbookingSummary = emptyList(),
          localRestrictions = listOf(
            restriction2,
            restriction1,
          ),
        ),
      )

      assertThat(result.id).isEqualTo(premises.id)
      assertThat(result.name).isEqualTo("the name")
      assertThat(result.apCode).isEqualTo("the ap code")
      assertThat(result.fullAddress).isEqualTo("the full address")
      assertThat(result.postcode).isEqualTo("LE11 1PO")
      assertThat(result.bedCount).isEqualTo(10)
      assertThat(result.availableBeds).isEqualTo(8)
      assertThat(result.outOfServiceBeds).isEqualTo(2)
      assertThat(result.apArea).isEqualTo(expectedApArea)
      assertThat(result.supportsSpaceBookings).isTrue()
      assertThat(result.managerDetails).isEqualTo("manager details")
      assertThat(result.overbookingSummary).isEmpty()
      assertThat(result.localRestrictions).isEqualTo(listOf(restriction2, restriction1))
    }
  }

  @Nested
  inner class ToPremiseBasicSummary {

    @Test
    fun success() {
      val premisesSummary = ApprovedPremisesBasicSummary(
        PREMISES_ID,
        "the name",
        "AP Area Code",
        AP_AREA_ID,
        "the ap area name",
        12,
        supportsSpaceBookings = false,
        fullAddress = "the full address",
        addressLine1 = "line 1",
        addressLine2 = null,
        town = null,
        postcode = "the postcode",
      )

      val result = transformer.toPremiseBasicSummary(premisesSummary)

      assertThat(result.id).isEqualTo(PREMISES_ID)
      assertThat(result.name).isEqualTo("the name")
      assertThat(result.apCode).isEqualTo("AP Area Code")
      assertThat(result.apArea.id).isEqualTo(AP_AREA_ID)
      assertThat(result.apArea.name).isEqualTo("the ap area name")
      assertThat(result.bedCount).isEqualTo(12)
      assertThat(result.supportsSpaceBookings).isEqualTo(false)
      assertThat(result.fullAddress).isEqualTo("the full address")
      assertThat(result.postcode).isEqualTo("the postcode")
    }
  }
}
