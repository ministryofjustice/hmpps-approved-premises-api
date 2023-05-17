package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.of
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentClarificationNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import java.util.stream.Stream
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as JpaAssessmentDecision

class AssessmentTransformerTest {
  private val mockApplicationsTransformer = mockk<ApplicationsTransformer>()
  private val mockAssessmentClarificationNoteTransformer = mockk<AssessmentClarificationNoteTransformer>()
  private val mockUserTransformer = mockk<UserTransformer>()
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val risksTransformer = RisksTransformer()
  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  companion object {
    @JvmStatic
    fun assessmentDecisionPairs(): Stream<Arguments> = Stream.of(
      of("ACCEPTED", ApiAssessmentDecision.accepted),
      of("REJECTED", ApiAssessmentDecision.rejected),
      of(null, null)
    )
  }

  private val assessmentTransformer = AssessmentTransformer(
    objectMapper,
    mockApplicationsTransformer,
    mockAssessmentClarificationNoteTransformer,
    mockUserTransformer,
    mockPersonTransformer,
    risksTransformer,
  )

  private val allocatedToUser = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private val assessmentFactory = AssessmentEntityFactory()
    .withApplication(mockk<ApprovedPremisesApplicationEntity>())
    .withId(UUID.fromString("7d0d3b38-5bc3-45c7-95eb-4d714cbd0db1"))
    .withAssessmentSchema(
      ApprovedPremisesAssessmentJsonSchemaEntity(
        id = UUID.fromString("aeeb6992-6485-4600-9c35-19479819c544"),
        addedAt = OffsetDateTime.now(),
        schema = "{}"
      )
    )
    .withDecision(JpaAssessmentDecision.REJECTED)
    .withRejectionRationale("reasoning")
    .withData("{\"data\": \"something\"}")
    .withCreatedAt(OffsetDateTime.parse("2022-12-14T12:05:00Z"))
    .withSubmittedAt(OffsetDateTime.parse("2022-12-14T12:06:00Z"))
    .withAllocatedToUser(allocatedToUser)

  private val user = mockk<ApprovedPremisesUser>()

  @BeforeEach
  fun setup() {
    every { mockApplicationsTransformer.transformJpaToApi(any<ApplicationEntity>(), any(), any()) } returns mockk<ApprovedPremisesApplication>()
    every { mockAssessmentClarificationNoteTransformer.transformJpaToApi(any()) } returns mockk()
    every { mockUserTransformer.transformJpaToApi(any(), any()) } returns user
  }

  @Test
  fun `transformJpaToApi transforms correctly`() {
    val assessment = assessmentFactory.produce()

    val result = assessmentTransformer.transformJpaToApi(assessment, mockk(), mockk()) as ApprovedPremisesAssessment

    assertThat(result.id).isEqualTo(UUID.fromString("7d0d3b38-5bc3-45c7-95eb-4d714cbd0db1"))
    assertThat(result.schemaVersion).isEqualTo(UUID.fromString("aeeb6992-6485-4600-9c35-19479819c544"))
    assertThat(result.decision).isEqualTo(ApiAssessmentDecision.rejected)
    assertThat(result.rejectionRationale).isEqualTo("reasoning")
    assertThat(result.createdAt).isEqualTo(Instant.parse("2022-12-14T12:05:00Z"))
    assertThat(result.submittedAt).isEqualTo(Instant.parse("2022-12-14T12:06:00Z"))
    assertThat(result.allocatedToStaffMember).isEqualTo(user)

    verify { mockUserTransformer.transformJpaToApi(allocatedToUser, ServiceName.approvedPremises) }
  }

  @Test
  fun `transformJpaToApi sets a pending status when there is a clarification note with no response`() {
    val assessment = assessmentFactory.withDecision(null).produce()

    val clarificationNotes = mutableListOf(
      AssessmentClarificationNoteEntityFactory()
        .withAssessment(assessment)
        .withResponse("Some text")
        .withCreatedBy(
          UserEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .produce()
        )
        .produce(),
      AssessmentClarificationNoteEntityFactory()
        .withAssessment(assessment)
        .withCreatedBy(
          UserEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .produce()
        )
        .produce()
    )

    assessment.clarificationNotes = clarificationNotes

    val result = assessmentTransformer.transformJpaToApi(assessment, mockk(), mockk())

    assertThat(result.status).isEqualTo(AssessmentStatus.awaitingResponse)
  }

  @Test
  fun `transformJpaToApi sets a completed status when there is a decision`() {
    val assessment = assessmentFactory
      .withDecision(JpaAssessmentDecision.ACCEPTED)
      .produce()

    val result = assessmentTransformer.transformJpaToApi(assessment, mockk(), mockk())

    assertThat(result.status).isEqualTo(AssessmentStatus.completed)
  }

