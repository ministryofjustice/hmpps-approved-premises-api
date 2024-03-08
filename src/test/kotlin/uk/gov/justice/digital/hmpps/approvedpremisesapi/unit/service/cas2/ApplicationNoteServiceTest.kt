package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import com.amazonaws.services.sns.model.NotFoundException
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.sentry.Sentry
import io.sentry.protocol.SentryId
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ExternalUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ExternalUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormattedHourOfDay
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationNoteServiceTest {
  private val mockApplicationRepository = mockk<Cas2ApplicationRepository>()
  private val mockApplicationNoteRepository = mockk<Cas2ApplicationNoteRepository>()
  private val mockUserService = mockk<NomisUserService>()
  private val mockExternalUserService = mockk<ExternalUserService>()
  private val mockHttpAuthService = mockk<HttpAuthService>()
  private val mockEmailNotificationService = mockk<EmailNotificationService>()
  private val mockNotifyConfig = mockk<NotifyConfig>()

  private val applicationNoteService = uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.ApplicationNoteService(
    mockApplicationRepository,
    mockApplicationNoteRepository,
    mockUserService,
    mockExternalUserService,
    mockHttpAuthService,
    mockEmailNotificationService,
    mockNotifyConfig,
    "http://frontend/applications/#id/overview",
  )

  @Nested
  inner class CreateApplicationNote {

    private val referrer = NomisUserEntityFactory().produce()

    @Nested
    inner class AsNomisUser {

      @BeforeEach
      fun setup() {
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getCas2AuthenticatedPrincipalOrThrow() } returns mockPrincipal
        every { mockPrincipal.isExternalUser() } returns false
      }

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
          createdByUser = referrer,
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

          verify(exactly = 0) { mockEmailNotificationService.sendEmail(any(), any(), any()) }
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

    @Nested
    inner class AsExternalUser {
      private val externalUser = ExternalUserEntityFactory().produce()

      @BeforeEach
      fun setup() {
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getCas2AuthenticatedPrincipalOrThrow() } returns mockPrincipal
        every { mockPrincipal.isExternalUser() } returns true
        every { mockNotifyConfig.templates.cas2NoteAddedForReferrer } returns "abc123"
      }

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
          createdByUser = externalUser,
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
          every { mockExternalUserService.getUserForRequest() } returns externalUser
          every { mockApplicationNoteRepository.save(any()) } answers
            {
              noteEntity
            }
          every {
            mockEmailNotificationService.sendEmail(
              recipientEmailAddress = referrer.email!!,
              templateId = "abc123",
              personalisation = mapOf(
                "dateNoteAdded" to noteEntity.createdAt.toLocalDate().toCas2UiFormat(),
                "timeNoteAdded" to noteEntity.createdAt.toCas2UiFormattedHourOfDay(),
                "nomsNumber" to "NOMSABC",
                "applicationType" to "Home Detention Curfew (HDC)",
                "applicationURl" to "http://frontend/applications/$applicationId/overview",
              ),
            )
          } just Runs

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

        @Test
        fun `alerts Sentry when the Referrer does not have an email`() {
          val submittedApplicationWithNoReferrerEmail = Cas2ApplicationEntityFactory()
            .withCreatedByUser(NomisUserEntityFactory().withEmail(null).produce())
            .withCrn("CRN123")
            .withNomsNumber("NOMSABC")
            .withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(2))
            .produce()

          every { mockApplicationRepository.findByIdOrNull(applicationId) } answers
            {
              submittedApplicationWithNoReferrerEmail
            }
          every { mockExternalUserService.getUserForRequest() } returns externalUser
          every { mockApplicationNoteRepository.save(any()) } answers
            {
              noteEntity
            }
          mockkStatic(Sentry::class)

          every {
            Sentry.captureException(
              RuntimeException(
                "Email not found for User ${submittedApplicationWithNoReferrerEmail.createdByUser.id}. " +
                  "Unable to send email for Note ${noteEntity.id} on Application ${submittedApplicationWithNoReferrerEmail.id}",
                NotFoundException("Email not found for User ${submittedApplicationWithNoReferrerEmail.createdByUser.id}"),
              ),
            )
          } returns SentryId.EMPTY_ID

          applicationNoteService.createApplicationNote(
            applicationId = applicationId,
            NewCas2ApplicationNote(note = "new note"),
          )

          verify(exactly = 1) {
            Sentry.captureException(
              any(),
            )
          }
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

          every { mockExternalUserService.getUserForRequest() } returns externalUser

          Assertions.assertThat(
            applicationNoteService.createApplicationNote(
              applicationId = UUID.randomUUID(),
              note = NewCas2ApplicationNote(note = "note for missing app"),
            ) is AuthorisableActionResult.NotFound,
          ).isTrue

          verify(exactly = 0) { mockEmailNotificationService.sendEmail(any(), any(), any()) }
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

          every { mockExternalUserService.getUserForRequest() } returns externalUser

          val result = applicationNoteService.createApplicationNote(
            applicationId = applicationNotSubmitted.id,
            note = NewCas2ApplicationNote(note = "note for in progress app"),
          )
          Assertions.assertThat(result is AuthorisableActionResult.Success).isTrue
          result as AuthorisableActionResult.Success

          Assertions.assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
          val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

          Assertions.assertThat(validatableActionResult.message).isEqualTo("This application has not been submitted")

          verify(exactly = 0) { mockEmailNotificationService.sendEmail(any(), any(), any()) }
        }
      }
    }
  }
}
