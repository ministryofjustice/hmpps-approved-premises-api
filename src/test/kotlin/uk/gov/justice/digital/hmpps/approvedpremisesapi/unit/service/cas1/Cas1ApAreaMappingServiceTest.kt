package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TeamFactoryDeliusContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApAreaMappingService

class Cas1ApAreaMappingServiceTest {

  private val apAreaRepository = mockk<ApAreaRepository>()

  private val service = Cas1ApAreaMappingService(apAreaRepository)

  @Nested
  inner class DetermineApArea {

    @Test
    fun `determineApArea when region has ap area defined, return region's associated ap area`() {
      val usersProbationRegionApArea = ApAreaEntityFactory().produce()

      val usersProbationRegion = ProbationRegionEntityFactory()
        .withDefaults()
        .withApArea(usersProbationRegionApArea)
        .produce()

      val staffUserDetails = StaffDetailFactory.staffDetail()

      val result = service.determineApArea(
        usersProbationRegion,
        staffUserDetails,
        staffUserDetails.username!!,
      )

      assertThat(result).isEqualTo(usersProbationRegionApArea)
    }

    @ParameterizedTest
    @CsvSource(
      "N07CEU, LON",
      "N43CP1, NE",
      "N43CP1, NE",
      "N43CP2, NE",
      "N43CPP, NE",
      "N43LKS, LON",
      "N43MID, Mids",
      "N43NTH, NW",
      "N43SCE, SEE",
      "N43WSW, SWSC",
      "N43OMD, Mids",
      "N43OLK, LON",
      "N43ONM, NW",
      "N43OSE, SEE",
      "N43OWS, SWSC",
      "N41CRU, NE",
      "N41EFT, NE",
      "N41EP2, NE",
      "N41EXM, NE",
      "N41EP4, NE",
      "XXXNAT, NE",
      "XXXNAT, NE",
    )
    fun `determineApArea when region has no ap area defined using team code`(
      deliusTeamCode: String,
      expectedApArea: String,
    ) {
      val usersProbationRegion = ProbationRegionEntityFactory()
        .withDefaults()
        .withApArea(null)
        .produce()

      val staffUserDetails = StaffDetailFactory.staffDetail(
        probationArea = ProbationArea(code = "XYA", description = "description"),
        teams =
        listOf(
          TeamFactoryDeliusContext.team(code = "CODE_NOT_IN_MAPPING"),
          TeamFactoryDeliusContext.team(code = deliusTeamCode),
          TeamFactoryDeliusContext.team(code = "OTHER_CODE_NOT_IN_MAPPING"),
        ),
      )

      val retrievedApArea = ApAreaEntityFactory().produce()
      every { apAreaRepository.findByIdentifier(expectedApArea) } returns retrievedApArea

      val result = service.determineApArea(
        usersProbationRegion,
        staffUserDetails,
        staffUserDetails.username!!,
      )

      assertThat(result).isEqualTo(retrievedApArea)
    }

    @Test
    fun `determineApArea when region has no ap area defined and user has no team codes, use NE`() {
      val usersProbationRegion = ProbationRegionEntityFactory()
        .withDefaults()
        .withApArea(null)
        .produce()

      val username = "J_ALUCARD"
      val staffUserDetails =
        StaffDetailFactory.staffDetail(
          deliusUsername = username,
          probationArea = ProbationArea(code = "N43", description = "description"),
          teams = emptyList(),
        )

      val retrievedApArea = ApAreaEntityFactory().produce()
      every { apAreaRepository.findByIdentifier("NE") } returns retrievedApArea

      val result = service.determineApArea(
        usersProbationRegion,
        staffUserDetails,
        username,
      )

      assertThat(result).isEqualTo(retrievedApArea)
    }

    @Test
    fun `throws error with details when region has no ap area defined and mapping is not available for any of their teams`() {
      val usersProbationRegion = ProbationRegionEntityFactory()
        .withDefaults()
        .withDeliusCode("PR1")
        .withApArea(null)
        .produce()

      val username = "J_ALUCARD"
      val staffUserDetails = StaffDetailFactory.staffDetail(
        deliusUsername = username,
        probationArea = ProbationArea(code = "XYZ", description = "description"),
        teams =
        listOf(
          TeamFactoryDeliusContext.team(code = "CODE_NOT_IN_MAPPING"),
          TeamFactoryDeliusContext.team(code = "OTHER_CODE_NOT_IN_MAPPING"),
        ),
      )

      assertThatThrownBy {
        service.determineApArea(
          usersProbationRegion,
          staffUserDetails,
          username,
        )
      }.hasMessage(
        "Internal Server Error: Could not find a delius team mapping for delius user J_ALUCARD " +
          "with probation region PR1 and teams " +
          "[CODE_NOT_IN_MAPPING, OTHER_CODE_NOT_IN_MAPPING]",
      )
    }

    @Test
    fun `throws error with details when resolved area id doesnt exist in ap area table`() {
      val usersProbationRegion = ProbationRegionEntityFactory()
        .withDefaults()
        .withApArea(null)
        .produce()

      val staffUserDetails = StaffDetailFactory.staffDetail(
        probationArea = ProbationArea(code = "XYZ", description = "description"),
        teams =
        listOf(
          TeamFactoryDeliusContext.team(code = "N43MID"),
        ),
      )
      every { apAreaRepository.findByIdentifier("Mids") } returns null

      assertThatThrownBy {
        service.determineApArea(
          usersProbationRegion,
          staffUserDetails,
          staffUserDetails.username!!,
        )
      }.hasMessage(
        "Internal Server Error: Could not find AP Area for code Mids",
      )
    }
  }
}
