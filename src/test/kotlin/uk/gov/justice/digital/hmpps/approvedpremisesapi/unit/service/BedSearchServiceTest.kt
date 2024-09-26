package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PostCodeDistrictEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TurnaroundEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostcodeDistrictRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApprovedPremisesBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CharacteristicNames
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TemporaryAccommodationBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TemporaryAccommodationBedSearchResultOverlap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BedSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class BedSearchServiceTest {
  private val mockBedSearchRepository = mockk<BedSearchRepository>()
  private val mockPostcodeDistrictRepository = mockk<PostcodeDistrictRepository>()
  private val mockCharacteristicService = mockk<CharacteristicService>()
  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockWorkingDayService = mockk<WorkingDayService>()
  private val mockProbationDeliveryUnitRepository = mockk<ProbationDeliveryUnitRepository>()

  private val bedSearchService = BedSearchService(
    mockBedSearchRepository,
    mockPostcodeDistrictRepository,
    mockCharacteristicService,
    mockBookingRepository,
    mockWorkingDayService,
    mockProbationDeliveryUnitRepository,
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

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
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

    every { mockCharacteristicService.getCharacteristicsByPropertyNames(listOf("isESAP", premisesCharacteristicPropertyName)) } returns listOf(roomCharacteristic)

    val authorisableResult = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      requiredCharacteristics = listOf(PlacementCriteria.isESAP, PlacementCriteria.isRecoveryFocussed),
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val fieldValidationError = authorisableResult.entity as ValidatableActionResult.FieldValidationError

    assertThat(fieldValidationError.validationMessages["$.requiredCharacteristics"]).isEqualTo("$premisesCharacteristicPropertyName doesNotExist")
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

    every { mockCharacteristicService.getCharacteristicsByPropertyNames(listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!)) } returns listOf(premisesCharacteristic, roomCharacteristic)

    val authorisableResult = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      requiredCharacteristics = listOf(PlacementCriteria.hasEnSuite, PlacementCriteria.isArsonSuitable),
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val fieldValidationError = authorisableResult.entity as ValidatableActionResult.FieldValidationError

    assertThat(fieldValidationError.validationMessages["$.requiredCharacteristics"]).isEqualTo("${premisesCharacteristic.propertyName} scopeInvalid")
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

    every { mockCharacteristicService.getCharacteristicsByPropertyNames(listOf(premisesCharacteristic.propertyName!!, roomCharacteristicPropertyName)) } returns listOf(premisesCharacteristic)

    val authorisableResult = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      requiredCharacteristics = listOf(PlacementCriteria.hasEnSuite, PlacementCriteria.isArsonSuitable),
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val fieldValidationError = authorisableResult.entity as ValidatableActionResult.FieldValidationError

    assertThat(fieldValidationError.validationMessages["$.requiredCharacteristics"]).isEqualTo("$roomCharacteristicPropertyName doesNotExist")
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

    every { mockCharacteristicService.getCharacteristicsByPropertyNames(listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!)) } returns listOf(premisesCharacteristic, roomCharacteristic)

    val authorisableResult = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      requiredCharacteristics = listOf(PlacementCriteria.isArsonSuitable, PlacementCriteria.hasEnSuite),
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val fieldValidationError = authorisableResult.entity as ValidatableActionResult.FieldValidationError

    assertThat(fieldValidationError.validationMessages["$.requiredCharacteristics"]).isEqualTo("${roomCharacteristic.propertyName} scopeInvalid")
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

    every { mockCharacteristicService.getCharacteristicsByPropertyNames(listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!)) } returns listOf(premisesCharacteristic, roomCharacteristic)

    every { mockCharacteristicService.getCharacteristicByPropertyName(roomCharacteristic.propertyName!!) } returns roomCharacteristic

    val authorisableResult = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      requiredCharacteristics = listOf(PlacementCriteria.isArsonSuitable, PlacementCriteria.hasEnSuite),
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val fieldValidationError = authorisableResult.entity as ValidatableActionResult.FieldValidationError

    assertThat(fieldValidationError.validationMessages["$.postcodeDistrictOutcode"]).isEqualTo("doesNotExist")
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

    every { mockCharacteristicService.getCharacteristicsByPropertyNames(listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!)) } returns listOf(premisesCharacteristic, roomCharacteristic)

    val authorisableResult = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 0,
      requiredCharacteristics = listOf(PlacementCriteria.hasEnSuite, PlacementCriteria.isArsonSuitable),
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val fieldValidationError = authorisableResult.entity as ValidatableActionResult.FieldValidationError

    assertThat(fieldValidationError.validationMessages["$.durationDays"]).isEqualTo("mustBeAtLeast1")
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

    every { mockCharacteristicService.getCharacteristicsByPropertyNames(listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!)) } returns listOf(premisesCharacteristic, roomCharacteristic)

    val authorisableResult = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 0,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      requiredCharacteristics = listOf(PlacementCriteria.hasEnSuite, PlacementCriteria.isArsonSuitable),
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val fieldValidationError = authorisableResult.entity as ValidatableActionResult.FieldValidationError

    assertThat(fieldValidationError.validationMessages["$.maxDistanceMiles"]).isEqualTo("mustBeAtLeast1")
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

    every { mockCharacteristicService.getCharacteristicsByPropertyNames(listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!)) } returns listOf(premisesCharacteristic, roomCharacteristic)

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

    val authorisableResult = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      requiredCharacteristics = listOf(PlacementCriteria.hasEnSuite, PlacementCriteria.isArsonSuitable),
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.Success).isTrue
    val result = (authorisableResult.entity as ValidatableActionResult.Success).entity

    assertThat(result).isEqualTo(repositorySearchResults)
  }

  @Test
  fun `findTemporaryAccommodationBeds returns FieldValidationError when duration in days is less than 1`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
      .withName(randomStringMultiCaseWithNumbers(10))
      .withProbationRegion(user.probationRegion)
      .produce()

    every {
      mockProbationDeliveryUnitRepository.findByName(probationDeliveryUnit.name)
    } returns probationDeliveryUnit

    val authorisableResult = bedSearchService.findTemporaryAccommodationBeds(
      user = user,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 0,
      probationDeliveryUnit = probationDeliveryUnit.name,
      probationDeliveryUnits = null,
      propertyBedAttributes = null,
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val validationError = authorisableResult.entity as ValidatableActionResult.FieldValidationError

    assertThat(validationError.validationMessages["$.durationDays"]).isEqualTo("mustBeAtLeast1")
  }

  @Test
  fun `findTemporaryAccommodationBeds returns FieldValidationError when no pdu is provided`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val result = bedSearchService.findTemporaryAccommodationBeds(
      user = user,
      startDate = LocalDate.parse("2024-08-27"),
      durationInDays = 16,
      probationDeliveryUnit = null,
      probationDeliveryUnits = null,
      propertyBedAttributes = null,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity is ValidatableActionResult.FieldValidationError).isTrue
    val validationError = result.entity as ValidatableActionResult.FieldValidationError

    assertThat(validationError.validationMessages["$.probationDeliveryUnit"]).isEqualTo("empty")
  }

  @Test
  fun `findTemporaryAccommodationBeds returns FieldValidationError when number of pdus is greater than pdus limit`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val probationDeliveryUnitIds = ProbationDeliveryUnitEntityFactory()
      .withName(randomStringMultiCaseWithNumbers(10))
      .withProbationRegion(user.probationRegion)
      .produceMany()
      .take(7)
      .map { it.id }
      .toList()

    val result = bedSearchService.findTemporaryAccommodationBeds(
      user = user,
      startDate = LocalDate.parse("2024-08-22"),
      durationInDays = 30,
      probationDeliveryUnit = null,
      probationDeliveryUnits = probationDeliveryUnitIds,
      propertyBedAttributes = null,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity is ValidatableActionResult.FieldValidationError).isTrue
    val validationError = result.entity as ValidatableActionResult.FieldValidationError

    assertThat(validationError.validationMessages["$.probationDeliveryUnits"]).isEqualTo("maxNumberProbationDeliveryUnits")
  }

  @Test
  fun `findTemporaryAccommodationBeds returns FieldValidationError when a pdu does not exist`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val probationDeliveryUnitIds = ProbationDeliveryUnitEntityFactory()
      .withName(randomStringMultiCaseWithNumbers(10))
      .withProbationRegion(user.probationRegion)
      .produceMany()
      .take(3)
      .map { it.id }
      .toMutableList()

    val notExistPduId = UUID.randomUUID()
    probationDeliveryUnitIds.add(notExistPduId)

    every {
      mockProbationDeliveryUnitRepository.existsById(match { probationDeliveryUnitIds.contains(it) })
    } returns true

    every {
      mockProbationDeliveryUnitRepository.existsById(notExistPduId)
    } returns false

    val result = bedSearchService.findTemporaryAccommodationBeds(
      user = user,
      startDate = LocalDate.parse("2024-08-28"),
      durationInDays = 84,
      probationDeliveryUnit = null,
      probationDeliveryUnits = probationDeliveryUnitIds,
      propertyBedAttributes = null,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity is ValidatableActionResult.FieldValidationError).isTrue
    val validationError = result.entity as ValidatableActionResult.FieldValidationError

    assertThat(validationError.validationMessages["$.probationDeliveryUnits[3]"]).isEqualTo("doesNotExist")
  }

  @Test
  fun `findTemporaryAccommodationBeds returns results from repository`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
      .withName(randomStringMultiCaseWithNumbers(10))
      .withProbationRegion(user.probationRegion)
      .produce()

    val repositorySearchResults = listOf(
      TemporaryAccommodationBedSearchResult(
        premisesId = UUID.randomUUID(),
        premisesName = "Premises Name",
        premisesAddressLine1 = "1 Someplace",
        premisesAddressLine2 = null,
        premisesTown = null,
        premisesPostcode = "LA1111A",
        probationDeliveryUnitName = probationDeliveryUnit.name,
        premisesCharacteristics = mutableListOf(
          CharacteristicNames(
            propertyName = "bedCharacteristicPropertyName",
            name = "Bed Characteristic Name",
          ),
        ),
        premisesNotes = "Premises notes",
        premisesBedCount = 3,
        bookedBedCount = 0,
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
        overlaps = mutableListOf(
          TemporaryAccommodationBedSearchResultOverlap(
            crn = "crn0123456789",
            days = 7,
            premisesId = UUID.randomUUID(),
            roomId = UUID.randomUUID(),
            bookingId = UUID.randomUUID(),
          ),
        ),
      ),
    )

    every {
      mockBedSearchRepository.findTemporaryAccommodationBeds(
        startDate = LocalDate.parse("2023-03-22"),
        endDate = LocalDate.parse("2023-03-28"),
        probationDeliveryUnits = listOf(probationDeliveryUnit.id),
        probationRegionId = user.probationRegion.id,
        premisesCharacteristicsIds = listOf(),
      )
    } returns repositorySearchResults

    every { mockBookingRepository.findClosestBookingBeforeDateForBeds(any(), any()) } returns listOf()
    every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
    every { mockBookingRepository.findAllNotCancelledByPremisesIdsAndOverlappingDate(any(), any(), any()) } returns listOf()
    every {
      mockProbationDeliveryUnitRepository.findByName(probationDeliveryUnit.name)
    } returns probationDeliveryUnit

    val authorisableResult = bedSearchService.findTemporaryAccommodationBeds(
      user = user,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      probationDeliveryUnit = probationDeliveryUnit.name,
      probationDeliveryUnits = null,
      propertyBedAttributes = null,
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.Success).isTrue
    val result = (authorisableResult.entity as ValidatableActionResult.Success).entity

    assertThat(result).isEqualTo(repositorySearchResults)
  }

  @Test
  fun `findTemporaryAccommodationBeds does not return results for beds that currently have turnarounds`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
      .withName(randomStringMultiCaseWithNumbers(10))
      .withProbationRegion(user.probationRegion)
      .produce()

    val expectedResults = listOf(
      TemporaryAccommodationBedSearchResult(
        premisesId = UUID.randomUUID(),
        premisesName = "Premises Name",
        premisesAddressLine1 = "1 Someplace",
        premisesAddressLine2 = null,
        premisesTown = null,
        premisesPostcode = "LA1111A",
        probationDeliveryUnitName = probationDeliveryUnit.name,
        premisesCharacteristics = mutableListOf(
          CharacteristicNames(
            propertyName = "bedCharacteristicPropertyName",
            name = "Bed Characteristic Name",
          ),
        ),
        premisesNotes = "Premises notes",
        premisesBedCount = 3,
        bookedBedCount = 0,
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
        overlaps = mutableListOf(),
      ),
    )

    // This bed is in a turnaround
    val unexpectedResults = listOf(
      TemporaryAccommodationBedSearchResult(
        premisesId = UUID.randomUUID(),
        premisesName = "Another Premises Name",
        premisesAddressLine1 = "2 Someplace",
        premisesAddressLine2 = null,
        premisesTown = null,
        premisesPostcode = "LA1111A",
        premisesCharacteristics = mutableListOf(
          CharacteristicNames(
            propertyName = "bedCharacteristicPropertyName",
            name = "Bed Characteristic Name",
          ),
        ),
        premisesNotes = "Premises notes",
        premisesBedCount = 3,
        bookedBedCount = 0,
        roomId = UUID.randomUUID(),
        roomName = "Another Room Name",
        bedId = UUID.randomUUID(),
        bedName = "Another Bed Name",
        probationDeliveryUnitName = probationDeliveryUnit.name,
        roomCharacteristics = mutableListOf(
          CharacteristicNames(
            propertyName = "roomCharacteristicPropertyName",
            name = "Room Characteristic Name",
          ),
        ),
        overlaps = mutableListOf(),
      ),
    )

    val repositorySearchResults = expectedResults + unexpectedResults

    every {
      mockProbationDeliveryUnitRepository.findByName(probationDeliveryUnit.name)
    } returns probationDeliveryUnit

    every {
      mockBedSearchRepository.findTemporaryAccommodationBeds(
        startDate = LocalDate.parse("2023-03-22"),
        endDate = LocalDate.parse("2023-03-28"),
        probationDeliveryUnits = listOf(probationDeliveryUnit.id),
        probationRegionId = user.probationRegion.id,
        premisesCharacteristicsIds = listOf(),
      )
    } returns repositorySearchResults

    val expectedResultPremises = TemporaryAccommodationPremisesEntityFactory()
      .withId(expectedResults[0].premisesId)
      .withProbationRegion(user.probationRegion)
      .withLocalAuthorityArea(
        LocalAuthorityEntityFactory()
          .produce(),
      )
      .produce()

    val expectedResultRoom = RoomEntityFactory()
      .withId(expectedResults[0].roomId)
      .withPremises(expectedResultPremises)
      .produce()

    val expectedResultBed = BedEntityFactory()
      .withId(expectedResults[0].bedId)
      .withRoom(expectedResultRoom)
      .produce()

    val expectedResultBooking = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-12-19"))
      .withDepartureDate(LocalDate.parse("2023-03-19"))
      .withPremises(expectedResultPremises)
      .withBed(expectedResultBed)
      .produce()

    val expectedTurnaround = TurnaroundEntityFactory()
      .withBooking(expectedResultBooking)
      .withWorkingDayCount(2)
      .produce()

    expectedResultBooking.turnarounds = mutableListOf(expectedTurnaround)

    val unexpectedResultPremises = TemporaryAccommodationPremisesEntityFactory()
      .withId(unexpectedResults[0].premisesId)
      .withProbationRegion(user.probationRegion)
      .withLocalAuthorityArea(
        LocalAuthorityEntityFactory()
          .produce(),
      )
      .produce()

    val unexpectedResultRoom = RoomEntityFactory()
      .withId(unexpectedResults[0].roomId)
      .withPremises(unexpectedResultPremises)
      .produce()

    val unexpectedResultBed = BedEntityFactory()
      .withId(unexpectedResults[0].bedId)
      .withRoom(unexpectedResultRoom)
      .produce()

    val unexpectedResultBooking = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-12-20"))
      .withDepartureDate(LocalDate.parse("2023-03-20"))
      .withPremises(unexpectedResultPremises)
      .withBed(unexpectedResultBed)
      .produce()

    val unexpectedTurnaround = TurnaroundEntityFactory()
      .withBooking(unexpectedResultBooking)
      .withWorkingDayCount(2)
      .produce()

    unexpectedResultBooking.turnarounds = mutableListOf(unexpectedTurnaround)

    every {
      mockBookingRepository.findClosestBookingBeforeDateForBeds(
        date = any(),
        bedIds = any(),
      )
    } returns listOf(
      expectedResultBooking,
      unexpectedResultBooking,
    )

    every { mockWorkingDayService.addWorkingDays(any(), any()) } answers {
      (it.invocation.args[0] as LocalDate).plusDays((it.invocation.args[1] as Int).toLong())
    }

    every { mockBookingRepository.findAllNotCancelledByPremisesIdsAndOverlappingDate(any(), any(), any()) } returns listOf()

    val authorisableResult = bedSearchService.findTemporaryAccommodationBeds(
      user = user,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      probationDeliveryUnit = probationDeliveryUnit.name,
      probationDeliveryUnits = null,
      propertyBedAttributes = null,
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.Success).isTrue
    val result = (authorisableResult.entity as ValidatableActionResult.Success).entity

    assertThat(result).isEqualTo(expectedResults)
  }
}
