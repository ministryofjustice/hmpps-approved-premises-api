package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentClarificationNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.AssessmentTransformerTest.DomainAssessmentSummaryImpl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as JpaAssessmentDecision

@ExtendWith(MockKExtension::class)
class Cas1AssessmentTransformerTest {
  @MockK
  lateinit var mockApplicationsTransformer: ApplicationsTransformer

  @MockK
  lateinit var mockCas1AssessmentClarificationNoteTransformer: Cas1AssessmentClarificationNoteTransformer

  @MockK
  lateinit var mockUserTransformer: UserTransformer

  @MockK
  lateinit var mockPersonTransformer: PersonTransformer

  @MockK
  lateinit var userService: UserService

  @MockK
  lateinit var approvedPremisesUser: ApprovedPremisesUser

  private val risksTransformer = RisksTransformer()
  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  @InjectMockKs
  lateinit var cas1AssessmentTransformer: Cas1AssessmentTransformer

  private val allocatedToUser = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private val approvedPremisesAssessmentFactory = ApprovedPremisesAssessmentEntityFactory()
    .withApplication(mockk<ApprovedPremisesApplicationEntity>())
    .withId(UUID.fromString("7d0d3b38-5bc3-45c7-95eb-4d714cbd0db1"))
    .withDecision(JpaAssessmentDecision.REJECTED)
    .withRejectionRationale("reasoning")
    .withData("{\"data\": \"something\"}")
    .withCreatedAt(OffsetDateTime.parse("2022-12-14T12:05:00Z"))
    .withSubmittedAt(OffsetDateTime.parse("2022-12-14T12:06:00Z"))
    .withAllocatedToUser(allocatedToUser)

  @BeforeEach
  fun setup() {
    every { mockApplicationsTransformer.transformJpaToApi(any<ApplicationEntity>(), any()) } answers {
      when (it.invocation.args[0]) {
        is ApprovedPremisesApplicationEntity -> mockk<ApprovedPremisesApplication>()
        is TemporaryAccommodationApplicationEntity -> mockk<TemporaryAccommodationApplication>()
        else -> fail("Unknown application entity type")
      }
    }
    every { mockCas1AssessmentClarificationNoteTransformer.transformJpaToCas1ClarificationNote(any()) } returns mockk()
    every { mockUserTransformer.transformJpaToApi(any(), ServiceName.approvedPremises) } returns approvedPremisesUser
    every { mockUserTransformer.transformCas1JpaToApi(any()) } returns approvedPremisesUser
    every { mockApplicationsTransformer.transformJpaToCas1Application(any(), any()) } returns mockk<Cas1Application>()
  }

  @Nested
  inner class TransformJpaToCas1Assessment {

    @Test
    fun `transformJpaToCas1Assessment transforms correctly`() {
      val assessment = approvedPremisesAssessmentFactory
        .withData("{ \"some\": \"data\" }")
        .withDocument("{ \"some\": \"doc\" }")
        .produce()

      val result = cas1AssessmentTransformer.transformJpaToCas1Assessment(assessment, mockk())

      assertThat(result.id).isEqualTo(UUID.fromString("7d0d3b38-5bc3-45c7-95eb-4d714cbd0db1"))
      assertThat(result.decision).isEqualTo(ApiAssessmentDecision.rejected)
      assertThat(result.rejectionRationale).isEqualTo("reasoning")
      assertThat(result.createdAt).isEqualTo(Instant.parse("2022-12-14T12:05:00Z"))
      assertThat(result.submittedAt).isEqualTo(Instant.parse("2022-12-14T12:06:00Z"))
      assertThat(result.allocatedToStaffMember).isEqualTo(approvedPremisesUser)
      assertThat(result.createdFromAppeal).isEqualTo(assessment.createdFromAppeal)
      assertThat(result.data.toString()).isEqualTo("{\"some\":\"data\"}")
      assertThat(result.document.toString()).isEqualTo("{\"some\":\"doc\"}")

      verify { mockUserTransformer.transformCas1JpaToApi(allocatedToUser) }
    }

    @Test
    fun `transformJpaToCas1Assessment for CAS1 handles minimum null values`() {
      val assessment = approvedPremisesAssessmentFactory
        .withAllocatedAt(null)
        .withAllocatedToUser(null)
        .produce()

      val result = cas1AssessmentTransformer.transformJpaToCas1Assessment(assessment, mockk())

      assertThat(result.id).isEqualTo(UUID.fromString("7d0d3b38-5bc3-45c7-95eb-4d714cbd0db1"))
      assertThat(result.allocatedAt).isNull()
      assertThat(result.allocatedToStaffMember).isNull()
    }

    @Test
    fun `transformJpaToCas1Assessment for Approved Premises sets a pending status when there is a clarification note with no response`() {
      val assessment = approvedPremisesAssessmentFactory.withDecision(null).produce()

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
              .produce(),
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
              .produce(),
          )
          .produce(),
      )

