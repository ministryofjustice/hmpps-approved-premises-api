package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingNotMadeEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestBookingSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision

class PlacementRequestTransformerTest {
  private val mockAssessmentTransformer = mockk<AssessmentTransformer>()
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockRisksTransformer = mockk<RisksTransformer>()
  private val mockUserTransformer = mockk<UserTransformer>()
  private val mockBookingSummaryTransformer = mockk<PlacementRequestBookingSummaryTransformer>()

  private val placementRequestTransformer = PlacementRequestTransformer(
    mockPersonTransformer,
    mockRisksTransformer,
    mockAssessmentTransformer,
    mockUserTransformer,
    mockBookingSummaryTransformer,
  )

  private val offenderDetailSummary = OffenderDetailsSummaryFactory().produce()
  private val inmateDetail = InmateDetailFactory().produce()
  private val personInfo = PersonInfoResult.Success.Full(offenderDetailSummary.otherIds.crn, offenderDetailSummary, inmateDetail)
  private val mockBookingSummary = mockk<PlacementRequestBookingSummary>()

  private val user = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .produce()

  private val applicationSubmittedAt = OffsetDateTime.now().minusDays(12)
  private val assessmentSubmittedAt = OffsetDateTime.now().minusDays(3)

  private val application = ApprovedPremisesApplicationEntityFactory()
    .withReleaseType("licence")
    .withCreatedByUser(user)
    .withSubmittedAt(applicationSubmittedAt)
    .produce()

  private val assessment = ApprovedPremisesAssessmentEntityFactory()
    .withAllocatedToUser(user)
    .withApplication(application)
    .withSubmittedAt(assessmentSubmittedAt)
    .produce()

  private val placementRequirementsFactory = PlacementRequirementsEntityFactory()
    .withApplication(application)
    .withAssessment(assessment)
    .withEssentialCriteria(
      listOf(
        CharacteristicEntityFactory().withPropertyName("isSemiSpecialistMentalHealth").produce(),
        CharacteristicEntityFactory().withPropertyName("isRecoveryFocussed").produce(),
        CharacteristicEntityFactory().withPropertyName("someOtherPropertyName").produce(),
      ),
    )
    .withDesirableCriteria(
      listOf(
        CharacteristicEntityFactory().withPropertyName("isWheelchairDesignated").produce(),
        CharacteristicEntityFactory().withPropertyName("isSingle").produce(),
        CharacteristicEntityFactory().withPropertyName("hasEnSuite").produce(),
        CharacteristicEntityFactory().withPropertyName("somethingElse").produce(),
      ),
    )

  private val placementRequestFactory = PlacementRequestEntityFactory()
    .withApplication(application)
    .withAssessment(assessment)
    .withAllocatedToUser(user)

  private val mockRisks = mockk<PersonRisks>()
  private val mockPersonInfo = mockk<Person>()

  private val mockUser = mockk<ApprovedPremisesUser>()
  private val decision = ApiAssessmentDecision.accepted

  @BeforeEach
  fun setup() {
    every { mockAssessmentTransformer.transformJpaDecisionToApi(assessment.decision) } returns decision
    every { mockPersonTransformer.transformModelToPersonApi(personInfo) } returns mockPersonInfo
    every { mockRisksTransformer.transformDomainToApi(application.riskRatings!!, application.crn) } returns mockRisks
    every { mockUserTransformer.transformJpaToApi(user, ServiceName.approvedPremises) } returns mockUser
  }

