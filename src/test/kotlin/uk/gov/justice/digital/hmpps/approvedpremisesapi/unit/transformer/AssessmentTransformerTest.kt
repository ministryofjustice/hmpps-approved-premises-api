package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentReferralHistorySystemNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentReferralHistoryUserNoteEntityFactory
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.JsonMapperFactory
import java.time.Instant
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

  private val jsonMapper = JsonMapperFactory.createJackson3JsonMapper()

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
