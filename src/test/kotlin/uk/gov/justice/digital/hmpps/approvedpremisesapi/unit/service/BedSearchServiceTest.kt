package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PostCodeDistrictEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostcodeDistrictRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApprovedPremisesBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CharacteristicNames
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BedSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThat
import java.time.LocalDate
import java.util.UUID

class BedSearchServiceTest {
  private val mockBedSearchRepository = mockk<BedSearchRepository>()
  private val mockPostcodeDistrictRepository = mockk<PostcodeDistrictRepository>()
  private val mockCharacteristicService = mockk<CharacteristicService>()

  private val bedSearchService = BedSearchService(
    mockBedSearchRepository,
    mockPostcodeDistrictRepository,
    mockCharacteristicService,
  )

  @Test
  fun `findApprovedPremisesBeds returns Unauthorised when user does not have the MATCHER role`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val result = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = "AA11",
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      requiredCharacteristics = listOf(),
    )

    assertThat(result).isUnauthorised()
  }

  @Test
  fun `findApprovedPremisesBeds returns FieldValidationError when premises characteristic does not exist`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_MATCHER)
          .produce()
      }

    val postcodeDistrict = PostCodeDistrictEntityFactory()
      .produce()

    every { mockPostcodeDistrictRepository.findByOutcode(postcodeDistrict.outcode) } returns postcodeDistrict

    val premisesCharacteristicPropertyName = "isRecoveryFocussed"

    val roomCharacteristic = CharacteristicEntityFactory()
      .withPropertyName("isESAP")
      .withModelScope("room")
      .withServiceScope("approved-premises")
      .produce()

    every {
      mockCharacteristicService.getCharacteristicsByPropertyNames(
        listOf("isESAP", premisesCharacteristicPropertyName),
        ServiceName.approvedPremises,
      )
    } returns listOf(roomCharacteristic)

    val result = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      requiredCharacteristics = listOf(PlacementCriteria.isESAP, PlacementCriteria.isRecoveryFocussed),
    )

    assertThat(result).isFieldValidationError(
      "$.requiredCharacteristics",
      "$premisesCharacteristicPropertyName doesNotExist",
    )
  }

  @Test
  fun `findApprovedPremisesBeds returns FieldValidationError when premises characteristic has invalid scope`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_MATCHER)
          .produce()
      }

    val postcodeDistrict = PostCodeDistrictEntityFactory()
      .produce()

    every { mockPostcodeDistrictRepository.findByOutcode(postcodeDistrict.outcode) } returns postcodeDistrict

    val premisesCharacteristic = CharacteristicEntityFactory()
      .withPropertyName("hasEnSuite")
      .withServiceScope("temporary-accommodation")
      .withModelScope("premises")
      .produce()

    val roomCharacteristic = CharacteristicEntityFactory()
      .withPropertyName("isArsonSuitable")
      .withModelScope("room")
      .withServiceScope("approved-premises")
      .produce()

    every {
      mockCharacteristicService.getCharacteristicsByPropertyNames(
        listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!),
        ServiceName.approvedPremises,
      )
    } returns listOf(premisesCharacteristic, roomCharacteristic)

    val result = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      requiredCharacteristics = listOf(PlacementCriteria.hasEnSuite, PlacementCriteria.isArsonSuitable),
    )

    assertThat(result).isFieldValidationError(
      "$.requiredCharacteristics",
      "${premisesCharacteristic.propertyName} scopeInvalid",
    )
  }

  @Test
  fun `findApprovedPremisesBeds returns FieldValidationError when room characteristic does not exist`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_MATCHER)
          .produce()
      }

    val postcodeDistrict = PostCodeDistrictEntityFactory()
      .produce()

    every { mockPostcodeDistrictRepository.findByOutcode(postcodeDistrict.outcode) } returns postcodeDistrict

    val premisesCharacteristic = CharacteristicEntityFactory()
      .withPropertyName("hasEnSuite")
      .withServiceScope("approved-premises")
      .withModelScope("premises")
      .produce()

    val roomCharacteristicPropertyName = "isArsonSuitable"

    every {
      mockCharacteristicService.getCharacteristicsByPropertyNames(
        listOf(premisesCharacteristic.propertyName!!, roomCharacteristicPropertyName),
        ServiceName.approvedPremises,
      )
    } returns listOf(premisesCharacteristic)

    val result = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      requiredCharacteristics = listOf(PlacementCriteria.hasEnSuite, PlacementCriteria.isArsonSuitable),
    )

    assertThat(result).isFieldValidationError(
      "$.requiredCharacteristics",
      "$roomCharacteristicPropertyName doesNotExist",
    )
  }

  @Test
  fun `findApprovedPremisesBeds returns FieldValidationError when room characteristic has invalid scope`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_MATCHER)
          .produce()
      }

    val postcodeDistrict = PostCodeDistrictEntityFactory()
      .produce()

    every { mockPostcodeDistrictRepository.findByOutcode(postcodeDistrict.outcode) } returns postcodeDistrict

    val premisesCharacteristic = CharacteristicEntityFactory()
      .withPropertyName("isArsonSuitable")
      .withServiceScope("approved-premises")
      .withModelScope("premises")
      .produce()

    val roomCharacteristic = CharacteristicEntityFactory()
      .withPropertyName("hasEnSuite")
      .withServiceScope("temporary-accommodation")
      .withModelScope("room")
      .produce()

    every {
      mockCharacteristicService.getCharacteristicsByPropertyNames(
        listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!),
        ServiceName.approvedPremises,
      )
    } returns listOf(premisesCharacteristic, roomCharacteristic)

    val result = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      requiredCharacteristics = listOf(PlacementCriteria.isArsonSuitable, PlacementCriteria.hasEnSuite),
    )

    assertThat(result).isFieldValidationError(
      "$.requiredCharacteristics",
      "${roomCharacteristic.propertyName} scopeInvalid",
    )
  }

  @Test
  fun `findApprovedPremisesBeds returns FieldValidationError when postcode district does not exist`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_MATCHER)
          .produce()
      }

    val postcodeDistrict = PostCodeDistrictEntityFactory()
      .produce()

    every { mockPostcodeDistrictRepository.findByOutcode(postcodeDistrict.outcode) } returns null

    val premisesCharacteristic = CharacteristicEntityFactory()
      .withPropertyName("isArsonSuitable")
      .withServiceScope("approved-premises")
      .withModelScope("premises")
      .produce()

    val roomCharacteristic = CharacteristicEntityFactory()
      .withPropertyName("hasEnSuite")
      .withServiceScope("approved-premises")
      .withModelScope("room")
      .produce()

    every {
      mockCharacteristicService.getCharacteristicsByPropertyNames(
        listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!),
        ServiceName.approvedPremises,
      )
    } returns listOf(premisesCharacteristic, roomCharacteristic)

    every { mockCharacteristicService.getCharacteristicByPropertyName(roomCharacteristic.propertyName!!, ServiceName.approvedPremises) } returns roomCharacteristic

    val result = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      requiredCharacteristics = listOf(PlacementCriteria.isArsonSuitable, PlacementCriteria.hasEnSuite),
    )

    assertThat(result).isFieldValidationError("$.postcodeDistrictOutcode", "doesNotExist")
  }

  @Test
  fun `findApprovedPremisesBeds returns FieldValidationError when duration in days is less than 1`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_MATCHER)
          .produce()
      }

    val postcodeDistrict = PostCodeDistrictEntityFactory()
      .produce()

    every { mockPostcodeDistrictRepository.findByOutcode(postcodeDistrict.outcode) } returns postcodeDistrict

    val premisesCharacteristic = CharacteristicEntityFactory()
      .withPropertyName("hasEnSuite")
      .withServiceScope("approved-premises")
      .withModelScope("premises")
      .produce()

    val roomCharacteristic = CharacteristicEntityFactory()
      .withPropertyName("isArsonSuitable")
      .withServiceScope("approved-premises")
      .withModelScope("room")
      .produce()

    every {
      mockCharacteristicService.getCharacteristicsByPropertyNames(
        listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!),
        ServiceName.approvedPremises,
      )
    } returns listOf(premisesCharacteristic, roomCharacteristic)

    val result = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 0,
      requiredCharacteristics = listOf(PlacementCriteria.hasEnSuite, PlacementCriteria.isArsonSuitable),
    )

    assertThat(result).isFieldValidationError("$.durationDays", "mustBeAtLeast1")
  }

  @Test
  fun `findApprovedPremisesBeds returns FieldValidationError when max distance in miles is less than 1`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_MATCHER)
          .produce()
      }

    val postcodeDistrict = PostCodeDistrictEntityFactory()
      .produce()

    every { mockPostcodeDistrictRepository.findByOutcode(postcodeDistrict.outcode) } returns postcodeDistrict

    val premisesCharacteristic = CharacteristicEntityFactory()
      .withPropertyName("hasEnSuite")
      .withServiceScope("approved-premises")
      .withModelScope("premises")
      .produce()

    val roomCharacteristic = CharacteristicEntityFactory()
      .withPropertyName("isArsonSuitable")
      .withServiceScope("approved-premises")
      .withModelScope("room")
      .produce()

    every {
      mockCharacteristicService.getCharacteristicsByPropertyNames(
        listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!),
        ServiceName.approvedPremises,
      )
    } returns listOf(premisesCharacteristic, roomCharacteristic)

    val result = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 0,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      requiredCharacteristics = listOf(PlacementCriteria.hasEnSuite, PlacementCriteria.isArsonSuitable),
    )

    assertThat(result).isFieldValidationError("$.maxDistanceMiles", "mustBeAtLeast1")
  }

  @Test
  fun `findApprovedPremisesBeds returns results from repository`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_MATCHER)
          .produce()
      }

    val postcodeDistrict = PostCodeDistrictEntityFactory()
      .produce()

    every { mockPostcodeDistrictRepository.findByOutcode(postcodeDistrict.outcode) } returns postcodeDistrict

    val premisesCharacteristic = CharacteristicEntityFactory()
      .withPropertyName("hasEnSuite")
      .withServiceScope("approved-premises")
      .withModelScope("premises")
      .produce()

    val roomCharacteristic = CharacteristicEntityFactory()
      .withPropertyName("isArsonSuitable")
      .withServiceScope("approved-premises")
      .withModelScope("room")
      .produce()

    every {
      mockCharacteristicService.getCharacteristicsByPropertyNames(
        listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!),
        ServiceName.approvedPremises,
      )
    } returns listOf(premisesCharacteristic, roomCharacteristic)

    val repositorySearchResults = listOf(
      ApprovedPremisesBedSearchResult(
        premisesId = UUID.randomUUID(),
        premisesName = "Premises Name",
        premisesAddressLine1 = "1 Someplace",
        premisesAddressLine2 = null,
        premisesTown = null,
        premisesPostcode = "LA1111A",
        premisesCharacteristics = mutableListOf(
          CharacteristicNames(
            propertyName = "bedCharacteristicPropertyName",
            name = "Bed Characteristic Name",
          ),
        ),
        roomId = UUID.randomUUID(),
        roomName = "Room Name",
        bedId = UUID.randomUUID(),
        bedName = "Bed Name",
        roomCharacteristics = mutableListOf(
          CharacteristicNames(
            propertyName = "roomCharacteristicPropertyName",
            name = "Room Characteristic Name",
          ),
        ),
        distance = 12.3,
        premisesBedCount = 3,
      ),
    )

    every {
      mockBedSearchRepository.findApprovedPremisesBeds(
        postcodeDistrictOutcode = postcodeDistrict.outcode,
        maxDistanceMiles = 20,
        startDate = LocalDate.parse("2023-03-22"),
        durationInDays = 7,
        requiredPremisesCharacteristics = listOf(premisesCharacteristic.id),
        requiredRoomCharacteristics = listOf(roomCharacteristic.id),
      )
    } returns repositorySearchResults

    val result = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      requiredCharacteristics = listOf(PlacementCriteria.hasEnSuite, PlacementCriteria.isArsonSuitable),
    )

    assertThat(result).isSuccess().hasValueEqualTo(repositorySearchResults)
  }
}
