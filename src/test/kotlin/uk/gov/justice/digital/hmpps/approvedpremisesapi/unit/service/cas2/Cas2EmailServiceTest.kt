package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import io.sentry.Sentry
import io.sentry.protocol.SentryId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Agency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2EmailService
import java.time.OffsetDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
class Cas2EmailServiceTest {

  @MockK
  lateinit var emailNotificationService: EmailNotificationService

  @MockK
  lateinit var notifyConfig: NotifyConfig

  @MockK
  lateinit var nomisUserRepository: NomisUserRepository

  @MockK
  lateinit var prisonsApiClient: PrisonsApiClient

  @InjectMockKs
  lateinit var emailService: Cas2EmailService

  private val prisoner = Prisoner(prisonId = "A1234AB", prisonName = "HM LONDON")
  private val nomsNumber = "NOMSABC"
  private val templateId = "SOME ID"
  private val user = NomisUserEntityFactory().produce()
  private val personalisation = mapOf(
    "nomsNumber" to nomsNumber,
    "receivingPrisonName" to prisoner.prisonName,
  )
  private val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withCreatedByUser(user).produce()
  private val applicationAssignment = Cas2ApplicationAssignmentEntity(
    id = UUID.randomUUID(),
    application = application,
    prisonCode = "OLD PRISON CODE",
    createdAt = OffsetDateTime.now(),
    allocatedPomUserId = user.id,
  )

