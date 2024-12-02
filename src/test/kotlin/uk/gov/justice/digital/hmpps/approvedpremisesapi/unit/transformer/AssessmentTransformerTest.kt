package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentReferralHistorySystemNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentReferralHistoryUserNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentClarificationNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentReferralHistoryNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as JpaAssessmentDecision

@ExtendWith(MockKExtension::class)
class AssessmentTransformerTest {

  @MockK
  lateinit var mockApplicationsTransformer: ApplicationsTransformer

  @MockK
  lateinit var mockAssessmentClarificationNoteTransformer: AssessmentClarificationNoteTransformer

  @InjectMockKs
  lateinit var assessmentReferralHistoryNoteTransformer: AssessmentReferralHistoryNoteTransformer

  @MockK
  lateinit var mockUserTransformer: UserTransformer

  @MockK
  lateinit var mockPersonTransformer: PersonTransformer

  @MockK
  lateinit var userService: UserService

  private val risksTransformer = RisksTransformer()
  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  @InjectMockKs
  lateinit var assessmentTransformer: AssessmentTransformer

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
    .withAssessmentSchema(
      ApprovedPremisesAssessmentJsonSchemaEntity(
        id = UUID.fromString("aeeb6992-6485-4600-9c35-19479819c544"),
        addedAt = OffsetDateTime.now(),
        schema = "{}",
      ),
    )
    .withDecision(JpaAssessmentDecision.REJECTED)
    .withRejectionRationale("reasoning")
    .withData("{\"data\": \"something\"}")
    .withCreatedAt(OffsetDateTime.parse("2022-12-14T12:05:00Z"))
    .withSubmittedAt(OffsetDateTime.parse("2022-12-14T12:06:00Z"))
    .withAllocatedToUser(allocatedToUser)

  private val temporaryAccommodationAssessmentFactory = TemporaryAccommodationAssessmentEntityFactory()
    .withApplication(mockk<TemporaryAccommodationApplicationEntity>())
    .withId(UUID.fromString("7d0d3b38-5bc3-45c7-95eb-4d714cbd0db1"))
    .withAssessmentSchema(
      TemporaryAccommodationAssessmentJsonSchemaEntity(
        id = UUID.fromString("aeeb6992-6485-4600-9c35-19479819c544"),
        addedAt = OffsetDateTime.now(),
        schema = "{}",
      ),
    )
    .withDecision(JpaAssessmentDecision.REJECTED)
    .withRejectionRationale("reasoning")
    .withData("{\"data\": \"something\"}")
    .withCreatedAt(OffsetDateTime.parse("2022-12-14T12:05:00Z"))
    .withSubmittedAt(OffsetDateTime.parse("2022-12-14T12:06:00Z"))
    .withAllocatedToUser(allocatedToUser)

  private val approvedPremisesUser = mockk<ApprovedPremisesUser>()
  private val temporaryAccommodationUser = mockk<TemporaryAccommodationUser>()

  @BeforeEach
  fun setup() {
    every { mockApplicationsTransformer.transformJpaToApi(any<ApplicationEntity>(), any()) } answers {
      when (it.invocation.args[0]) {
        is ApprovedPremisesApplicationEntity -> mockk<ApprovedPremisesApplication>()
        is TemporaryAccommodationApplicationEntity -> mockk<TemporaryAccommodationApplication>()
        else -> fail("Unknown application entity type")
      }
    }
    every { mockAssessmentClarificationNoteTransformer.transformJpaToApi(any()) } returns mockk()
    every { mockUserTransformer.transformJpaToApi(any(), ServiceName.approvedPremises) } returns approvedPremisesUser
    every { mockUserTransformer.transformJpaToApi(any(), ServiceName.temporaryAccommodation) } returns temporaryAccommodationUser
  }

