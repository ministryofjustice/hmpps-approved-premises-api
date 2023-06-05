package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.time.LocalDate
import java.time.OffsetDateTime

class TaskTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockUserTransformer = mockk<UserTransformer>()

  private val mockPerson = mockk<Person>()
  private val mockUser = mockk<ApprovedPremisesUser>()
  private val mockOffenderDetailSummary = mockk<OffenderDetailSummary>()
  private val mockInmateDetail = mockk<InmateDetail>()

  private val user = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private val application = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(user)
    .produce()

  private val assessmentFactory = AssessmentEntityFactory()
    .withApplication(application)
    .withAllocatedToUser(user)
    .withCreatedAt(OffsetDateTime.parse("2022-12-07T10:40:00Z"))

  private val taskTransformer = TaskTransformer(mockPersonTransformer, mockUserTransformer)

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToApi(mockOffenderDetailSummary, mockInmateDetail) } returns mockPerson
    every { mockUserTransformer.transformJpaToApi(user, ServiceName.approvedPremises) } returns mockUser
  }

  @Test
  fun `Not started assessment is correctly transformed`() {
    var assessment = assessmentFactory.produce()

    assessment.data = null

    var result = taskTransformer.transformAssessmentToTask(assessment, mockOffenderDetailSummary, mockInmateDetail)

    assertThat(result.status).isEqualTo(TaskStatus.notStarted)
    assertThat(result.taskType).isEqualTo(TaskType.assessment)
    assertThat(result.applicationId).isEqualTo(application.id)
    assertThat(result.dueDate).isEqualTo(LocalDate.parse("2022-12-17"))
    assertThat(result.person).isEqualTo(mockPerson)
    assertThat(result.allocatedToStaffMember).isEqualTo(mockUser)
  }

  @Test
  fun `In Progress assessment is correctly transformed`() {
    var assessment = assessmentFactory
      .withDecision(null)
      .withData("{\"test\": \"data\"}")
      .produce()

    var result = taskTransformer.transformAssessmentToTask(assessment, mockOffenderDetailSummary, mockInmateDetail)

    assertThat(result.status).isEqualTo(TaskStatus.inProgress)
  }

  @Test
  fun `Complete assessment is correctly transformed`() {
    var assessment = assessmentFactory
      .withDecision(AssessmentDecision.ACCEPTED)
      .withData("{\"test\": \"data\"}")
      .produce()

    var result = taskTransformer.transformAssessmentToTask(assessment, mockOffenderDetailSummary, mockInmateDetail)

    assertThat(result.status).isEqualTo(TaskStatus.complete)
  }
}