  @Test
  fun `send email to Nacro when location changes`() {
    application.applicationAssignments.add(applicationAssignment)
    val agency =
      Agency(agencyId = applicationAssignment.prisonCode, description = "HMS LIVERPOOL", agencyType = "prison")
    val personalisationForNacro = mapOf(
      "receivingPrisonName" to prisoner.prisonName,
      "transferringPrisonName" to agency.description,
      "link" to nomsNumber,
    )
    every {
      emailNotificationService.sendEmail(
        eq("tbc"),
        eq(templateId),
        eq(personalisationForNacro),
      )
    } returns Unit
    every { notifyConfig.templates.toNacroApplicationTransferredToAnotherPrison } returns templateId
    every { prisonsApiClient.getAgencyDetails(eq(applicationAssignment.prisonCode)) } returns ClientResult.Success(
      HttpStatus.OK,
      agency,
    )

    emailService.sendLocationChangedEmailToNacro(application, nomsNumber, prisoner)

    verify(exactly = 1) {
      emailNotificationService.sendEmail(
        eq("tbc"),
        eq(templateId),
        eq(personalisationForNacro),
      )
    }
    verify(exactly = 1) { notifyConfig.templates.toNacroApplicationTransferredToAnotherPrison }
    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(applicationAssignment.prisonCode)) }

  }

  @Test
  fun `send email to receiving POM Unit when location changes`() {
    application.applicationAssignments.add(applicationAssignment)
    val personalisationWithLink = mapOf(
      "nomsNumber" to nomsNumber,
      "receivingPrisonName" to prisoner.prisonName,
      "link" to "NOMSABC",
    )
    every {
      emailNotificationService.sendEmail(
        eq("tbc"),
        eq(templateId),
        eq(personalisationWithLink),
      )
    } returns Unit
    every { notifyConfig.templates.toReceivingPomUnitApplicationTransferredToAnotherPrison } returns templateId

    emailService.sendLocationChangedEmailToReceivingPomUnit(application, nomsNumber)

    verify(exactly = 1) {
      emailNotificationService.sendEmail(
        eq("tbc"),
        eq(templateId),
        eq(personalisationWithLink),
      )
    }
    verify(exactly = 1) { notifyConfig.templates.toReceivingPomUnitApplicationTransferredToAnotherPrison }
  }

  @Test
  fun `send email to transferring POM Unit when location changes`() {
    application.applicationAssignments.add(applicationAssignment)

    every {
      emailNotificationService.sendEmail(
        eq("tbc"),
        eq(templateId),
        eq(personalisation),
      )
    } returns Unit
    every { notifyConfig.templates.toTransferringPomUnitApplicationTransferredToAnotherPrison } returns templateId

    emailService.sendLocationChangedEmailToTransferringPomUnit(application.id, nomsNumber, prisoner)

    verify(exactly = 1) {
      emailNotificationService.sendEmail(
        eq("tbc"),
        eq(templateId),
        eq(personalisation),
      )
    }
    verify(exactly = 1) { notifyConfig.templates.toTransferringPomUnitApplicationTransferredToAnotherPrison }
  }

  @Test
  fun `send email to referring POM when location changes`() {
    application.applicationAssignments.add(applicationAssignment)

    every {
      emailNotificationService.sendEmail(
        eq(user.email!!),
        eq(templateId),
        eq(personalisation),
      )
    } returns Unit
    every { notifyConfig.templates.toTransferringPomApplicationTransferredToAnotherPrison } returns templateId
    every { nomisUserRepository.findById(eq(user.id)) } returns Optional.of(user)

    emailService.sendLocationChangedEmailToTransferringPom(application, nomsNumber, prisoner)

    verify(exactly = 1) {
      emailNotificationService.sendEmail(
        eq(user.email!!),
        eq(templateId),
        eq(personalisation),
      )
    }
    verify(exactly = 1) { notifyConfig.templates.toTransferringPomApplicationTransferredToAnotherPrison }
    verify(exactly = 1) { nomisUserRepository.findById(eq(user.id)) }
  }

  @Test
  fun `do not send email as no email available for old POM`() {
    val userWithNoEmail = NomisUserEntityFactory().withEmail(null).produce()

    val applicationWithNoEmail =
      Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withCreatedByUser(userWithNoEmail).produce()
    applicationWithNoEmail.applicationAssignments.add(
      Cas2ApplicationAssignmentEntity(
        id = UUID.randomUUID(),
        application = applicationWithNoEmail,
        prisonCode = "OLD PRISON CODE",
        createdAt = OffsetDateTime.now(),
        allocatedPomUserId = userWithNoEmail.id,
      ),
    )

    every { emailNotificationService.sendEmail(any(), any(), any()) } returns Unit
    every { notifyConfig.templates.toTransferringPomApplicationTransferredToAnotherPrison } returns templateId
    every { nomisUserRepository.findById(eq(userWithNoEmail.id)) } returns Optional.of(userWithNoEmail)
    val slot = slot<String>()
    mockkStatic(Sentry::class)
    every { Sentry.captureMessage(capture(slot)) } returns SentryId.EMPTY_ID

    val errorMessage =
      "Email not found for User ${userWithNoEmail.id}. Unable to send email for Location Transfer on Application ${applicationWithNoEmail.id}"

    emailService.sendLocationChangedEmailToTransferringPom(applicationWithNoEmail, nomsNumber, prisoner)

    verify(exactly = 0) { emailNotificationService.sendEmail(any(), any(), any()) }
    verify(exactly = 1) { notifyConfig.templates.toTransferringPomApplicationTransferredToAnotherPrison }
    verify(exactly = 1) { nomisUserRepository.findById(eq(userWithNoEmail.id)) }
    verify(exactly = 1) { Sentry.captureMessage(any()) }

    assertTrue(slot.isCaptured)
    assertTrue(slot.captured == errorMessage)
  }

  @Test
  fun `do not send email as old POM is not in NomisUsers Table`() {
    application.applicationAssignments.add(applicationAssignment)
    val errorMessage = "No user for id ${user.id} found"
    every { emailNotificationService.sendEmail(any(), any(), any()) } returns Unit
    every { notifyConfig.templates.toTransferringPomApplicationTransferredToAnotherPrison } returns templateId
    every { nomisUserRepository.findById(eq(user.id)) } throws RuntimeException(errorMessage)

    val exception = assertThrows<RuntimeException> {
      emailService.sendLocationChangedEmailToTransferringPom(
        application,
        nomsNumber,
        prisoner,
      )
    }
    assertThat(exception.message).isEqualTo(errorMessage)
    application.applicationAssignments.add(applicationAssignment)

    verify(exactly = 0) { emailNotificationService.sendEmail(any(), any(), any()) }
    verify(exactly = 0) { notifyConfig.templates.toTransferringPomApplicationTransferredToAnotherPrison }
    verify(exactly = 1) { nomisUserRepository.findById(eq(user.id)) }
  }

  @Test
  fun `do not send email as no application assignments allocatedPomUserId is not null`() {
    val applicationAssignmentWithNullPomUserId = Cas2ApplicationAssignmentEntity(
      id = UUID.randomUUID(),
      application = application,
      prisonCode = "OLD PRISON CODE",
      createdAt = OffsetDateTime.now(),
      allocatedPomUserId = null,
    )

    application.applicationAssignments.add(applicationAssignmentWithNullPomUserId)
    val errorMessage = "Collection contains no element matching the predicate."

    every { emailNotificationService.sendEmail(any(), any(), any()) } returns Unit
    every { notifyConfig.templates.toTransferringPomApplicationTransferredToAnotherPrison } returns templateId
    every { nomisUserRepository.findById(any()) } throws RuntimeException(errorMessage)

    val exception = assertThrows<RuntimeException> {
      emailService.sendLocationChangedEmailToTransferringPom(
        application,
        nomsNumber,
        prisoner,
      )
    }
    assertThat(exception.message).isEqualTo(errorMessage)

    verify(exactly = 0) { emailNotificationService.sendEmail(any(), any(), any()) }
    verify(exactly = 0) { notifyConfig.templates.toTransferringPomApplicationTransferredToAnotherPrison }
    verify(exactly = 0) { nomisUserRepository.findById(any()) }
  }
}
