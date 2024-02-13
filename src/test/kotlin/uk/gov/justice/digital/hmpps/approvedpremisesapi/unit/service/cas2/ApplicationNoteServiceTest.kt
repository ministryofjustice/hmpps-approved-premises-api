package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationNoteServiceTest {
  private val mockApplicationRepository = mockk<Cas2ApplicationRepository>()
  private val mockApplicationNoteRepository = mockk<Cas2ApplicationNoteRepository>()
  private val mockUserService = mockk<NomisUserService>()

  private val applicationNoteService = uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.ApplicationNoteService(
    mockApplicationRepository,
    mockApplicationNoteRepository,
    mockUserService,
  )

  @Nested
  inner class CreateApplicationNote {

    private val referrer = NomisUserEntityFactory().produce()

    @Nested
    inner class WhenSuccessful {
      private val submittedApplication = Cas2ApplicationEntityFactory()
        .withCreatedByUser(referrer)
        .withCrn("CRN123")
        .withNomsNumber("NOMSABC")
        .withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(2))
        .produce()
      private val applicationId = submittedApplication.id
      private val noteEntity = Cas2ApplicationNoteEntity(
        id = UUID.randomUUID(),
        createdByNomisUser = referrer,
        application = submittedApplication,
        body = "new note",
        createdAt = OffsetDateTime.now().randomDateTimeBefore(1),
      )

      @Test
      fun `returns Success result with entity from db`() {
        every { mockApplicationRepository.findByIdOrNull(applicationId) } answers
          {
            submittedApplication
          }
        every { mockUserService.getUserForRequest() } returns referrer
        every { mockApplicationNoteRepository.save(any()) } answers
          {
            noteEntity
          }

        val result = applicationNoteService.createApplicationNote(
          applicationId = applicationId,
          NewCas2ApplicationNote(note = "new note"),
        )

        verify(exactly = 1) { mockApplicationNoteRepository.save(any()) }

        Assertions.assertThat(result is AuthorisableActionResult.Success).isTrue
        result as AuthorisableActionResult.Success

        Assertions.assertThat(result.entity is ValidatableActionResult.Success).isTrue
        val validatableActionResult = result.entity as ValidatableActionResult.Success

        val createdNote = validatableActionResult.entity

        Assertions.assertThat(createdNote).isEqualTo(noteEntity)
      }
    }

    @Nested
    inner class WhenUnsuccessful {
      @Test
      fun `returns Not Found when application not found`() {
        every { mockApplicationRepository.findByIdOrNull(any()) } answers
          {
            null
          }

        Assertions.assertThat(
          applicationNoteService.createApplicationNote(
            applicationId = UUID.randomUUID(),
            note = NewCas2ApplicationNote(note = "note for missing app"),
          ) is AuthorisableActionResult.NotFound,
        ).isTrue
      }

      @Test
      fun `returns Not Authorised when application was not created by user`() {
        val applicationCreatedByOtherUser = Cas2ApplicationEntityFactory()
          .withCreatedByUser(NomisUserEntityFactory().produce())
          .withCrn("CRN123")
          .withNomsNumber("NOMSABC")
          .withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(2))
          .produce()

        every { mockApplicationRepository.findByIdOrNull(applicationCreatedByOtherUser.id) } answers
          {
            applicationCreatedByOtherUser
          }

        every { mockUserService.getUserForRequest() } returns referrer

        Assertions.assertThat(
          applicationNoteService.createApplicationNote(
            applicationId = applicationCreatedByOtherUser.id,
            note = NewCas2ApplicationNote(note = "note for unauthorised app"),
          ) is AuthorisableActionResult.Unauthorised,
        ).isTrue
      }

      @Test
      fun `returns Validation error when application is not submitted`() {
        val applicationNotSubmitted = Cas2ApplicationEntityFactory()
          .withCreatedByUser(referrer)
          .withCrn("CRN123")
          .withNomsNumber("NOMSABC")
          .produce()

        every { mockApplicationRepository.findByIdOrNull(any()) } answers
          {
            applicationNotSubmitted
          }

        every { mockUserService.getUserForRequest() } returns referrer

        val result = applicationNoteService.createApplicationNote(
          applicationId = applicationNotSubmitted.id,
          note = NewCas2ApplicationNote(note = "note for in progress app"),
        )
        Assertions.assertThat(result is AuthorisableActionResult.Success).isTrue
        result as AuthorisableActionResult.Success

        Assertions.assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
        val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

        Assertions.assertThat(validatableActionResult.message).isEqualTo("This application has not been submitted")
      }
    }
  }
}