      assessment.clarificationNotes = clarificationNotes

      val result = cas1AssessmentTransformer.transformJpaToCas1Assessment(assessment, mockk())

      assertThat(result).isInstanceOf(Cas1Assessment::class.java)
      assertThat(result.status).isEqualTo(Cas1AssessmentStatus.awaitingResponse)
    }

    @Test
    fun `transformJpaToCas1Assessment for Approved Premises sets a completed status when there is a decision`() {
      val assessment = approvedPremisesAssessmentFactory
        .withDecision(JpaAssessmentDecision.ACCEPTED)
        .produce()

      val result = cas1AssessmentTransformer.transformJpaToCas1Assessment(assessment, mockk())

      assertThat(result).isInstanceOf(Cas1Assessment::class.java)
      assertThat(result.status).isEqualTo(Cas1AssessmentStatus.completed)
    }

    @Test
    fun `transformJpaToCas1Assessment for Approved Premises sets a deallocated status when there is a deallocated timestamp`() {
      val assessment = approvedPremisesAssessmentFactory
        .withDecision(null)
        .withReallocatedAt(OffsetDateTime.now())
        .produce()

      val result = cas1AssessmentTransformer.transformJpaToCas1Assessment(assessment, mockk())

      assertThat(result).isInstanceOf(Cas1Assessment::class.java)
      assertThat(result.status).isEqualTo(Cas1AssessmentStatus.reallocated)
    }

    @Test
    fun `transformJpaToCas1Assessment for Approved Premises sets an inProgress status when there is no decision and the assessment has data`() {
      val assessment = approvedPremisesAssessmentFactory
        .withData("{\"data\": \"something\"}")
        .withDecision(null)
        .produce()

      val result = cas1AssessmentTransformer.transformJpaToCas1Assessment(assessment, mockk())

      assertThat(result).isInstanceOf(Cas1Assessment::class.java)
      assertThat(result.status).isEqualTo(Cas1AssessmentStatus.inProgress)
    }

    @Test
    fun `transformJpaToCas1Assessment for Approved Premises sets a notStarted status when there is no decision and the assessment has no data`() {
      val assessment = approvedPremisesAssessmentFactory
        .withData(null)
        .withDecision(null)
        .produce()

      val result = cas1AssessmentTransformer.transformJpaToCas1Assessment(assessment, mockk())

      assertThat(result).isInstanceOf(Cas1Assessment::class.java)
      assertThat(result.status).isEqualTo(Cas1AssessmentStatus.notStarted)
    }
  }

  @Nested
  inner class TransformDomainToCas1AssessmentSummary {

    @Test
    fun `transform domain to Cas1AssessmentSummary  - approved premises`() {
      val personRisks = PersonRisksFactory().produce()
      val domainSummary = DomainAssessmentSummaryImpl(
        type = "approved-premises",
        id = UUID.randomUUID(),
        applicationId = UUID.randomUUID(),
        createdAt = Instant.now(),
        riskRatings = objectMapper.writeValueAsString(personRisks),
        arrivalDate = OffsetDateTime.now().randomDateTimeBefore(14).toInstant(),
        completed = false,
        decision = "ACCEPTED",
        crn = randomStringMultiCaseWithNumbers(6),
        allocated = true,
        status = DomainAssessmentSummaryStatus.AWAITING_RESPONSE,
        dueAt = Instant.now(),
        probationDeliveryUnitName = null,
      )

      every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
      val apiSummary = cas1AssessmentTransformer.transformDomainToCas1AssessmentSummary(domainSummary, mockk())

      assertThat(apiSummary).isInstanceOf(Cas1AssessmentSummary::class.java)
      assertThat(apiSummary.id).isEqualTo(domainSummary.id)
      assertThat(apiSummary.applicationId).isEqualTo(domainSummary.applicationId)
      assertThat(apiSummary.createdAt).isEqualTo(domainSummary.createdAt)
      assertThat(apiSummary.arrivalDate).isEqualTo(domainSummary.arrivalDate)
      assertThat(apiSummary.status).isEqualTo(Cas1AssessmentStatus.awaitingResponse)
      assertThat(apiSummary.risks).isEqualTo(risksTransformer.transformDomainToApi(personRisks, domainSummary.crn))
      assertThat(apiSummary.person).isNotNull
    }
  }
}
