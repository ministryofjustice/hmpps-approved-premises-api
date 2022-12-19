package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import java.time.OffsetDateTime
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
          ApprovedPremisesApplicationEntityFactory()
            .withCreatedByUser(UserEntityFactory().produce())
            .produce()
        )
        .produce()
    )

    every { assessmentRepositoryMock.findAll() } returns allAssessments
    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

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
          ApprovedPremisesApplicationEntityFactory()
            .withCreatedByUser(UserEntityFactory().produce())
            .produce()
        )
        .produce()
    )

    every { assessmentRepositoryMock.findAllByAllocatedToUser_Id(user.id) } returns allocatedAssessments
    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

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
          ApprovedPremisesApplicationEntityFactory()
            .withCreatedByUser(UserEntityFactory().produce())
            .produce()
        )
        .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment
    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

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
          ApprovedPremisesApplicationEntityFactory()
            .withCreatedByUser(UserEntityFactory().produce())
            .produce()
        )
        .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment
    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.getAssessmentForUser(user, assessmentId)

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getAssessmentForUser returns not found for non-existent Assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns null
    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.getAssessmentForUser(user, assessmentId)

    assertThat(result is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `addAssessmentClarificationNote returns not found for non-existent Assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns null
    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

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
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withAllocatedToUser(UserEntityFactory().produce())
      .produce()

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

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
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withAllocatedToUser(UserEntityFactory().produce())
      .produce()

    every { assessmentClarificationNoteRepositoryMock.save(any()) } answers {
      it.invocation.args[0] as AssessmentClarificationNoteEntity
    }

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

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
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withAllocatedToUser(user)
      .produce()

    every { assessmentClarificationNoteRepositoryMock.save(any()) } answers {
      it.invocation.args[0] as AssessmentClarificationNoteEntity
    }

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.addAssessmentClarificationNote(user, assessmentId, "clarification note")

    assertThat(result is AuthorisableActionResult.Success).isTrue

    verify(exactly = 1) { assessmentClarificationNoteRepositoryMock.save(any()) }
  }

  @Test
  fun `updateAssessment returns unauthorised for Assessment not allocated to user`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withAllocatedToUser(UserEntityFactory().produce())
      .produce()

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.updateAssessment(user, assessmentId, "{}")

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `updateAssessment returns general validation error for Assessment where schema is outdated`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withAllocatedToUser(user)
      .produce()

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.updateAssessment(user, assessmentId, "{}")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.GeneralValidationError)
    val generalValidationError = validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(generalValidationError.message).isEqualTo("The schema version is outdated")
  }

  @Test
  fun `updateAssessment returns general validation error for Assessment where decision has already been taken`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}"
    )

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withAllocatedToUser(user)
      .withAssessmentSchema(schema)
      .produce()

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    val result = assessmentService.updateAssessment(user, assessmentId, "{}")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.GeneralValidationError)
    val generalValidationError = validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(generalValidationError.message).isEqualTo("A decision has already been taken on this assessment")
  }

  @Test
  fun `updateAssessment returns updated assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}"
    )

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withAllocatedToUser(user)
      .withAssessmentSchema(schema)
      .produce()

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as AssessmentEntity }

    val result = assessmentService.updateAssessment(user, assessmentId, "{\"test\": \"data\"}")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.Success)
    val updatedAssessment = (validationResult as ValidatableActionResult.Success).entity
    assertThat(updatedAssessment.data).isEqualTo("{\"test\": \"data\"}")
  }

  @Test
  fun `acceptAssessment returns unauthorised for Assessment not allocated to user`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withAllocatedToUser(UserEntityFactory().produce())
      .produce()

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.acceptAssessment(user, assessmentId, "{}")

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `acceptAssessment returns general validation error for Assessment where schema is outdated`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withAllocatedToUser(user)
      .produce()

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.acceptAssessment(user, assessmentId, "{}")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.GeneralValidationError)
    val generalValidationError = validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(generalValidationError.message).isEqualTo("The schema version is outdated")
  }

  @Test
  fun `acceptAssessment returns general validation error for Assessment where decision has already been taken`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}"
    )

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withAllocatedToUser(user)
      .withAssessmentSchema(schema)
      .produce()

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    val result = assessmentService.acceptAssessment(user, assessmentId, "{}")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.GeneralValidationError)
    val generalValidationError = validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(generalValidationError.message).isEqualTo("A decision has already been taken on this assessment")
  }

  @Test
  fun `acceptAssessment returns field validation error when JSON schema not satisfied by data`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}"
    )

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withAllocatedToUser(user)
      .withAssessmentSchema(schema)
      .withData("{\"test\": \"data\"}")
      .produce()

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    every { jsonSchemaServiceMock.validate(schema, "{\"test\": \"data\"}") } returns false

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as AssessmentEntity }

    val result = assessmentService.acceptAssessment(user, assessmentId, "{\"test\": \"data\"}")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.FieldValidationError)
    val fieldValidationError = (validationResult as ValidatableActionResult.FieldValidationError)
    assertThat(fieldValidationError.validationMessages).contains(
      entry("$.data", "invalid")
    )
  }

  @Test
  fun `acceptAssessment returns updated assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}"
    )

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withAllocatedToUser(user)
      .withAssessmentSchema(schema)
      .withData("{\"test\": \"data\"}")
      .produce()

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    every { jsonSchemaServiceMock.validate(schema, "{\"test\": \"data\"}") } returns true

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as AssessmentEntity }

    val result = assessmentService.acceptAssessment(user, assessmentId, "{\"test\": \"data\"}")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.Success)
    val updatedAssessment = (validationResult as ValidatableActionResult.Success).entity
    assertThat(updatedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
    assertThat(updatedAssessment.submittedAt).isNotNull()
    assertThat(updatedAssessment.document).isEqualTo("{\"test\": \"data\"}")
  }

  @Test
  fun `rejectAssessment returns unauthorised for Assessment not allocated to user`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withAllocatedToUser(UserEntityFactory().produce())
      .produce()

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.rejectAssessment(user, assessmentId, "{}", "reasoning")

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `rejectAssessment returns general validation error for Assessment where schema is outdated`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withAllocatedToUser(user)
      .produce()

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.rejectAssessment(user, assessmentId, "{}", "reasoning")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.GeneralValidationError)
    val generalValidationError = validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(generalValidationError.message).isEqualTo("The schema version is outdated")
  }

  @Test
  fun `rejectAssessment returns general validation error for Assessment where decision has already been taken`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}"
    )

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withAllocatedToUser(user)
      .withAssessmentSchema(schema)
      .produce()

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    val result = assessmentService.rejectAssessment(user, assessmentId, "{}", "reasoning")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.GeneralValidationError)
    val generalValidationError = validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(generalValidationError.message).isEqualTo("A decision has already been taken on this assessment")
  }

  @Test
  fun `rejectAssessment returns field validation error when JSON schema not satisfied by data`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}"
    )

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withAllocatedToUser(user)
      .withAssessmentSchema(schema)
      .withData("{\"test\": \"data\"}")
      .produce()

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    every { jsonSchemaServiceMock.validate(schema, "{\"test\": \"data\"}") } returns false

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as AssessmentEntity }

    val result = assessmentService.rejectAssessment(user, assessmentId, "{\"test\": \"data\"}", "reasoning")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.FieldValidationError)
    val fieldValidationError = (validationResult as ValidatableActionResult.FieldValidationError)
    assertThat(fieldValidationError.validationMessages).contains(
      entry("$.data", "invalid")
    )
  }

  @Test
  fun `rejectAssessment returns updated assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}"
    )

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns AssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(UserEntityFactory().produce())
          .produce()
      )
      .withAllocatedToUser(user)
      .withAssessmentSchema(schema)
      .withData("{\"test\": \"data\"}")
      .produce()

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    every { jsonSchemaServiceMock.validate(schema, "{\"test\": \"data\"}") } returns true

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as AssessmentEntity }

    val result = assessmentService.rejectAssessment(user, assessmentId, "{\"test\": \"data\"}", "reasoning")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.Success)
    val updatedAssessment = (validationResult as ValidatableActionResult.Success).entity
    assertThat(updatedAssessment.decision).isEqualTo(AssessmentDecision.REJECTED)
    assertThat(updatedAssessment.rejectionRationale).isEqualTo("reasoning")
    assertThat(updatedAssessment.submittedAt).isNotNull()
    assertThat(updatedAssessment.document).isEqualTo("{\"test\": \"data\"}")
  }
}
