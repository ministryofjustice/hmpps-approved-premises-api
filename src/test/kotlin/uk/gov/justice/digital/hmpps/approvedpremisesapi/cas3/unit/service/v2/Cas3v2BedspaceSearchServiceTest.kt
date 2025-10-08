package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.v2

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.v2.Cas3v2TurnaroundEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2OverlapBookingsSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.Cas3BedspaceSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.Cas3CharacteristicNames
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.Cas3v2CandidateBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.Cas3v2CandidateBedspaceOverlap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BedspaceSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class Cas3v2BedspaceSearchServiceTest {
  private val mockCas3BedspaceSearchRepository = mockk<Cas3BedspaceSearchRepository>()
  private val mockCharacteristicService = mockk<CharacteristicService>()
  private val mockCas3v2BookingRepository = mockk<Cas3v2BookingRepository>()
  private val mockWorkingDayService = mockk<WorkingDayService>()
  private val mockProbationDeliveryUnitRepository = mockk<ProbationDeliveryUnitRepository>()
  private val mockOffenderService = mockk<OffenderService>()

  private val cas3v2BedspaceSearchService = Cas3v2BedspaceSearchService(
    mockCas3BedspaceSearchRepository,
    mockCas3v2BookingRepository,
    mockProbationDeliveryUnitRepository,
    mockCharacteristicService,
    mockWorkingDayService,
    mockOffenderService,
  )

  @Test
  fun `searchBedspaces returns FieldValidationError when duration in days is less than 1`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
      .withName(randomStringMultiCaseWithNumbers(10))
      .withProbationRegion(user.probationRegion)
      .produce()

    every {
      mockProbationDeliveryUnitRepository.existsById(probationDeliveryUnit.id)
    } returns true

    val result = cas3v2BedspaceSearchService.searchBedspaces(
      user = user,
      Cas3BedspaceSearchParameters(
        startDate = LocalDate.parse("2023-03-22"),
        durationDays = 0,
        probationDeliveryUnits = listOf(probationDeliveryUnit.id),
      ),
    )

    assertThatCasResult(result).isFieldValidationError("$.durationDays", "mustBeAtLeast1")
  }

  @Test
  fun `searchBedspaces returns FieldValidationError when number of pdus is greater than pdus limit`() {
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

    val result = cas3v2BedspaceSearchService.searchBedspaces(
      user = user,
      Cas3BedspaceSearchParameters(
        startDate = LocalDate.parse("2024-08-22"),
        durationDays = 30,
        probationDeliveryUnits = probationDeliveryUnitIds,
      ),
    )
    assertThatCasResult(result).isFieldValidationError("$.probationDeliveryUnits", "maxNumberProbationDeliveryUnits")
  }

  @Test
  fun `searchBedspaces returns FieldValidationError when a pdu does not exist`() {
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

    val result = cas3v2BedspaceSearchService.searchBedspaces(
      user = user,
      Cas3BedspaceSearchParameters(
        startDate = LocalDate.parse("2024-08-28"),
        durationDays = 84,
        probationDeliveryUnits = probationDeliveryUnitIds,
      ),
    )
    assertThatCasResult(result).isFieldValidationError("$.probationDeliveryUnits[3]", "doesNotExist")
  }

  @Test
  fun `searchBedspaces returns results from repository`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
      .withName(randomStringMultiCaseWithNumbers(10))
      .withProbationRegion(user.probationRegion)
      .produce()

    val caseSummary = CaseSummaryFactory()
      .produce()

    val overlapBookingsSearchResult = TestOverlapBookingsSearchResult(
      bookingId = UUID.randomUUID(),
      crn = caseSummary.crn,
      arrivalDate = LocalDate.parse("2023-02-15"),
      departureDate = LocalDate.parse("2023-03-10"),
      premisesId = UUID.randomUUID(),
      bedspaceId = UUID.randomUUID(),
      assessmentId = UUID.randomUUID(),
      sexualRisk = false,
    )

    val candidateBedspaces = listOf(
      Cas3v2CandidateBedspace(
        premisesId = overlapBookingsSearchResult.premisesId,
        premisesName = "Premises Name",
        premisesAddressLine1 = "1 Someplace",
        premisesAddressLine2 = null,
        premisesTown = null,
        premisesPostcode = "LA1111A",
        probationDeliveryUnitName = probationDeliveryUnit.name,
        premisesCharacteristics = mutableListOf(
          Cas3CharacteristicNames(
            name = "bedCharacteristicPropertyName",
            description = "Bed Characteristic Name",
          ),
        ),
        premisesNotes = "Premises notes",
        premisesBedspaceCount = 3,
        bookedBedspaceCount = 0,
        bedspaceId = overlapBookingsSearchResult.bedspaceId,
        bedspaceReference = "Bedspace Ref",
        bedspaceCharacteristics = mutableListOf(
          Cas3CharacteristicNames(
            name = "roomCharacteristicPropertyName",
            description = "Bedspace Characteristic Name",
          ),
        ),
        overlaps = mutableListOf(
          Cas3v2CandidateBedspaceOverlap(
            name = caseSummary.name.forename,
            sex = caseSummary.gender,
            personType = PersonType.fullPerson,
            crn = overlapBookingsSearchResult.crn,
            days = 7,
            premisesId = overlapBookingsSearchResult.premisesId,
            bedspaceId = overlapBookingsSearchResult.bedspaceId,
            bookingId = overlapBookingsSearchResult.bookingId,
            assessmentId = overlapBookingsSearchResult.assessmentId,
            isSexualRisk = false,
          ),
        ),
      ),
    )

    every {
      mockCas3BedspaceSearchRepository.searchBedspaces(
        startDate = LocalDate.parse("2023-03-22"),
        endDate = LocalDate.parse("2023-03-28"),
        probationDeliveryUnits = listOf(probationDeliveryUnit.id),
        probationRegionId = user.probationRegion.id,
      )
    } returns candidateBedspaces

    every { mockCas3v2BookingRepository.findClosestBookingBeforeDateForBedspaces(any(), any()) } returns listOf()
    every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
    every { mockCas3v2BookingRepository.findAllNotCancelledByPremisesIdsAndOverlappingDate(any(), any(), any()) } returns
      listOf(overlapBookingsSearchResult)
    every {
      mockProbationDeliveryUnitRepository.existsById(probationDeliveryUnit.id)
    } returns true
    every { mockOffenderService.getPersonSummaryInfoResults(setOf(caseSummary.crn), any()) } returns
      listOf(PersonSummaryInfoResult.Success.Full(caseSummary.crn, caseSummary))

    val result = cas3v2BedspaceSearchService.searchBedspaces(
      user = user,
      Cas3BedspaceSearchParameters(
        probationDeliveryUnits = listOf(probationDeliveryUnit.id),
        startDate = LocalDate.parse("2023-03-22"),
        durationDays = 7,
      ),
    )

    assertThatCasResult(result).isSuccess().hasValueEqualTo(candidateBedspaces)
  }

  @Test
  fun `searchBedspaces does not return results for beds that currently have turnarounds`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
      .withName(randomStringMultiCaseWithNumbers(10))
      .withProbationRegion(user.probationRegion)
      .produce()

    val expectedResults = listOf(
      Cas3v2CandidateBedspace(
        premisesId = UUID.randomUUID(),
        premisesName = "Premises Name",
        premisesAddressLine1 = "1 Someplace",
        premisesAddressLine2 = null,
        premisesTown = null,
        premisesPostcode = "LA1111A",
        probationDeliveryUnitName = probationDeliveryUnit.name,
        premisesCharacteristics = mutableListOf(
          Cas3CharacteristicNames(
            name = "bedCharacteristicPropertyName",
            description = "Bed Characteristic Name",
          ),
        ),
        premisesNotes = "Premises notes",
        premisesBedspaceCount = 3,
        bookedBedspaceCount = 0,
        bedspaceId = UUID.randomUUID(),
        bedspaceReference = "Bedspace Ref",
        bedspaceCharacteristics = mutableListOf(
          Cas3CharacteristicNames(
            name = "roomCharacteristicPropertyName",
            description = "Bedspace Characteristic Name",
          ),
        ),
        overlaps = mutableListOf(),
      ),
    )

    // This bed is in a turnaround
    val unexpectedResults = listOf(
      Cas3v2CandidateBedspace(
        premisesId = UUID.randomUUID(),
        premisesName = "Another Premises Name",
        premisesAddressLine1 = "2 Someplace",
        premisesAddressLine2 = null,
        premisesTown = null,
        premisesPostcode = "LA1111A",
        premisesCharacteristics = mutableListOf(
          Cas3CharacteristicNames(
            name = "bedCharacteristicPropertyName",
            description = "Bed Characteristic Name",
          ),
        ),
        premisesNotes = "Premises notes",
        premisesBedspaceCount = 3,
        bookedBedspaceCount = 0,
        bedspaceId = UUID.randomUUID(),
        bedspaceReference = "Another Bedspace Name",
        probationDeliveryUnitName = probationDeliveryUnit.name,
        bedspaceCharacteristics = mutableListOf(
          Cas3CharacteristicNames(
            name = "roomCharacteristicPropertyName",
            description = "Bedspace Characteristic Name",
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
      mockCas3BedspaceSearchRepository.searchBedspaces(
        startDate = LocalDate.parse("2023-03-22"),
        endDate = LocalDate.parse("2023-03-28"),
        probationDeliveryUnits = listOf(probationDeliveryUnit.id),
        probationRegionId = user.probationRegion.id,
      )
    } returns repositorySearchResults

    val expectedResultPremises = Cas3PremisesEntityFactory()
      .withId(expectedResults[0].premisesId)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(user.probationRegion)
          .produce(),
      )
      .withLocalAuthorityArea(
        LocalAuthorityEntityFactory()
          .produce(),
      )
      .produce()

    val expectedResultBed = Cas3BedspaceEntityFactory()
      .withPremises(expectedResultPremises)
      .withId(expectedResults[0].bedspaceId)
      .produce()

    val expectedResultBooking = Cas3BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-12-19"))
      .withDepartureDate(LocalDate.parse("2023-03-19"))
      .withPremises(expectedResultPremises)
      .withBedspace(expectedResultBed)
      .produce()

    val expectedTurnaround = Cas3v2TurnaroundEntityFactory()
      .withBooking(expectedResultBooking)
      .withWorkingDayCount(2)
      .produce()

    expectedResultBooking.turnarounds = mutableListOf(expectedTurnaround)

    val unexpectedResultPremises = Cas3PremisesEntityFactory()
      .withId(unexpectedResults[0].premisesId)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(user.probationRegion)
          .produce(),
      )
      .withLocalAuthorityArea(
        LocalAuthorityEntityFactory()
          .produce(),
      )
      .produce()

    val unexpectedResultBed = Cas3BedspaceEntityFactory()
      .withPremises(unexpectedResultPremises)
      .withId(unexpectedResults[0].bedspaceId)
      .produce()

    val unexpectedResultBooking = Cas3BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-12-20"))
      .withDepartureDate(LocalDate.parse("2023-03-20"))
      .withPremises(unexpectedResultPremises)
      .withBedspace(unexpectedResultBed)
      .produce()

    val unexpectedTurnaround = Cas3v2TurnaroundEntityFactory()
      .withBooking(unexpectedResultBooking)
      .withWorkingDayCount(2)
      .produce()

    unexpectedResultBooking.turnarounds = mutableListOf(unexpectedTurnaround)

    every {
      mockCas3v2BookingRepository.findClosestBookingBeforeDateForBedspaces(
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

    every { mockCas3v2BookingRepository.findAllNotCancelledByPremisesIdsAndOverlappingDate(any(), any(), any()) } returns listOf()

    every { mockOffenderService.getPersonSummaryInfoResults(any(), any()) } returns listOf()

    every {
      mockProbationDeliveryUnitRepository.existsById(probationDeliveryUnit.id)
    } returns true

    val result = cas3v2BedspaceSearchService.searchBedspaces(
      user = user,
      Cas3BedspaceSearchParameters(
        startDate = LocalDate.parse("2023-03-22"),
        durationDays = 7,
        probationDeliveryUnits = listOf(probationDeliveryUnit.id),
      ),
    )
    assertThatCasResult(result).isSuccess().hasValueEqualTo(expectedResults)
  }

  @Suppress("LongParameterList")
  class TestOverlapBookingsSearchResult(
    override val bookingId: UUID,
    override val crn: String,
    override val arrivalDate: LocalDate,
    override val departureDate: LocalDate,
    override val premisesId: UUID,
    override val bedspaceId: UUID,
    override val assessmentId: UUID,
    override val sexualRisk: Boolean,
  ) : Cas3v2OverlapBookingsSearchResult
}