  @Nested
  inner class TransformJpaToApiCas1 {

    @Test
    fun `transformJpaToApi transforms correctly`() {
      val assessment = approvedPremisesAssessmentFactory.produce()

      val result = assessmentTransformer.transformJpaToApi(assessment, mockk()) as ApprovedPremisesAssessment

      assertThat(result.id).isEqualTo(UUID.fromString("7d0d3b38-5bc3-45c7-95eb-4d714cbd0db1"))
      assertThat(result.schemaVersion).isEqualTo(UUID.fromString("aeeb6992-6485-4600-9c35-19479819c544"))
      assertThat(result.decision).isEqualTo(ApiAssessmentDecision.REJECTED)
      assertThat(result.rejectionRationale).isEqualTo("reasoning")
      assertThat(result.createdAt).isEqualTo(Instant.parse("2022-12-14T12:05:00Z"))
      assertThat(result.submittedAt).isEqualTo(Instant.parse("2022-12-14T12:06:00Z"))
      assertThat(result.allocatedToStaffMember).isEqualTo(approvedPremisesUser)
      assertThat(result.createdFromAppeal).isEqualTo(assessment.createdFromAppeal)

      verify { mockUserTransformer.transformJpaToApi(allocatedToUser, ServiceName.approvedPremises) }
    }

    @Test
    fun `transformJpaToApi for CAS1 handles minimum null values`() {
      val assessment = approvedPremisesAssessmentFactory
        .withAllocatedAt(null)
        .withAllocatedToUser(null)
        .produce()

      val result = assessmentTransformer.transformJpaToApi(assessment, mockk()) as ApprovedPremisesAssessment

      assertThat(result.id).isEqualTo(UUID.fromString("7d0d3b38-5bc3-45c7-95eb-4d714cbd0db1"))
      assertThat(result.allocatedAt).isNull()
      assertThat(result.allocatedToStaffMember).isNull()
    }

    @Test
    fun `transformJpaToApi for Approved Premises sets a pending status when there is a clarification note with no response`() {
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

      val result = assessmentTransformer.transformJpaToApi(assessment, mockk())

      assertThat(result).isInstanceOf(ApprovedPremisesAssessment::class.java)
      result as ApprovedPremisesAssessment
      assertThat(result.status).isEqualTo(ApprovedPremisesAssessmentStatus.AWAITING_RESPONSE)
    }

    @Test
    fun `transformJpaToApi for Approved Premises sets a completed status when there is a decision`() {
      val assessment = approvedPremisesAssessmentFactory
        .withDecision(JpaAssessmentDecision.ACCEPTED)
        .produce()

      val result = assessmentTransformer.transformJpaToApi(assessment, mockk())

      assertThat(result).isInstanceOf(ApprovedPremisesAssessment::class.java)
      result as ApprovedPremisesAssessment
      assertThat(result.status).isEqualTo(ApprovedPremisesAssessmentStatus.COMPLETED)
    }

    @Test
    fun `transformJpaToApi for Approved Premises sets a deallocated status when there is a deallocated timestamp`() {
      val assessment = approvedPremisesAssessmentFactory
        .withDecision(null)
        .withReallocatedAt(OffsetDateTime.now())
        .produce()

      val result = assessmentTransformer.transformJpaToApi(assessment, mockk())

      assertThat(result).isInstanceOf(ApprovedPremisesAssessment::class.java)
      result as ApprovedPremisesAssessment
      assertThat(result.status).isEqualTo(ApprovedPremisesAssessmentStatus.REALLOCATED)
    }

    @Test
    fun `transformJpaToApi for Approved Premises sets an inProgress status when there is no decision and the assessment has data`() {
      val assessment = approvedPremisesAssessmentFactory
        .withData("{\"data\": \"something\"}")
        .withDecision(null)
        .produce()

      val result = assessmentTransformer.transformJpaToApi(assessment, mockk())

      assertThat(result).isInstanceOf(ApprovedPremisesAssessment::class.java)
      result as ApprovedPremisesAssessment
      assertThat(result.status).isEqualTo(ApprovedPremisesAssessmentStatus.IN_PROGRESS)
    }

    @Test
    fun `transformJpaToApi for Approved Premises sets a notStarted status when there is no decision and the assessment has no data`() {
      val assessment = approvedPremisesAssessmentFactory
        .withData(null)
        .withDecision(null)
        .produce()

      val result = assessmentTransformer.transformJpaToApi(assessment, mockk())

      assertThat(result).isInstanceOf(ApprovedPremisesAssessment::class.java)
      result as ApprovedPremisesAssessment
      assertThat(result.status).isEqualTo(ApprovedPremisesAssessmentStatus.NOT_STARTED)
    }
  }

