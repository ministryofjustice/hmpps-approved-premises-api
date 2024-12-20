package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDaySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDaySummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingDaySummaryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingDaySummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1.Cas1SpaceBookingDaySummarySearchResultImpl
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1SpaceBookingDaySummaryServiceTest {

  private val userAccessService = mockk<UserAccessService>()
  private val cas1SpaceBookingRepository = mockk<Cas1SpaceBookingRepository>()
  private val characteristicService = mockk<CharacteristicService>()
  private val cas1SpaceBookingDaySummaryTransformer = mockk<Cas1SpaceBookingDaySummaryTransformer>()
  private val offenderService = mockk<OffenderService>()
  private val userService = mockk<UserService>()
  private val cas1PremisesService = mockk<Cas1PremisesService>()

  private val service = Cas1SpaceBookingDaySummaryService(
    userAccessService,
    cas1SpaceBookingRepository,
    characteristicService,
    cas1SpaceBookingDaySummaryTransformer,
    offenderService,
    userService,
    cas1PremisesService,
  )

  @Nested
  inner class GetBookingDaySummaries {

    @Test
    fun `returns not found error if premises with the given Id does not exist`() {
      every { cas1PremisesService.findPremiseById(any()) } returns null

      val result = service.getBookingDaySummaries(
        UUID.randomUUID(),
        LocalDate.now(),
        emptyList(),
        Cas1SpaceBookingDaySummarySortField.PERSON_NAME,
        SortDirection.desc,
      )

      assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
      assertThat((result as CasResult.NotFound).entityType).isEqualTo("premises")
    }
  }

  @Test
  fun `returns booking day summaries when bookings exist`() {
    val premisesId = UUID.randomUUID()
    val date = LocalDate.now()
    val sort = Sort.by(Sort.Direction.DESC, Cas1SpaceBookingDaySummarySortField.PERSON_NAME.value)

    val spaceBooking1Summary = Cas1SpaceBookingDaySummarySearchResultImpl(
      UUID.randomUUID(),
      "crn1",
      LocalDate.now().minusDays(3),
      LocalDate.now().plusDays(3),
      tier = "A1",
      releaseType = "rotl",
      characteristicsPropertyNames = "isArsonSuitable,hasEnSuite",
    )
    val spaceBooking2Summary = Cas1SpaceBookingDaySummarySearchResultImpl(
      UUID.randomUUID(),
      "crn2",
      LocalDate.now().minusDays(2),
      LocalDate.now().plusDays(2),
      tier = "A2",
      releaseType = "licence",
      characteristicsPropertyNames = "isSingle",
    )
    val spaceBookingSummaries = listOf(spaceBooking1Summary, spaceBooking2Summary)

    val person1CaseSummary = CaseSummaryFactory().produce()
    val person2CaseSummary = CaseSummaryFactory().produce()
    val personSummaries = listOf(
      PersonSummaryInfoResult.Success.Full("crn1", person1CaseSummary),
      PersonSummaryInfoResult.Success.Full("crn2", person2CaseSummary),
    )
    val person1Summary = PersonTransformer()
      .personSummaryInfoToPersonSummary(
        PersonSummaryInfoResult.Success.Full("crn1", person1CaseSummary),
      )
    val person2Summary = PersonTransformer()
      .personSummaryInfoToPersonSummary(
        PersonSummaryInfoResult.Success.Full("crn2", person2CaseSummary),
      )

    val booking1DaySummary = Cas1SpaceBookingDaySummary(
      id = UUID.randomUUID(),
      person = person1Summary,
      canonicalArrivalDate = LocalDate.now().minusDays(6),
      canonicalDepartureDate = LocalDate.now().plusDays(6),
      tier = "A2",
      releaseType = "rotl",
      essentialCharacteristics = listOf(Cas1SpaceBookingCharacteristic.IS_SINGLE, Cas1SpaceBookingCharacteristic.HAS_EN_SUITE),
    )

    val booking2DaySummary = Cas1SpaceBookingDaySummary(
      id = UUID.randomUUID(),
      person = person2Summary,
      canonicalArrivalDate = LocalDate.now().minusDays(3),
      canonicalDepartureDate = LocalDate.now().plusDays(36),
      tier = "B3",
      releaseType = "licence",
      essentialCharacteristics = listOf(Cas1SpaceBookingCharacteristic.IS_SINGLE),
    )

    val roomCharacteristic = CharacteristicEntityFactory()
      .withPropertyName("isArsonSuitable")
      .withModelScope("room")
      .withServiceScope("approved-premises")
      .produce()

    val user = UserEntityFactory()
      .withDefaults()
      .produce()

    every { cas1PremisesService.findPremiseById(premisesId) } returns ApprovedPremisesEntityFactory().withDefaults().produce()
    every { cas1SpaceBookingRepository.findAllPremisesBookingsForDate(premisesId, date, any(), sort, any()) } returns spaceBookingSummaries
    every { characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises) } returns listOf(roomCharacteristic)
    every { userService.getUserForRequest() } returns user
    every { offenderService.getPersonSummaryInfoResults(any(), any()) } returns personSummaries
    every { cas1SpaceBookingDaySummaryTransformer.toCas1SpaceBookingDaySummary(spaceBookingSummaries[0], any()) } returns booking1DaySummary
    every { cas1SpaceBookingDaySummaryTransformer.toCas1SpaceBookingDaySummary(spaceBookingSummaries[1], any()) } returns booking2DaySummary

    val result = service.getBookingDaySummaries(
      premisesId,
      LocalDate.now(),
      listOf(Cas1SpaceBookingCharacteristic.IS_SINGLE),
      Cas1SpaceBookingDaySummarySortField.PERSON_NAME,
      SortDirection.desc,
    )

    assertThat(result).isInstanceOf(CasResult.Success::class.java)
    result as CasResult.Success
    val daySummaries = result.value
    assertThat(daySummaries.size).isEqualTo(2)
  }
}
