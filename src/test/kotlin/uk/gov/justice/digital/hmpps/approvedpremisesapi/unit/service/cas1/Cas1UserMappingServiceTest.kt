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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserMappingService

class Cas1UserMappingServiceTest {

  private val apAreaRepository = mockk<ApAreaRepository>()

  private val service = Cas1UserMappingService(apAreaRepository)

  private val usersProbationRegionApArea = ApAreaEntityFactory().produce()

  private val usersProbationRegion = ProbationRegionEntityFactory()
    .withDefaults()
    .withApArea(usersProbationRegionApArea)
    .produce()

  @Nested
  inner class DetermineApArea {

    @Test
    fun `determineApArea when user not in national probation area, return user's probation region's associated ap area`() {
      val staffUserDetails = StaffUserDetailsFactory()
        .withProbationAreaCode("N42")
        .produce()

      val result = service.determineApArea(
        usersProbationRegion,
        staffUserDetails,
      )

      assertThat(result).isEqualTo(usersProbationRegionApArea)
    }

    @ParameterizedTest
    @CsvSource(
      "N43, N07CEU, LON",
      "N43, N43CP1, NE",
      "N43, N43CP1, NE",
      "N43, N43CP2, NE",
      "N43, N43CPP, NE",
      "N43, N43LKS, LON",
      "N43, N43MID, Mids",
      "N43, N43NTH, NW",
      "N43, N43SCE, SEE",
      "N43, N43WSW, SWSC",
      "N43, N43OMD, Mids",
      "N43, N43OLK, LON",
      "N43, N43ONM, NW",
      "N43, N43OSE, SEE",
      "N43, N43OWS, SWSC",
      "N43, N41CRU, NE",
      "N43, N41EFT, NE",
      "N43, N41EP2, NE",
      "N43, N41EXM, NE",
      "N43, N41EP4, NE",
      "N43, XXXNAT, NE",
      "N43, XXXNAT, NE",
      "N41, N07CEU, LON",
      "N41, N43CP1, NE",
      "N41, N43CP1, NE",
      "N41, N43CP2, NE",
      "N41, N43CPP, NE",
      "N41, N43LKS, LON",
      "N41, N43MID, Mids",
      "N41, N43NTH, NW",
      "N41, N43SCE, SEE",
      "N41, N43WSW, SWSC",
      "N41, N43OMD, Mids",
      "N41, N43OLK, LON",
      "N41, N43ONM, NW",
      "N41, N43OSE, SEE",
      "N41, N43OWS, SWSC",
      "N41, N41CRU, NE",
      "N41, N41EFT, NE",
      "N41, N41EP2, NE",
      "N41, N41EXM, NE",
      "N41, N41EP4, NE",
      "N41, XXXNAT, NE",
      "N41, XXXNAT, NE",
      "XXX, N07CEU, LON",
      "XXX, N43CP1, NE",
      "XXX, N43CP1, NE",
      "XXX, N43CP2, NE",
      "XXX, N43CPP, NE",
      "XXX, N43LKS, LON",
      "XXX, N43MID, Mids",
      "XXX, N43NTH, NW",
      "XXX, N43SCE, SEE",
      "XXX, N43WSW, SWSC",
      "XXX, N43OMD, Mids",
      "XXX, N43OLK, LON",
      "XXX, N43ONM, NW",
      "XXX, N43OSE, SEE",
      "XXX, N43OWS, SWSC",
      "XXX, N41CRU, NE",
      "XXX, N41EFT, NE",
      "XXX, N41EP2, NE",
      "XXX, N41EXM, NE",
      "XXX, N41EP4, NE",
      "XXX, XXXNAT, NE",
      "XXX, XXXNAT, NE",
    )
    fun `determine ApArea from team code when user in national probation area and mapping exists for team`(
      deliusProbationAreaCode: String,
      deliusTeamCode: String,
      expectedApArea: String,
    ) {
      val staffUserDetails = StaffUserDetailsFactory()
        .withProbationAreaCode(deliusProbationAreaCode)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory().withCode("CODE_NOT_IN_MAPPING").produce(),
            StaffUserTeamMembershipFactory().withCode(deliusTeamCode).produce(),
            StaffUserTeamMembershipFactory().withCode("OTHER_CODE_NOT_IN_MAPPING").produce(),
          ),
        )
        .produce()

      val retrievedApArea = ApAreaEntityFactory().produce()
      every { apAreaRepository.findByIdentifier(expectedApArea) } returns retrievedApArea

      val result = service.determineApArea(
        usersProbationRegion,
        staffUserDetails,
      )

      assertThat(result).isEqualTo(retrievedApArea)
    }

    @Test
    fun `determineApArea when user in national probation area but they are not in any teams use NE`() {
      val staffUserDetails = StaffUserDetailsFactory()
        .withUsername("J_ALUCARD")
        .withProbationAreaCode("N43")
        .withTeams(emptyList())
        .produce()

      val retrievedApArea = ApAreaEntityFactory().produce()
      every { apAreaRepository.findByIdentifier("NE") } returns retrievedApArea

      val result = service.determineApArea(
        usersProbationRegion,
        staffUserDetails,
      )

      assertThat(result).isEqualTo(retrievedApArea)
    }

    @Test
    fun `throws error with details when user in national probation area but mapping is not available for any of their teams`() {
      val staffUserDetails = StaffUserDetailsFactory()
        .withUsername("J_ALUCARD")
        .withProbationAreaCode("N43")
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory().withCode("CODE_NOT_IN_MAPPING").produce(),
            StaffUserTeamMembershipFactory().withCode("OTHER_CODE_NOT_IN_MAPPING").produce(),
          ),
        )
        .produce()

      assertThatThrownBy {
        service.determineApArea(
          usersProbationRegion,
          staffUserDetails,
        )
      }.hasMessage(
        "Internal Server Error: Could not find a delius team mapping for delius user J_ALUCARD " +
          "with delius probation area code N43 and teams " +
          "[CODE_NOT_IN_MAPPING, OTHER_CODE_NOT_IN_MAPPING]",
      )
    }

    @Test
    fun `throws error with details when resolved area id doesnt exist in ap area table`() {
      val staffUserDetails = StaffUserDetailsFactory()
        .withProbationAreaCode("XXX")
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory().withCode("N43MID").produce(),
          ),
        )
        .produce()

      every { apAreaRepository.findByIdentifier("Mids") } returns null

      assertThatThrownBy {
        service.determineApArea(
          usersProbationRegion,
          staffUserDetails,
        )
      }.hasMessage(
        "Internal Server Error: Could not find AP Area for code Mids",
      )
    }
  }
}