  @Nested
  inner class TransformJpaToApi {

    @Test
    fun `transforms a basic placement request entity`() {
      val placementRequirementsEntity = placementRequirementsFactory
        .withEssentialCriteria(
          listOf(
            CharacteristicEntityFactory().withPropertyName("isSemiSpecialistMentalHealth").produce(),
            CharacteristicEntityFactory().withPropertyName("isRecoveryFocussed").produce(),
            CharacteristicEntityFactory().withPropertyName("someOtherPropertyName").produce(),
          ),
        )
        .withDesirableCriteria(
          listOf(
            CharacteristicEntityFactory().withPropertyName("isWheelchairDesignated").produce(),
            CharacteristicEntityFactory().withPropertyName("isSingle").produce(),
            CharacteristicEntityFactory().withPropertyName("hasEnSuite").produce(),
            CharacteristicEntityFactory().withPropertyName("somethingElse").produce(),
          ),
        )
        .produce()

      val placementRequestEntity = placementRequestFactory
        .withPlacementRequirements(placementRequirementsEntity)
        .withNotes("Some notes")
        .produce()

      val result = placementRequestTransformer.transformJpaToApi(
        placementRequestEntity,
        PersonInfoResult.Success.Full(offenderDetailSummary.otherIds.crn, offenderDetailSummary, inmateDetail),
      )

      assertThat(result).isEqualTo(
        PlacementRequest(
          id = placementRequestEntity.id,
          gender = placementRequirementsEntity.gender,
          type = placementRequirementsEntity.apType,
          expectedArrival = placementRequestEntity.expectedArrival,
          duration = placementRequestEntity.duration,
          location = placementRequirementsEntity.postcodeDistrict.outcode,
          radius = placementRequirementsEntity.radius,
          essentialCriteria = listOf(
            PlacementCriteria.isSemiSpecialistMentalHealth,
            PlacementCriteria.isRecoveryFocussed,
          ),
          desirableCriteria = listOf(
            PlacementCriteria.isWheelchairDesignated,
            PlacementCriteria.isSingle,
            PlacementCriteria.hasEnSuite,
          ),
          person = mockPersonInfo,
          risks = mockRisks,
          applicationId = application.id,
          assessmentId = assessment.id,
          releaseType = ReleaseTypeOption.licence,
          status = PlacementRequestStatus.notMatched,
          assessmentDecision = decision,
          assessmentDate = assessmentSubmittedAt.toInstant(),
          applicationDate = applicationSubmittedAt.toInstant(),
          assessor = mockUser,
          notes = placementRequestEntity.notes,
          isParole = placementRequestEntity.isParole,
          requestType = PlacementRequestRequestType.standardRelease,
          booking = null,
          isWithdrawn = false,
          withdrawalReason = null,
        ),
      )
    }

    @Test
    fun `returns a status of matched when a placement request has a booking`() {
      val premises = ApprovedPremisesEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
        .produce()

      val booking = BookingEntityFactory()
        .withServiceName(ServiceName.approvedPremises)
        .withPremises(premises)
        .produce()

      val placementRequirementsEntity = placementRequirementsFactory.produce()

      val placementRequestEntity = placementRequestFactory
        .withPlacementRequirements(placementRequirementsEntity)
        .withBooking(booking)
        .produce()

      every { mockBookingSummaryTransformer.transformJpaToApi(booking) } returns mockBookingSummary

      val result = placementRequestTransformer.transformJpaToApi(placementRequestEntity, personInfo)

      assertThat(result.status).isEqualTo(PlacementRequestStatus.matched)
      assertThat(result.booking).isEqualTo(mockBookingSummary)
    }

    @Test
    fun `returns a status of unableToMatch when a placement request has a bookingNotMade entity`() {
      val placementRequirementsEntity = placementRequirementsFactory.produce()

      val placementRequestEntity = placementRequestFactory
        .withPlacementRequirements(placementRequirementsEntity)
        .produce()

      val bookingNotMade = BookingNotMadeEntityFactory()
        .withPlacementRequest(placementRequestEntity)
        .produce()

      placementRequestEntity.bookingNotMades = mutableListOf(bookingNotMade)

      val result = placementRequestTransformer.transformJpaToApi(placementRequestEntity, personInfo)

      assertThat(result.status).isEqualTo(PlacementRequestStatus.unableToMatch)
    }

    @Test
    fun `returns a requestStatus of parole when a placement request is for parole`() {
      val placementRequestEntity = placementRequestFactory
        .withPlacementRequirements(placementRequirementsFactory.produce())
        .withIsParole(true)
        .produce()

      val result = placementRequestTransformer.transformJpaToApi(placementRequestEntity, personInfo)

      assertThat(result.requestType).isEqualTo(PlacementRequestRequestType.parole)
    }

    @Test
    fun `returns a requestStatus of standardRelease when a placement request is not for parole`() {
      val placementRequestEntity = placementRequestFactory
        .withPlacementRequirements(placementRequirementsFactory.produce())
        .withIsParole(false)
        .produce()

      val result = placementRequestTransformer.transformJpaToApi(placementRequestEntity, personInfo)

      assertThat(result.requestType).isEqualTo(PlacementRequestRequestType.standardRelease)
    }

    @Test
    fun `returns a status of notMatched when a placement request has a cancelled booking`() {
      val premises = ApprovedPremisesEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
        .produce()

      val booking = BookingEntityFactory()
        .withServiceName(ServiceName.approvedPremises)
        .withPremises(premises)
        .produce()

      booking.let {
        it.cancellations = mutableListOf(
          CancellationEntityFactory().withBooking(it).withReason(
            CancellationReasonEntityFactory().produce(),
          ).produce(),
        )
      }

      val placementRequirementsEntity = placementRequirementsFactory.produce()

      val placementRequestEntity = placementRequestFactory
        .withPlacementRequirements(placementRequirementsEntity)
        .withBooking(booking)
        .produce()

      every { mockBookingSummaryTransformer.transformJpaToApi(booking) } returns mockBookingSummary

      val result = placementRequestTransformer.transformJpaToApi(
        placementRequestEntity,
        PersonInfoResult.Success.Full(offenderDetailSummary.otherIds.crn, offenderDetailSummary, inmateDetail),
      )

      assertThat(result.status).isEqualTo(PlacementRequestStatus.notMatched)
      assertThat(result.booking).isNull()
    }

    @ParameterizedTest
    @EnumSource(value = ReleaseTypeOption::class)
    fun `release types are transformed correctly`(releaseTypeOption: ReleaseTypeOption) {
      val placementRequirementsEntity = placementRequirementsFactory
        .withEssentialCriteria(
          listOf(
            CharacteristicEntityFactory().withPropertyName("isSemiSpecialistMentalHealth").produce(),
            CharacteristicEntityFactory().withPropertyName("isRecoveryFocussed").produce(),
            CharacteristicEntityFactory().withPropertyName("someOtherPropertyName").produce(),
          ),
        )
        .withDesirableCriteria(
          listOf(
            CharacteristicEntityFactory().withPropertyName("isWheelchairDesignated").produce(),
            CharacteristicEntityFactory().withPropertyName("isSingle").produce(),
            CharacteristicEntityFactory().withPropertyName("hasEnSuite").produce(),
            CharacteristicEntityFactory().withPropertyName("somethingElse").produce(),
          ),
        )
        .produce()

      val placementRequestEntity = placementRequestFactory
        .withPlacementRequirements(placementRequirementsEntity)
        .withNotes("Some notes")
        .produce()

      application.releaseType = releaseTypeOption.name

      val result = placementRequestTransformer.transformJpaToApi(
        placementRequestEntity,
        PersonInfoResult.Success.Full(offenderDetailSummary.otherIds.crn, offenderDetailSummary, inmateDetail),
      )

      assertThat(result.releaseType).isEqualTo(releaseTypeOption)
    }

    @Test
    fun `returns a withdrawn status of true when a placement request is withdrawn`() {
      val placementRequestEntity = placementRequestFactory
        .withPlacementRequirements(placementRequirementsFactory.produce())
        .withIsWithdrawn(true)
        .withWithdrawalReason(PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
        .produce()

      val result = placementRequestTransformer.transformJpaToApi(placementRequestEntity, personInfo)

      assertThat(result.isWithdrawn).isEqualTo(true)
      assertThat(result.withdrawalReason).isEqualTo(WithdrawPlacementRequestReason.duplicatePlacementRequest)
    }

    @Test
    fun `delegates mapping of legacy booking`() {
      val booking = BookingEntityFactory().withDefaults().produce()

      val placementRequestEntity = placementRequestFactory
        .withPlacementRequirements(placementRequirementsFactory.produce())
        .withBooking(booking)
        .withSpaceBookings(mutableListOf())
        .produce()

      every { mockBookingSummaryTransformer.transformJpaToApi(booking) } returns mockBookingSummary

      val result = placementRequestTransformer.transformJpaToApi(placementRequestEntity, personInfo)

      assertThat(result.booking).isEqualTo(mockBookingSummary)
    }

    @Test
    fun `delegates mapping of space booking`() {
      val spaceBooking = Cas1SpaceBookingEntityFactory().produce()

      val placementRequestEntity = placementRequestFactory
        .withPlacementRequirements(placementRequirementsFactory.produce())
        .withBooking(null)
        .withSpaceBookings(mutableListOf(spaceBooking))
        .produce()

      every { mockBookingSummaryTransformer.transformJpaToApi(spaceBooking) } returns mockBookingSummary

      val result = placementRequestTransformer.transformJpaToApi(placementRequestEntity, personInfo)

      assertThat(result.booking).isEqualTo(mockBookingSummary)
    }
  }

  @Nested
  inner class TransformToWithdrawable {

    @Test
    fun `transforms a placement request entity`() {
      val id = UUID.randomUUID()

      val placementRequestEntity = placementRequestFactory
        .withId(id)
        .withPlacementRequirements(placementRequirementsFactory.produce())
        .withNotes("Some notes")
        .withExpectedArrival(LocalDate.of(2023, 12, 11))
        .withDuration(30)
        .produce()

      val result = placementRequestTransformer.transformToWithdrawable(placementRequestEntity)

      assertThat(result).isEqualTo(
        Withdrawable(
          id,
          WithdrawableType.placementRequest,
          listOf(
            DatePeriod(
              LocalDate.of(2023, 12, 11),
              LocalDate.of(2024, 1, 10),
            ),
          ),
        ),
      )
    }
  }
}
