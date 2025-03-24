package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2EmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas2EmailServiceTest {

  private val notifyConfig = mockk<NotifyConfig>()
  private val emailNotificationService = mockk<EmailNotificationService>()
  private val nomisUserRepository = mockk<NomisUserRepository>()
  private val prisonsApiClient = mockk<PrisonsApiClient>()
  private val applicationUrlTemplate = UrlTemplate("/applications/#id/overview").toString()

  private val emailService = Cas2EmailService(
    emailNotificationService,
    notifyConfig,
    nomisUserRepository,
    prisonsApiClient,
    applicationUrlTemplate,
  )

  private val prisoner = Prisoner(prisonId = "A1234AB", prisonName = "HM LONDON")
  private val nomsNumber = "NOMSABC"
  private val templateId = "SOME ID"
  private val user = NomisUserEntityFactory().produce()
  private val personalisation = mapOf(
    "nomsNumber" to nomsNumber,
    "receivingPrisonName" to prisoner.prisonName,
  )
  private val application =
    Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber)
      .withCreatedByUser(user).produce()
  private val link = applicationUrlTemplate.replace("#id", application.id.toString())
  private val applicationAssignmentOld = Cas2ApplicationAssignmentEntity(
    id = UUID.randomUUID(),
    application = application,
    prisonCode = "OLD PRISON CODE",
    createdAt = OffsetDateTime.now(),
    allocatedPomUserId = user.id,
  )
  private val applicationAssignmentNew = Cas2ApplicationAssignmentEntity(
    id = UUID.randomUUID(),
    application = application,
    prisonCode = "NEW PRISON CODE",
    createdAt = OffsetDateTime.now(),
    allocatedPomUserId = user.id,
  )
  private val oldAgency =
    Agency(agencyId = applicationAssignmentOld.prisonCode, description = "HMS LIVERPOOL", agencyType = "prison")
  private val newAgency =
    Agency(agencyId = applicationAssignmentNew.prisonCode, description = "HMS LONDON", agencyType = "prison")

  @Test
  fun `send email to Nacro when location changes`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    val personalisationForNacro = mapOf(
      "nomsNumber" to nomsNumber,
      "receivingPrisonName" to prisoner.prisonName,
      "transferringPrisonName" to oldAgency.description,
      "link" to link,
    )
    every {
      emailNotificationService.sendEmail(
        eq("tbc"),
        eq(templateId),
        eq(personalisationForNacro),
      )
    } returns Unit
    every { notifyConfig.templates.toNacroApplicationTransferredToAnotherPrison } returns templateId

    every { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) } returns ClientResult.Success(
      HttpStatus.OK,
      oldAgency,
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
    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) }
  }

  @Test
  fun `send email to Nacro when allocation changes`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    val personalisationForNacro = mapOf(
      "nomsNumber" to nomsNumber,
      "receivingPrisonName" to newAgency.description,
      "link" to link,
    )
    every {
      emailNotificationService.sendEmail(
        eq("tbc"),
        eq(templateId),
        eq(personalisationForNacro),
      )
    } returns Unit
    every { notifyConfig.templates.toNacroApplicationTransferredToAnotherPom } returns templateId

    every { prisonsApiClient.getAgencyDetails(eq(newAgency.agencyId)) } returns ClientResult.Success(
      HttpStatus.OK,
      newAgency,
    )

    emailService.sendAllocationChangedEmailToNacro(application, nomsNumber)

    verify(exactly = 1) {
      emailNotificationService.sendEmail(
        eq("tbc"),
        eq(templateId),
        eq(personalisationForNacro),
      )
    }
    verify(exactly = 1) { notifyConfig.templates.toNacroApplicationTransferredToAnotherPom }
    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(newAgency.agencyId)) }
  }

  @Test
  fun `send email to receiving POM Unit when location changes`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    val personalisationWithLink = mapOf(
      "nomsNumber" to nomsNumber,
      "transferringPrisonName" to oldAgency.description,
      "link" to link,
      "applicationStatus" to "PLACEHOLDER",
    )
    every {
      emailNotificationService.sendEmail(
        eq("tbc"),
        eq(templateId),
        eq(personalisationWithLink),
      )
    } returns Unit
    every { notifyConfig.templates.toReceivingPomUnitApplicationTransferredToAnotherPrison } returns templateId
    every { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) } returns ClientResult.Success(
      HttpStatus.OK,
      oldAgency,
    )
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
    application.applicationAssignments.add(applicationAssignmentNew)

    every {
      emailNotificationService.sendEmail(
        eq("tbc"),
        eq(templateId),
        eq(personalisation),
      )
    } returns Unit
    every { notifyConfig.templates.toTransferringPomUnitApplicationTransferredToAnotherPrison } returns templateId

    emailService.sendLocationChangedEmailToTransferringPomUnit(nomsNumber, prisoner)

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
  fun `send email to transferring POM when location changes`() {
    application.applicationAssignments.add(applicationAssignmentNew)

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
  fun `send email to receiving POM when location changes`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)

    val personalisationForNewPom = mapOf(
      "nomsNumber" to nomsNumber,
      "transferringPrisonName" to oldAgency.description,
      "link" to link,
      "applicationStatus" to "PLACEHOLDER",
    )

    every {
      emailNotificationService.sendEmail(
        eq(user.email!!),
        eq(templateId),
        eq(personalisationForNewPom),
      )
    } returns Unit
    every { notifyConfig.templates.toReceivingPomApplicationTransferredToAnotherPom } returns templateId
    every { nomisUserRepository.findById(eq(user.id)) } returns Optional.of(user)
    every { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) } returns ClientResult.Success(
      HttpStatus.OK,
      oldAgency,
    )

    emailService.sendAllocationChangedEmailToReceivingPom(application, nomsNumber)

    verify(exactly = 1) {
      emailNotificationService.sendEmail(
        eq(user.email!!),
        eq(templateId),
        eq(personalisationForNewPom),
      )
    }
    verify(exactly = 1) { notifyConfig.templates.toReceivingPomApplicationTransferredToAnotherPom }
    verify(exactly = 1) { nomisUserRepository.findById(eq(user.id)) }
    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) }
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

    val errorMessage = "Email $templateId not sent for NOMS Number ${personalisation["nomsNumber"]}"

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
    application.applicationAssignments.add(applicationAssignmentNew)
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
    application.applicationAssignments.add(applicationAssignmentNew)

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
