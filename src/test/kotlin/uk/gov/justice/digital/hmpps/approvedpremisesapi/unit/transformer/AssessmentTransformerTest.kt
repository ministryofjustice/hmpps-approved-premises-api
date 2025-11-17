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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentReferralHistorySystemNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentReferralHistoryUserNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
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
import java.time.OffsetDateTime
import java.util.UUID

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
    every { mockUserTransformer.transformCas1JpaToApi(any()) } returns approvedPremisesUser
    every { mockApplicationsTransformer.transformJpaToCas1Application(any(), any()) } returns mockk<Cas1Application>()
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
      assertThat(apiSummary.status).isEqualTo(ApprovedPremisesAssessmentStatus.awaitingResponse)
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
      val apiSummary = assessmentTransformer.transformDomainToCas1AssessmentSummary(domainSummary, mockk())

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