  @Nested
  inner class TransformJpaToApiCas3 {

    @Test
    fun `transformJpaToApi for Temporary Accommodation sets an unallocated status when there is no allocated user`() {
      val assessment = temporaryAccommodationAssessmentFactory
        .withDecision(null)
        .withoutAllocatedToUser()
        .withReleaseDate(LocalDate.now())
        .withAccommodationRequiredFromDate(LocalDate.now())
        .produce()

      val result = assessmentTransformer.transformJpaToApi(assessment, mockk())

      assertThat(result).isInstanceOf(TemporaryAccommodationAssessment::class.java)
      result as TemporaryAccommodationAssessment
      assertThat(result.status).isEqualTo(TemporaryAccommodationAssessmentStatus.unallocated)
    }

    @Test
    fun `transformJpaToApi for Temporary Accommodation sets an inReview status when there is an allocated user`() {
      val assessment = temporaryAccommodationAssessmentFactory
        .withDecision(null)
        .withReleaseDate(LocalDate.now())
        .withAccommodationRequiredFromDate(LocalDate.now())
        .produce()

      val result = assessmentTransformer.transformJpaToApi(assessment, mockk())

      assertThat(result).isInstanceOf(TemporaryAccommodationAssessment::class.java)
      result as TemporaryAccommodationAssessment
      assertThat(result.status).isEqualTo(TemporaryAccommodationAssessmentStatus.inReview)
    }

    @Test
    fun `transformJpaToApi for Temporary Accommodation sets a readyToPlace status when the assessment is approved`() {
      val assessment = temporaryAccommodationAssessmentFactory
        .withDecision(JpaAssessmentDecision.ACCEPTED)
        .withReleaseDate(LocalDate.now())
        .withAccommodationRequiredFromDate(LocalDate.now())
        .produce()

      val result = assessmentTransformer.transformJpaToApi(assessment, mockk())

      assertThat(result).isInstanceOf(TemporaryAccommodationAssessment::class.java)
      result as TemporaryAccommodationAssessment
      assertThat(result.status).isEqualTo(TemporaryAccommodationAssessmentStatus.readyToPlace)
    }

    @Test
    fun `transformJpaToApi for Temporary Accommodation sets a closed status when the assessment is approved and has been completed`() {
      val assessment = temporaryAccommodationAssessmentFactory
        .withDecision(JpaAssessmentDecision.ACCEPTED)
        .withCompletedAt(OffsetDateTime.now())
        .withReleaseDate(LocalDate.now())
        .withAccommodationRequiredFromDate(LocalDate.now())
        .produce()

      val result = assessmentTransformer.transformJpaToApi(assessment, mockk())

      assertThat(result).isInstanceOf(TemporaryAccommodationAssessment::class.java)
      result as TemporaryAccommodationAssessment
      assertThat(result.status).isEqualTo(TemporaryAccommodationAssessmentStatus.closed)
    }

    @Test
    fun `transformJpaToApi for Temporary Accommodation sets a rejected status when the assessment is rejected`() {
      val assessment = temporaryAccommodationAssessmentFactory
        .withDecision(JpaAssessmentDecision.REJECTED)
        .withReleaseDate(LocalDate.now())
        .withAccommodationRequiredFromDate(LocalDate.now())
        .produce()

      val result = assessmentTransformer.transformJpaToApi(assessment, mockk())

      assertThat(result).isInstanceOf(TemporaryAccommodationAssessment::class.java)
      result as TemporaryAccommodationAssessment
      assertThat(result.status).isEqualTo(TemporaryAccommodationAssessmentStatus.rejected)
    }

    @Test
    fun `transformJpaToApi for Temporary Accommodation serializes the summary data blob correctly`() {
      val assessment = temporaryAccommodationAssessmentFactory
        .withSummaryData("{\"num\": 50, \"text\": \"Hello world!\"}")
        .withReleaseDate(LocalDate.now())
        .withAccommodationRequiredFromDate(LocalDate.now())
        .produce()

      val result = assessmentTransformer.transformJpaToApi(assessment, mockk())

      assertThat(result).isInstanceOf(TemporaryAccommodationAssessment::class.java)
      result as TemporaryAccommodationAssessment
      assertThat(result.summaryData).isEqualTo(
        objectMapper.valueToTree(
          object {
            val num = 50
            val text = "Hello world!"
          },
        ),
      )
    }
  }

