package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayCountService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementType as ApiPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType as JpaPlacementType

class TaskTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockUserTransformer = mockk<UserTransformer>()
  private val mockRisksTransformer = mockk<RisksTransformer>()
  private val mockPlacementRequestTransformer = mockk<PlacementRequestTransformer>()
  private val mockApAreaTransformer = mockk<ApAreaTransformer>()

  private val mockUser = mockk<ApprovedPremisesUser>()
  private val mockOffenderDetailSummary = mockk<OffenderDetailSummary>()
  private val mockInmateDetail = mockk<InmateDetail>()
  private val mockWorkingDayCountService = mockk<WorkingDayCountService>()

  private val user = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private val applicationFactory = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(user)
    .withRiskRatings(
      PersonRisksFactory()
        .withTier(
          RiskWithStatus(
            RiskTier(
              level = "M1",
              lastUpdated = LocalDate.parse("2023-06-26"),
            ),
          ),
        )
        .produce(),
    )

  private val application = applicationFactory.produce()

  private val assessmentFactory = ApprovedPremisesAssessmentEntityFactory()
    .withApplication(application)
    .withAllocatedToUser(user)
    .withCreatedAt(OffsetDateTime.parse("2022-12-07T10:40:00Z"))

  private val placementRequestFactory = PlacementRequestEntityFactory()
    .withPlacementRequirements(
      PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessmentFactory.produce())
        .produce(),
    )
    .withApplication(application)
    .withAssessment(assessmentFactory.produce())
    .withAllocatedToUser(user)

  val placementApplicationFactory = PlacementApplicationEntityFactory()
    .withApplication(application)
    .withAllocatedToUser(user)
    .withCreatedByUser(user)

  private val mockApArea = ApArea(UUID.randomUUID(), "someIdentifier", "someName")

  private val taskTransformer = TaskTransformer(
    mockUserTransformer,
    mockRisksTransformer,
    mockPlacementRequestTransformer,
    mockWorkingDayCountService,
    mockApAreaTransformer,
  )

  @BeforeEach
  fun setup() {
    every { mockUserTransformer.transformJpaToApi(user, ServiceName.approvedPremises) } returns mockUser
    every { mockWorkingDayCountService.addWorkingDays(any(), any()) } returns LocalDate.now().plusDays(2)
    every { mockApAreaTransformer.transformJpaToApi(any()) } returns mockApArea
  }

  @Nested
  inner class TransformAssessmentsTest {
    @Test
    fun `Not started assessment is correctly transformed`() {
      var assessment = assessmentFactory.produce()

      assessment.data = null

      var result = taskTransformer.transformAssessmentToTask(assessment, "First Last")

      assertThat(result.id).isEqualTo(assessment.id)
      assertThat(result.status).isEqualTo(TaskStatus.notStarted)
      assertThat(result.taskType).isEqualTo(TaskType.assessment)
      assertThat(result.applicationId).isEqualTo(application.id)
      assertThat(result.dueDate).isEqualTo(LocalDate.now().plusDays(2))
      assertThat(result.personName).isEqualTo("First Last")
      assertThat(result.crn).isEqualTo(assessment.application.crn)
      assertThat(result.allocatedToStaffMember).isEqualTo(mockUser)
    }

    @Test
    fun `In Progress assessment is correctly transformed`() {
      var assessment = assessmentFactory
        .withDecision(null)
        .withData("{\"test\": \"data\"}")
        .produce()

      var result = taskTransformer.transformAssessmentToTask(assessment, "First Last")

      assertThat(result.status).isEqualTo(TaskStatus.inProgress)
    }

    @Test
    fun `Complete assessment is correctly transformed`() {
      var assessment = assessmentFactory
        .withDecision(AssessmentDecision.ACCEPTED)
        .withData("{\"test\": \"data\"}")
        .produce()

      var result = taskTransformer.transformAssessmentToTask(assessment, "First Last")

      assertThat(result.status).isEqualTo(TaskStatus.complete)
    }

    @Test
    fun `assessment with ApArea is correctly transformed`() {
      val apArea = ApAreaEntityFactory().produce()
      val application = applicationFactory.withApArea(apArea).produce()
      val assessment = assessmentFactory
        .withApplication(application)
        .produce()

      val result = taskTransformer.transformAssessmentToTask(assessment, "First Last")

      assertThat(result.apArea).isEqualTo(mockApArea)

      verify {
        mockApAreaTransformer.transformJpaToApi(apArea)
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `assessment with createdFromAppeal is transformed correctly`(createdFromAppeal: Boolean) {
      val assessment = assessmentFactory.withCreatedFromAppeal(createdFromAppeal).produce()

      val result = taskTransformer.transformAssessmentToTask(assessment, "First Last")

      assertThat(result.createdFromAppeal).isEqualTo(createdFromAppeal)
    }

    @Test
    fun `Temporary Accommodation assessment returns false for createdFromAppeal`() {
      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .withCreatedAt(OffsetDateTime.parse("2022-12-07T10:40:00Z"))
        .produce()

      val result = taskTransformer.transformAssessmentToTask(assessment, "First Last")

      assertThat(result.createdFromAppeal).isEqualTo(false)
    }
  }

  @Nested
  inner class TransformPlacementApplicationToTaskTest {
    private val placementApplication = placementApplicationFactory
      .withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
      .withData(null)
      .produce()
    val application = placementApplication.application
    private val mockTier = mockk<RiskTierEnvelope>()
    private val releaseType = ReleaseTypeOption.licence

    @BeforeEach
    fun setup() {
      every { mockRisksTransformer.transformTierDomainToApi(application.riskRatings!!.tier) } returns mockTier
      every { mockPlacementRequestTransformer.getReleaseType(application.releaseType) } returns releaseType
    }

    @Test
    fun `Placement application is correctly transformed`() {
      placementApplication.placementDates = mutableListOf(
        PlacementDateEntity(
          id = UUID.randomUUID(),
          createdAt = OffsetDateTime.now(),
          expectedArrival = LocalDate.now(),
          duration = 12,
          placementApplication = placementApplication,
        ),
      )
      val result = taskTransformer.transformPlacementApplicationToTask(placementApplication, "First Last")

      assertThat(result.status).isEqualTo(TaskStatus.notStarted)
      assertThat(result.id).isEqualTo(placementApplication.id)
      assertThat(result.tier).isEqualTo(mockTier)
      assertThat(result.releaseType).isEqualTo(releaseType)
      assertThat(result.personName).isEqualTo("First Last")
      assertThat(result.crn).isEqualTo(placementApplication.application.crn)
      assertThat(result.placementDates).isEqualTo(
        mutableListOf(
          PlacementDates(
            placementApplication.placementDates[0].expectedArrival,
            placementApplication.placementDates[0].duration,
          ),
        ),
      )
    }

    @ParameterizedTest
    @EnumSource(value = JpaPlacementType::class)
    fun `Placement types are transformed correctly`(placementType: JpaPlacementType) {
      val placementApplication = placementApplicationFactory
        .withPlacementType(placementType)
        .produce()

      val result = taskTransformer.transformPlacementApplicationToTask(placementApplication, "First Last")

      if (placementType === JpaPlacementType.ROTL) {
        assertThat(result.placementType).isEqualTo(ApiPlacementType.rotl)
      } else if (placementType === JpaPlacementType.ADDITIONAL_PLACEMENT) {
        assertThat(result.placementType).isEqualTo(ApiPlacementType.additionalPlacement)
      } else if (placementType === JpaPlacementType.RELEASE_FOLLOWING_DECISION) {
        assertThat(result.placementType).isEqualTo(ApiPlacementType.releaseFollowingDecision)
      }
    }

    @Test
    fun `In-progress placement application is correctly transformed`() {
      val placementApplication = placementApplicationFactory
        .withData("{}")
        .withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
        .produce()

      val result = taskTransformer.transformPlacementApplicationToTask(placementApplication, "First Last")

      assertThat(result.status).isEqualTo(TaskStatus.inProgress)
    }

    @Test
    fun `Completed placement application is correctly transformed`() {
      val placementApplication = placementApplicationFactory
        .withData("{}")
        .withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .produce()

      val result = taskTransformer.transformPlacementApplicationToTask(placementApplication, "First Last")

      assertThat(result.status).isEqualTo(TaskStatus.complete)
    }

    @Test
    fun `placement application with ApArea is correctly transformed`() {
      val apArea = ApAreaEntityFactory().produce()
      val application = applicationFactory.withApArea(apArea).produce()
      val placementApplication = placementApplicationFactory
        .withApplication(application)
        .produce()

      val result = taskTransformer.transformPlacementApplicationToTask(placementApplication, "First Last")

      assertThat(result.apArea).isEqualTo(mockApArea)

      verify {
        mockApAreaTransformer.transformJpaToApi(apArea)
      }
    }
  }

  @Nested
  inner class TransformPlacementRequestToTaskTest {

    val placementRequest = placementRequestFactory.produce()
    val application = placementRequest.application
    private val mockTier = mockk<RiskTierEnvelope>()
    private val releaseType = ReleaseTypeOption.licence
    private val placementRequestStatus = PlacementRequestStatus.notMatched

    @BeforeEach
    fun setup() {
      every { mockRisksTransformer.transformTierDomainToApi(application.riskRatings!!.tier) } returns mockTier
      every { mockPlacementRequestTransformer.getReleaseType(application.releaseType) } returns releaseType
      every { mockPlacementRequestTransformer.getStatus(placementRequest) } returns placementRequestStatus
    }

    @Test
    fun `Placement request is correctly transformed`() {
      val result = taskTransformer.transformPlacementRequestToTask(placementRequest, "First Last")

      assertThat(result.status).isEqualTo(TaskStatus.notStarted)
      assertThat(result.id).isEqualTo(placementRequest.id)
      assertThat(result.tier).isEqualTo(mockTier)
      assertThat(result.personName).isEqualTo("First Last")
      assertThat(result.crn).isEqualTo(placementRequest.application.crn)
      assertThat(result.releaseType).isEqualTo(releaseType)
      assertThat(result.expectedArrival).isEqualTo(placementRequest.expectedArrival)
      assertThat(result.duration).isEqualTo(placementRequest.duration)
      assertThat(result.placementRequestStatus).isEqualTo(placementRequestStatus)
    }

    @Test
    fun `Complete placement request is correctly transformed`() {
      val placementRequest = placementRequestFactory
        .withBooking(
          BookingEntityFactory()
            .withPremises(
              ApprovedPremisesEntityFactory()
                .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
                .withYieldedProbationRegion {
                  ProbationRegionEntityFactory().withYieldedApArea { ApAreaEntityFactory().produce() }.produce()
                }
                .produce(),
            )
            .produce(),
        )
        .produce()

      every { mockPlacementRequestTransformer.getStatus(placementRequest) } returns placementRequestStatus

      val result = taskTransformer.transformPlacementRequestToTask(placementRequest, "First Last")

      assertThat(result.status).isEqualTo(TaskStatus.complete)
    }

    @Test
    fun `placement request with ApArea is correctly transformed`() {
      val apArea = ApAreaEntityFactory().produce()
      val application = applicationFactory.withApArea(apArea).produce()
      val placementRequest = placementRequestFactory
        .withApplication(application)
        .produce()

      every { mockPlacementRequestTransformer.getStatus(placementRequest) } returns placementRequestStatus

      val result = taskTransformer.transformPlacementRequestToTask(placementRequest, "First Last")

      assertThat(result.apArea).isEqualTo(mockApArea)

      verify {
        mockApAreaTransformer.transformJpaToApi(apArea)
      }
    }
  }
}
