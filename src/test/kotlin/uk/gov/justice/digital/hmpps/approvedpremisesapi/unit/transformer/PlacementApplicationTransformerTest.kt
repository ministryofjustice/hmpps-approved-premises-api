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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementDateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesPlacementApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class PlacementApplicationTransformerTest {
  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val jsonSchemaService = mockk<JsonSchemaService>()
  private val placementRequestTransformer = mockk<PlacementRequestTransformer>()

  private val placementApplicationTransformer = PlacementApplicationTransformer(
    objectMapper,
    jsonSchemaService,
    placementRequestTransformer,
  )

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
    assertThat(result.outdatedSchema).isEqualTo(true)
    assertThat(result.submittedAt).isNull()
    assertThat(result.canBeWithdrawn).isEqualTo(true)
    assertThat(result.isWithdrawn).isEqualTo(false)
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
        WithdrawableType.placementApplication,
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

  @ParameterizedTest
  @EnumSource(PlacementApplicationDecision::class)
  @NullSource
  fun `transformJpaToApi returns isWithdrawn based upon decision`(decision: PlacementApplicationDecision?) {
    val data = "{\"data\": \"something\"}"
    val document = "{\"document\": \"something\"}"
    val placementApplication = PlacementApplicationEntityFactory()
      .withCreatedByUser(user)
      .withApplication(applicationMock)
      .withData(data)
      .withDocument(document)
      .withDecision(decision)
      .withWithdrawalReason(PlacementApplicationWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST)
      .produce()

    val result = placementApplicationTransformer.transformJpaToApi(placementApplication)

    when (decision) {
      PlacementApplicationDecision.ACCEPTED -> assertThat(result.isWithdrawn).isEqualTo(false)
      PlacementApplicationDecision.REJECTED -> assertThat(result.isWithdrawn).isEqualTo(false)
      PlacementApplicationDecision.WITHDRAW -> assertThat(result.isWithdrawn).isEqualTo(true)
      PlacementApplicationDecision.WITHDRAWN_BY_PP -> assertThat(result.isWithdrawn).isEqualTo(true)
      null -> assertThat(result.isWithdrawn).isEqualTo(false)
    }

    assertThat(result.withdrawalReason).isEqualTo(WithdrawPlacementRequestReason.errorInPlacementRequest)
  }

  @Test
  fun `transformPlacementRequestJpaToApi converts correctly`() {
    val placementRequest = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .produce(),
      )
      .withExpectedArrival(LocalDate.of(2023, 12, 11))
      .withDuration(30)
      .produce()

    val schemaId = UUID.randomUUID()
    every {
      jsonSchemaService.getNewestSchema<ApprovedPremisesPlacementApplicationJsonSchemaEntity>(any())
    } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().withId(schemaId).produce()

    every { placementRequestTransformer.getWithdrawalReason(null) } returns null

    val result = placementApplicationTransformer.transformPlacementRequestJpaToApi(placementRequest)

    assertThat(result.id).isEqualTo(placementRequest.id)
    assertThat(result.applicationId).isEqualTo(application.id)
    assertThat(result.applicationCompletedAt).isEqualTo(application.submittedAt!!.toInstant())
    assertThat(result.assessmentId).isEqualTo(assessment.id)
    assertThat(result.assessmentCompletedAt).isEqualTo(assessment.submittedAt!!.toInstant())
    assertThat(result.createdByUserId).isEqualTo(application.createdByUser.id)
    assertThat(result.schemaVersion).isEqualTo(schemaId)
    assertThat(result.createdAt).isEqualTo(placementRequest.createdAt.toInstant())
    assertThat(result.data).isEqualTo("{}")
    assertThat(result.document).isEqualTo("{}")
    assertThat(result.outdatedSchema).isEqualTo(false)
    assertThat(result.submittedAt).isEqualTo(application.submittedAt?.toInstant())
    assertThat(result.canBeWithdrawn).isEqualTo(true)
    assertThat(result.isWithdrawn).isEqualTo(false)
    assertThat(result.withdrawalReason).isNull()
    assertThat(result.type).isEqualTo(PlacementApplicationType.initial)

    assertThat(result.placementDates).hasSize(1)
    assertThat(result.placementDates[0].expectedArrival).isEqualTo(LocalDate.of(2023, 12, 11))
    assertThat(result.placementDates[0].duration).isEqualTo(30)
  }

  @Test
  fun `transformPlacementRequestJpaToApi converts correctly for withdrawn request`() {
    val placementRequest = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .produce(),
      )
      .withIsWithdrawn(true)
      .withWithdrawalReason(PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
      .produce()

    val schemaId = UUID.randomUUID()
    every {
      jsonSchemaService.getNewestSchema<ApprovedPremisesPlacementApplicationJsonSchemaEntity>(any())
    } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().withId(schemaId).produce()

    every {
      placementRequestTransformer.getWithdrawalReason(PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
    } returns WithdrawPlacementRequestReason.duplicatePlacementRequest

    val result = placementApplicationTransformer.transformPlacementRequestJpaToApi(placementRequest)

    assertThat(result.id).isEqualTo(placementRequest.id)
    assertThat(result.canBeWithdrawn).isEqualTo(false)
    assertThat(result.isWithdrawn).isEqualTo(true)
    assertThat(result.withdrawalReason).isEqualTo(WithdrawPlacementRequestReason.duplicatePlacementRequest)
    assertThat(result.type).isEqualTo(PlacementApplicationType.initial)
  }
}
