package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.JsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import java.util.UUID

class AssessmentServiceTest {
  private val userRepositoryMock = mockk<UserRepository>()
  private val assessmentRepositoryMock = mockk<AssessmentRepository>()
  private val assessmentClarificationNoteRepositoryMock = mockk<AssessmentClarificationNoteRepository>()
  private val jsonSchemaServiceMock = mockk<JsonSchemaService>()

  private val assessmentService = AssessmentService(
    userRepositoryMock,
    assessmentRepositoryMock,
    assessmentClarificationNoteRepositoryMock,
    jsonSchemaServiceMock
  )

  @Test
  fun `getVisibleAssessmentsForUser fetches all assessments for workflow managers`() {
    val user = UserEntityFactory()
      .produce()

    user.roles.add(
      UserRoleAssignmentEntityFactory()
        .withRole(UserRole.WORKFLOW_MANAGER)
        .withUser(user)
        .produce()
    )

    val allAssessments = listOf(
      AssessmentEntityFactory()
        .withAllocatedToUser(
          UserEntityFactory().produce()
        )
        .withApplication(
          ApplicationEntityFactory()
            .withCreatedByUser(UserEntityFactory().produce())
            .produce()
        )
        .produce()
    )

    every { assessmentRepositoryMock.findAll() } returns allAssessments
    every { jsonSchemaServiceMock.getNewestSchema(JsonSchemaType.ASSESSMENT) } returns JsonSchemaEntityFactory().produce()

    val result = assessmentService.getVisibleAssessmentsForUser(user)

    assertThat(result).containsAll(allAssessments)

    verify(exactly = 1) { assessmentRepositoryMock.findAll() }
  }

  @Test
  fun `getVisibleAssessmentsForUser fetches only allocated assessments`() {
    val user = UserEntityFactory()
      .produce()

    val allocatedAssessments = listOf(
      AssessmentEntityFactory()
        .withAllocatedToUser(user)
        .withApplication(
          ApplicationEntityFactory()
            .withCreatedByUser(UserEntityFactory().produce())
            .produce()
        )
        .produce()
    )

    every { assessmentRepositoryMock.findAllByAllocatedToUser_Id(user.id) } returns allocatedAssessments
    every { jsonSchemaServiceMock.getNewestSchema(JsonSchemaType.ASSESSMENT) } returns JsonSchemaEntityFactory().produce()

    val result = assessmentService.getVisibleAssessmentsForUser(user)

    assertThat(result).containsAll(allocatedAssessments)

    verify(exactly = 1) { assessmentRepositoryMock.findAllByAllocatedToUser_Id(user.id) }
  }

  @Test
  fun `getAssessmentForUser gets any assessment for workflow manager`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    user.roles.add(
      UserRoleAssignmentEntityFactory()
        .withRole(UserRole.WORKFLOW_MANAGER)
        .withUser(user)
        .produce()
    )

    val assessment =
      AssessmentEntityFactory()
        .withId(assessmentId)
        .withAllocatedToUser(
          UserEntityFactory().produce()
        )
        .withApplication(
          ApplicationEntityFactory()
            .withCreatedByUser(UserEntityFactory().produce())
            .produce()
        )
        .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment
    every { jsonSchemaServiceMock.getNewestSchema(JsonSchemaType.ASSESSMENT) } returns JsonSchemaEntityFactory().produce()

    val result = assessmentService.getAssessmentForUser(user, assessmentId)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity).isEqualTo(assessment)
  }

  @Test
  fun `getAssessmentForUser does not get assessments allocated to other users for non-workflow manager`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    val assessment =
      AssessmentEntityFactory()
        .withId(assessmentId)
        .withAllocatedToUser(
          UserEntityFactory().produce()
        )
        .withApplication(
          ApplicationEntityFactory()
            .withCreatedByUser(UserEntityFactory().produce())
            .produce()
        )
        .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment
    every { jsonSchemaServiceMock.getNewestSchema(JsonSchemaType.ASSESSMENT) } returns JsonSchemaEntityFactory().produce()

    val result = assessmentService.getAssessmentForUser(user, assessmentId)

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getAssessmentForUser returns not found for non-existent Assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns null
    every { jsonSchemaServiceMock.getNewestSchema(JsonSchemaType.ASSESSMENT) } returns JsonSchemaEntityFactory().produce()

    val result = assessmentService.getAssessmentForUser(user, assessmentId)

    assertThat(result is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `addAssessmentClarificationNote returns not found for non-existent Assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns null
    every { jsonSchemaServiceMock.getNewestSchema(JsonSchemaType.ASSESSMENT) } returns JsonSchemaEntityFactory().produce()

    val result = assessmentService.addAssessmentClarificationNote(user, assessmentId, "clarification note")

    assertThat(result is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `addAssessmentClarificationNote returns unauthorised for Assessment not allocated to user`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withAllocatedToUser(UserEntityFactory().produce())
      .produce()

    every { jsonSchemaServiceMock.getNewestSchema(JsonSchemaType.ASSESSMENT) } returns JsonSchemaEntityFactory().produce()

    val result = assessmentService.addAssessmentClarificationNote(user, assessmentId, "clarification note")

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `addAssessmentClarificationNote adds note to assessment allocated to different user for workflow managers`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    user.roles.add(
      UserRoleAssignmentEntityFactory()
        .withRole(UserRole.WORKFLOW_MANAGER)
        .withUser(user)
        .produce()
    )

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withAllocatedToUser(UserEntityFactory().produce())
      .produce()

    every { assessmentClarificationNoteRepositoryMock.save(any()) } answers {
      it.invocation.args[0] as AssessmentClarificationNoteEntity
    }

    every { jsonSchemaServiceMock.getNewestSchema(JsonSchemaType.ASSESSMENT) } returns JsonSchemaEntityFactory().produce()

    val result = assessmentService.addAssessmentClarificationNote(user, assessmentId, "clarification note")

    assertThat(result is AuthorisableActionResult.Success).isTrue

    verify(exactly = 1) { assessmentClarificationNoteRepositoryMock.save(any()) }
  }

  @Test
  fun `addAssessmentClarificationNote adds note to assessment allocated to calling user`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withAllocatedToUser(user)
      .produce()

    every { assessmentClarificationNoteRepositoryMock.save(any()) } answers {
      it.invocation.args[0] as AssessmentClarificationNoteEntity
    }

    every { jsonSchemaServiceMock.getNewestSchema(JsonSchemaType.ASSESSMENT) } returns JsonSchemaEntityFactory().produce()

    val result = assessmentService.addAssessmentClarificationNote(user, assessmentId, "clarification note")

    assertThat(result is AuthorisableActionResult.Success).isTrue

    verify(exactly = 1) { assessmentClarificationNoteRepositoryMock.save(any()) }
  }
}
