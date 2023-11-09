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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementApplicationTransformer
import java.time.OffsetDateTime

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
  fun `transformJpaToApi returns canBeWithdrawn when associated bookings are present`() {
    val data = "{\"data\": \"something\"}"
    val document = "{\"document\": \"something\"}"
    val placementApplication = PlacementApplicationEntityFactory()
      .withCreatedByUser(user)
      .withApplication(applicationMock)
      .withData(data)
      .withDocument(document)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .produce(),
      )
      .withApplication(application)
      .withPlacementApplication(placementApplication)
      .withAssessment(assessment)
      .withAllocatedToUser(user)
      .produce()

    val premisesEntity = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    placementRequest.booking = BookingEntityFactory()
      .withYieldedPremises { premisesEntity }
      .produce()

    placementApplication.placementRequests = mutableListOf(
      placementRequest,
    )

    val result = placementApplicationTransformer.transformJpaToApi(placementApplication)

    assertThat(result.id).isEqualTo(placementApplication.id)
    assertThat(result.canBeWithdrawn).isEqualTo(false)
  }
}
