package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementDateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementApplicationTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class PlacementApplicationTransformerTest {
  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val placementApplicationTransformer = PlacementApplicationTransformer(objectMapper)

  private var user = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private var applicationMock = mockk<ApprovedPremisesApplicationEntity>()

  private var application = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()

  private var assessment = ApprovedPremisesAssessmentEntityFactory()
    .withAllocatedToUser(user)
    .withApplication(application)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()

  @BeforeEach
  fun setup() {
    every { applicationMock.getLatestAssessment() } returns assessment
    every { applicationMock.id } returns application.id
    every { applicationMock.submittedAt } returns application.submittedAt
  }

  @Test
  fun `transformJpaToApi converts correctly when there is no data or document`() {
    val placementApplication = PlacementApplicationEntityFactory()
      .withCreatedByUser(user)
      .withApplication(applicationMock)
      .withData(null)
      .withDocument(null)
      .produce()

    placementApplication.placementDates.add(
      PlacementDateEntityFactory()
        .withExpectedArrival(LocalDate.of(2023, 12, 11))
        .withDuration(30)
        .withPlacementApplication(placementApplication)
        .produce(),
    )

    placementApplication.placementDates.add(
      PlacementDateEntityFactory()
        .withExpectedArrival(LocalDate.of(2024, 1, 31))
        .withDuration(15)
        .withPlacementApplication(placementApplication)
        .produce(),
    )

    val result = placementApplicationTransformer.transformJpaToApi(placementApplication)

    assertThat(result.id).isEqualTo(placementApplication.id)
    assertThat(result.applicationId).isEqualTo(application.id)
    assertThat(result.applicationCompletedAt).isEqualTo(application.submittedAt!!.toInstant())
    assertThat(result.assessmentId).isEqualTo(assessment.id)
    assertThat(result.assessmentCompletedAt).isEqualTo(assessment.submittedAt!!.toInstant())
    assertThat(result.createdByUserId).isEqualTo(placementApplication.createdByUser.id)
    assertThat(result.schemaVersion).isEqualTo(placementApplication.schemaVersion.id)
    assertThat(result.createdAt).isEqualTo(placementApplication.createdAt.toInstant())
    assertThat(result.data).isNull()
    assertThat(result.document).isNull()
    assertThat(result.outdatedSchema).isTrue
    assertThat(result.submittedAt).isNull()
    assertThat(result.canBeWithdrawn).isFalse
    assertThat(result.isWithdrawn).isFalse
    assertThat(result.withdrawalReason).isNull()
    assertThat(result.type).isEqualTo(PlacementApplicationType.additional)

    assertThat(result.placementDates).hasSize(2)
    assertThat(result.placementDates[0].expectedArrival).isEqualTo(LocalDate.of(2023, 12, 11))
    assertThat(result.placementDates[0].duration).isEqualTo(30)
    assertThat(result.placementDates[1].expectedArrival).isEqualTo(LocalDate.of(2024, 1, 31))
    assertThat(result.placementDates[1].duration).isEqualTo(15)
  }

  @Test
  fun `transformJpaToApi converts correctly when there is data and a document`() {
    val data = "{\"data\": \"something\"}"
    val document = "{\"document\": \"something\"}"
    val placementApplication = PlacementApplicationEntityFactory()
      .withCreatedByUser(user)
      .withApplication(applicationMock)
      .withData(data)
      .withDocument(document)
      .produce()

    val result = placementApplicationTransformer.transformJpaToApi(placementApplication)

    assertThat(result.id).isEqualTo(placementApplication.id)
    assertThat(result.data).isEqualTo(objectMapper.readTree(data))
    assertThat(result.document).isEqualTo(objectMapper.readTree(document))
  }

  @Test
  fun `transformJpaToApi returns canBeWithdrawn false if already withdrawn`() {
    val data = "{\"data\": \"something\"}"
    val document = "{\"document\": \"something\"}"
    val placementApplication = PlacementApplicationEntityFactory()
      .withCreatedByUser(user)
      .withApplication(applicationMock)
      .withData(data)
      .withDocument(document)
      .withDecision(PlacementApplicationDecision.WITHDRAW)
      .produce()

    val result = placementApplicationTransformer.transformJpaToApi(placementApplication)

    assertThat(result.id).isEqualTo(placementApplication.id)
    assertThat(result.canBeWithdrawn).isEqualTo(false)
  }

  @Test
  fun `transformToWithdrawable converts correctly`() {
    val id = UUID.randomUUID()

    val jpa = PlacementApplicationEntityFactory()
      .withId(id)
      .withCreatedByUser(user)
      .withApplication(applicationMock)
      .withDecision(PlacementApplicationDecision.ACCEPTED)
      .produce()

    jpa.placementDates.add(
      PlacementDateEntityFactory()
        .withExpectedArrival(LocalDate.of(2023, 12, 11))
        .withDuration(30)
        .withPlacementApplication(jpa)
        .produce(),
    )

    jpa.placementDates.add(
      PlacementDateEntityFactory()
        .withExpectedArrival(LocalDate.of(2024, 1, 31))
        .withDuration(15)
        .withPlacementApplication(jpa)
        .produce(),
    )

    val result = placementApplicationTransformer.transformToWithdrawable(jpa)

    assertThat(result).isEqualTo(
      Withdrawable(
        id,
        WithdrawableType.PLACEMENT_APPLICATION,
        listOf(
          DatePeriod(
            LocalDate.of(2023, 12, 11),
            LocalDate.of(2024, 1, 10),
          ),
          DatePeriod(
            LocalDate.of(2024, 1, 31),
            LocalDate.of(2024, 2, 15),
          ),
        ),
      ),
    )
  }
}
