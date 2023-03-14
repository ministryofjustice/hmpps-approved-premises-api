package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BedSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import java.time.LocalDate
import java.util.UUID

class BedSearchServiceTest {
  private val mockBedSearchRepository = mockk<BedSearchRepository>()
  private val mockCharacteristicService = mockk<CharacteristicService>()

  private val bedSearchService = BedSearchService(
    mockBedSearchRepository,
    mockCharacteristicService
  )

  @Test
  fun `findBeds returns Unauthorised for users without MATCHER role`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val result = bedSearchService.findBeds(
      user = user,
      postcodeDistrictOutcode = "AA11",
      maxDistanceMiles = 50,
      startDate = LocalDate.parse("2023-03-14"),
      durationInDays = 10,
      requiredPremisesCharacteristics = listOf("premisesCharacteristic1", "premisesCharacteristic2"),
      requiredRoomCharacteristics = listOf("roomCharacteristic1", "roomCharacteristic2"),
      service = "approved-premises"
    )

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `findBeds returns FieldValidationError for invalid characteristics`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.MATCHER)
          .produce()
      }

    every { mockCharacteristicService.getCharacteristic("premisesCharacteristic1") } returns null
    every { mockCharacteristicService.getCharacteristic("premisesCharacteristic2") } returns CharacteristicEntityFactory()
      .withModelScope("room")
      .withServiceScope("temporary-accommodation")
      .produce()

    every { mockCharacteristicService.getCharacteristic("roomCharacteristic1") } returns null
    every { mockCharacteristicService.getCharacteristic("roomCharacteristic2") } returns CharacteristicEntityFactory()
      .withModelScope("premises")
      .withServiceScope("temporary-accommodation")
      .produce()

    val result = bedSearchService.findBeds(
      user = user,
      postcodeDistrictOutcode = "AA11",
      maxDistanceMiles = 50,
      startDate = LocalDate.parse("2023-03-14"),
      durationInDays = 10,
      requiredPremisesCharacteristics = listOf("premisesCharacteristic1", "premisesCharacteristic2"),
      requiredRoomCharacteristics = listOf("roomCharacteristic1", "roomCharacteristic2"),
      service = "approved-premises"
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    val validationResult = result.entity
    assertThat(validationResult is ValidatableActionResult.FieldValidationError).isTrue
    validationResult as ValidatableActionResult.FieldValidationError

    assertThat(validationResult.validationMessages["$.requiredPremisesCharacteristics"]).isEqualTo(
      "premisesCharacteristic1 doesNotExist, premisesCharacteristic2 serviceScopeInvalid, premisesCharacteristic2 modelScopeInvalid"
    )

    assertThat(validationResult.validationMessages["$.requiredRoomCharacteristics"]).isEqualTo(
      "roomCharacteristic1 doesNotExist, roomCharacteristic2 serviceScopeInvalid, roomCharacteristic2 modelScopeInvalid"
    )
  }

  @Test
  fun `findBeds returns Success with search results`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.MATCHER)
          .produce()
      }

    every { mockCharacteristicService.getCharacteristic("premisesCharacteristic1") } returns CharacteristicEntityFactory()
      .withId(UUID.fromString("082b2ee5-4d5b-4724-9964-488f4be7cd40"))
      .withModelScope("premises")
      .withServiceScope("approved-premises")
      .produce()
    every { mockCharacteristicService.getCharacteristic("premisesCharacteristic2") } returns CharacteristicEntityFactory()
      .withId(UUID.fromString("e1e789bf-945e-4f15-9587-13a66e2e32bc"))
      .withModelScope("premises")
      .withServiceScope("approved-premises")
      .produce()

    every { mockCharacteristicService.getCharacteristic("roomCharacteristic1") } returns CharacteristicEntityFactory()
      .withId(UUID.fromString("455212fa-3d2e-45cd-9e44-da97676606c3"))
      .withModelScope("room")
      .withServiceScope("approved-premises")
      .produce()
    every { mockCharacteristicService.getCharacteristic("roomCharacteristic2") } returns CharacteristicEntityFactory()
      .withId(UUID.fromString("4f2f8e4d-85b5-4903-8f8e-9cfea7061467"))
      .withModelScope("room")
      .withServiceScope("approved-premises")
      .produce()

    val searchResults = mutableListOf<BedSearchResult>(
      BedSearchResult(
        premisesId = UUID.fromString("323706f4-5054-452a-aa4a-ecc075ebc9ac"),
        premisesName = "Premises Name",
        premisesCharacteristicPropertyNames = mutableListOf("propertyCharacteristic1", "propertyCharacteristic2"),
        roomId = UUID.fromString("59a2ce43-eda8-4564-807a-72d6c043c0f7"),
        roomName = "Room Name",
        bedId = UUID.fromString("dd8c3e05-c372-4c06-af20-77c6347a403f"),
        bedName = "Bed Name",
        roomCharacteristicPropertyNames = mutableListOf("roomCharacteristic1", "roomCharacteristic2"),
        distance = 32.12
      )
    )

    every {
      mockBedSearchRepository.findBeds(
        postcodeDistrictOutcode = "AA11",
        maxDistanceMiles = 50,
        startDate = LocalDate.parse("2023-03-14"),
        durationInDays = 10,
        requiredPremisesCharacteristics = listOf(UUID.fromString("082b2ee5-4d5b-4724-9964-488f4be7cd40"), UUID.fromString("e1e789bf-945e-4f15-9587-13a66e2e32bc")),
        requiredRoomCharacteristics = listOf(UUID.fromString("455212fa-3d2e-45cd-9e44-da97676606c3"), UUID.fromString("4f2f8e4d-85b5-4903-8f8e-9cfea7061467")),
        service = "approved-premises"
      )
    } returns searchResults

    val result = bedSearchService.findBeds(
      user = user,
      postcodeDistrictOutcode = "AA11",
      maxDistanceMiles = 50,
      startDate = LocalDate.parse("2023-03-14"),
      durationInDays = 10,
      requiredPremisesCharacteristics = listOf("premisesCharacteristic1", "premisesCharacteristic2"),
      requiredRoomCharacteristics = listOf("roomCharacteristic1", "roomCharacteristic2"),
      service = "approved-premises"
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    val validationResult = result.entity
    assertThat(validationResult is ValidatableActionResult.Success).isTrue
    validationResult as ValidatableActionResult.Success

    assertThat(validationResult.entity).isEqualTo(searchResults)
  }
}