  @Nested
  inner class TransformDomainToApiCas3 {

    @Test
    fun `transform domain to api summary - temporary application`() {
      val domainSummary = DomainAssessmentSummaryImpl(
        type = "temporary-accommodation",
        id = UUID.randomUUID(),
        applicationId = UUID.randomUUID(),
        createdAt = Instant.now(),
        riskRatings = null,
        arrivalDate = null,
        completed = false,
        decision = null,
        crn = randomStringMultiCaseWithNumbers(6),
        allocated = true,
        status = null,
        dueAt = null,
        probationDeliveryUnitName = "test pdu name",
      )

      every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
      val apiSummary = assessmentTransformer.transformDomainToApiSummary(domainSummary, mockk())

      assertThat(apiSummary).isInstanceOf(TemporaryAccommodationAssessmentSummary::class.java)
      apiSummary as TemporaryAccommodationAssessmentSummary
      assertThat(apiSummary.id).isEqualTo(domainSummary.id)
      assertThat(apiSummary.applicationId).isEqualTo(domainSummary.applicationId)
      assertThat(apiSummary.createdAt).isEqualTo(domainSummary.createdAt)
      assertThat(apiSummary.status).isEqualTo(TemporaryAccommodationAssessmentStatus.inReview)
      assertThat(apiSummary.decision).isNull()
      assertThat(apiSummary.risks).isNull()
      assertThat(apiSummary.person).isNotNull
      assertThat(apiSummary.probationDeliveryUnitName).isEqualTo("test pdu name")
    }
  }

  @Nested
  inner class TransformDomainToApiCas1 {

    @Test
    fun `transform domain to api summary - approved premises`() {
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
      val apiSummary = assessmentTransformer.transformDomainToApiSummary(domainSummary, mockk())

      assertThat(apiSummary).isInstanceOf(ApprovedPremisesAssessmentSummary::class.java)
      apiSummary as ApprovedPremisesAssessmentSummary
      assertThat(apiSummary.id).isEqualTo(domainSummary.id)
      assertThat(apiSummary.applicationId).isEqualTo(domainSummary.applicationId)
      assertThat(apiSummary.createdAt).isEqualTo(domainSummary.createdAt)
      assertThat(apiSummary.arrivalDate).isEqualTo(domainSummary.arrivalDate)
      assertThat(apiSummary.status).isEqualTo(ApprovedPremisesAssessmentStatus.AWAITING_RESPONSE)
      assertThat(apiSummary.risks).isEqualTo(risksTransformer.transformDomainToApi(personRisks, domainSummary.crn))
      assertThat(apiSummary.person).isNotNull
    }
  }

  @Nested
  inner class Cas3NotesFiltering {

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `getSortedReferralHistoryNotes correctly filters user notes`(includeUserNotes: Boolean) {
      val application = TemporaryAccommodationApplicationEntityFactory().withDefaults().produce()
      val assessment = TemporaryAccommodationAssessmentEntityFactory().withApplication(application)
        .produce()
      assessment.referralHistoryNotes = mutableListOf(
        AssessmentReferralHistoryUserNoteEntityFactory().withAssessment(assessment).produce(),
        AssessmentReferralHistorySystemNoteEntityFactory().withAssessment(assessment).produce(),
      )

      val result =
        assessmentTransformer.getSortedReferralHistoryNotes(
          assessment,
          cas3Events = emptyList(),
          includeUserNotes = includeUserNotes,
        )

      assertThat(assessment.referralHistoryNotes.size).isEqualTo(2)
      if (includeUserNotes) {
        assertThat(result.size).isEqualTo(2)
      } else {
        assertThat(result.size).isEqualTo(1)
      }
    }
  }

  @SuppressWarnings("LongParameterList")
  class DomainAssessmentSummaryImpl(
    override val type: String,
    override val id: UUID,
    override val applicationId: UUID,
    override val createdAt: Instant,
    override val riskRatings: String?,
    override val arrivalDate: Instant?,
    override val completed: Boolean,
    override val allocated: Boolean,
    override val decision: String?,
    override val crn: String,
    override val status: DomainAssessmentSummaryStatus?,
    override val dueAt: Instant?,
    override val probationDeliveryUnitName: String?,
  ) : DomainAssessmentSummary
}