  @Test
  fun `transformJpaToApi sets a deallocated status when there is a deallocated timestamp`() {
    val assessment = assessmentFactory
      .withDecision(null)
      .withReallocatedAt(OffsetDateTime.now())
      .produce()

    val result = assessmentTransformer.transformJpaToApi(assessment, mockk(), mockk())

    assertThat(result.status).isEqualTo(AssessmentStatus.reallocated)
  }

  @Test
  fun `transformJpaToApi sets an inProgress status when there is no decision and the assessment has data`() {
    val assessment = assessmentFactory
      .withData("{\"data\": \"something\"}")
      .withDecision(null)
      .produce()

    val result = assessmentTransformer.transformJpaToApi(assessment, mockk(), mockk())

    assertThat(result.status).isEqualTo(AssessmentStatus.inProgress)
  }

  @Test
  fun `transformJpaToApi sets an notStarted status when there is no decision and the assessment has no data`() {
    val assessment = assessmentFactory
      .withData(null)
      .withDecision(null)
      .produce()

    val result = assessmentTransformer.transformJpaToApi(assessment, mockk(), mockk())

    assertThat(result.status).isEqualTo(AssessmentStatus.notStarted)
  }

  @ParameterizedTest
  @MethodSource(value = ["assessmentDecisionPairs"])
  fun `transform domain to api summary - temporary application`(domainDecision: String?, apiDecision: ApiAssessmentDecision?) {
    val domainSummary = DomainAssessmentSummary(
      type = "temporary-accommodation",
      id = UUID.randomUUID(),
      applicationId = UUID.randomUUID(),
      createdAt = OffsetDateTime.now(),
      riskRatings = null,
      arrivalDate = null,
      dateOfInfoRequest = null,
      completed = false,
      decision = domainDecision,
      crn = randomStringMultiCaseWithNumbers(6),
      isStarted = true
    )

    every { mockPersonTransformer.transformModelToApi(any(), any()) } returns mockk<Person>()
    val apiSummary = assessmentTransformer.transformDomainToApiSummary(domainSummary, mockk(), mockk())

    assertThat(apiSummary.type).isEqualTo(AssessmentSummary.Type.cAS3)
    assertThat(apiSummary.id).isEqualTo(domainSummary.id)
    assertThat(apiSummary.applicationId).isEqualTo(domainSummary.applicationId)
    assertThat(apiSummary.createdAt).isEqualTo(domainSummary.createdAt.toInstant())
    assertThat(apiSummary.status).isEqualTo(AssessmentStatus.inProgress)
    assertThat(apiSummary.decision).isEqualTo(apiDecision)
    assertThat(apiSummary.risks).isNull()
    assertThat(apiSummary.person).isNotNull
  }

  @Test
  fun `transform domain to api summary - approved premises`() {
    val personRisks = PersonRisksFactory().produce()
    val domainSummary = DomainAssessmentSummary(
      type = "approved-premises",
      id = UUID.randomUUID(),
      applicationId = UUID.randomUUID(),
      createdAt = OffsetDateTime.now(),
      riskRatings = objectMapper.writeValueAsString(personRisks),
      arrivalDate = OffsetDateTime.now().randomDateTimeBefore(),
      dateOfInfoRequest = OffsetDateTime.now().randomDateTimeBefore(),
      completed = false,
      decision = "ACCEPTED",
      crn = randomStringMultiCaseWithNumbers(6),
      isStarted = true
    )

    every { mockPersonTransformer.transformModelToApi(any(), any()) } returns mockk<Person>()
    val apiSummary = assessmentTransformer.transformDomainToApiSummary(domainSummary, mockk(), mockk())

    assertThat(apiSummary.type).isEqualTo(AssessmentSummary.Type.cAS1)
    assertThat(apiSummary.id).isEqualTo(domainSummary.id)
    assertThat(apiSummary.applicationId).isEqualTo(domainSummary.applicationId)
    assertThat(apiSummary.createdAt).isEqualTo(domainSummary.createdAt.toInstant())
    assertThat(apiSummary.arrivalDate).isEqualTo(domainSummary.arrivalDate?.toInstant())
    assertThat(apiSummary.status).isEqualTo(AssessmentStatus.awaitingResponse)
    assertThat(apiSummary.risks).isEqualTo(risksTransformer.transformDomainToApi(personRisks, domainSummary.crn))
    assertThat(apiSummary.person).isNotNull
  }
}
