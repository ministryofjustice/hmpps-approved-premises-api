package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.OmuContactDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
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
  private val statusUpdateRepository = mockk<Cas2StatusUpdateRepository>()
  private val prisonRegisterClient = mockk<PrisonRegisterClient>()
  private val applicationUrlTemplate = UrlTemplate("/applications/#id/overview").toString()
  private val nacroEmail = "nacro@test.co.uk"

  private val emailService = Cas2EmailService(
    emailNotificationService,
    notifyConfig,
    nomisUserRepository,
    statusUpdateRepository,
    prisonRegisterClient,
    applicationUrlTemplate,
    nacroEmail,
  )

  private val oldUser = NomisUserEntityFactory().produce()
  private val oldPrisonCode = "LIV"
  private val oldPrison = PrisonDto(prisonId = oldPrisonCode, prisonName = "HMP LIVERPOOL")
  private val oldOmuContactDetails = OmuContactDetails(emailAddress = "old@digital.justice.gov")

  private val newUser = NomisUserEntityFactory().produce()
  private val newPrisonCode = "LON"
  private val newPrison = PrisonDto(prisonId = newPrisonCode, prisonName = "HMP LONDON")
  private val newOmuContactDetails = OmuContactDetails(emailAddress = "new@digital.justice.gov")
  private val prisoner = Prisoner(newPrisonCode, newPrison.prisonName)
  private val nomsNumber = "NOMSABC"
  private val templateId = "SOME ID"

  private val application =
    Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber)
      .withCreatedByUser(oldUser).produce()

  private val link = applicationUrlTemplate.replace("#id", application.id.toString())
  private val applicationAssignmentOlder = Cas2ApplicationAssignmentEntity(
    id = UUID.randomUUID(),
    application = application,
    prisonCode = oldPrisonCode,
    createdAt = OffsetDateTime.now().minusDays(2),
    allocatedPomUserId = oldUser.id,
  )
  private val applicationAssignmentOld = Cas2ApplicationAssignmentEntity(
    id = UUID.randomUUID(),
    application = application,
    prisonCode = newPrisonCode,
    createdAt = OffsetDateTime.now().minusDays(1),
    allocatedPomUserId = null,
  )
  private val applicationAssignmentNew = Cas2ApplicationAssignmentEntity(
    id = UUID.randomUUID(),
    application = application,
    prisonCode = newPrisonCode,
    createdAt = OffsetDateTime.now(),
    allocatedPomUserId = newUser.id,
  )

  private val cas2StatusUpdateEntity = Cas2StatusUpdateEntityFactory()
    .withApplication(application)
    .withLabel("Status Update")
    .produce()

  @Test
  fun `send allocation changed emails`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) } returns cas2StatusUpdateEntity
    every { notifyConfig.templates.cas2ToNacroApplicationTransferredToAnotherPom } returns templateId
    every { notifyConfig.templates.cas2ToReceivingPomApplicationTransferredToAnotherPom } returns templateId
    every { prisonRegisterClient.getPrison(eq(oldPrisonCode)) } returns ClientResult.Success(status = HttpStatus.OK, body = oldPrison)
    every { prisonRegisterClient.getPrison(eq(newPrisonCode)) } returns ClientResult.Success(status = HttpStatus.OK, body = newPrison)

    every {
      emailNotificationService.sendCas2Email(
        eq(newUser.email!!),
        eq(templateId),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "transferringPrisonName" to oldPrison.prisonName,
            "link" to link,
            "applicationStatus" to cas2StatusUpdateEntity.label,
          ),
        ),
      )
    } returns Unit
    every {
      emailNotificationService.sendCas2Email(
        eq(nacroEmail),
        eq(templateId),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to newPrison.prisonName,
            "link" to link,
          ),
        ),
      )
    } returns Unit

    emailService.sendAllocationChangedEmails(application, newUser, newPrisonCode)

    verify(exactly = 2) { emailNotificationService.sendCas2Email(any(), any(), any()) }
  }

  @Test
  fun `do not send allocation changed emails and throw error as no application status found`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { prisonRegisterClient.getPrison(eq(oldPrisonCode)) } returns ClientResult.Success(status = HttpStatus.OK, body = oldPrison)
    every { prisonRegisterClient.getPrison(eq(newPrisonCode)) } returns ClientResult.Success(status = HttpStatus.OK, body = newPrison)
    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) } returns null

    val exception = assertThrows<IllegalStateException> { emailService.sendAllocationChangedEmails(application, newUser, newPrisonCode) }
    assertThat(exception.message).isEqualTo("StatusUpdate for ${application.id} not found")
  }

  @Test
  fun `do not send allocation changed emails and throw error as old prison code not found`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)

    val exception = assertThrows<IllegalStateException> { emailService.sendAllocationChangedEmails(application, newUser, newPrisonCode) }
    assertThat(exception.message).isEqualTo("Old prison code not found.")
  }

  @Test
  fun `send location changed emails`() {
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)
    every { prisonRegisterClient.getPrison(eq(oldPrisonCode)) } returns ClientResult.Success(status = HttpStatus.OK, body = oldPrison)
    every { prisonRegisterClient.getOmuContactDetails(eq(oldPrisonCode)) } returns ClientResult.Success(status = HttpStatus.OK, body = oldOmuContactDetails)
    every { prisonRegisterClient.getOmuContactDetails(eq(newPrisonCode)) } returns ClientResult.Success(status = HttpStatus.OK, body = newOmuContactDetails)
    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) } returns cas2StatusUpdateEntity
    every { notifyConfig.templates.cas2ToTransferringPomApplicationTransferredToAnotherPrison } returns templateId
    every { notifyConfig.templates.cas2ToTransferringPomUnitApplicationTransferredToAnotherPrison } returns templateId
    every { notifyConfig.templates.cas2ToReceivingPomUnitApplicationTransferredToAnotherPrison } returns templateId
    every { notifyConfig.templates.cas2ToNacroApplicationTransferredToAnotherPrison } returns templateId
    every { nomisUserRepository.findById(eq(oldUser.id)) } returns Optional.of(oldUser)

    every {
      emailNotificationService.sendCas2Email(
        eq(oldUser.email!!),
        eq(templateId),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to prisoner.prisonName,
          ),
        ),
      )
    } returns Unit
    every {
      emailNotificationService.sendCas2Email(
        eq(oldOmuContactDetails.emailAddress!!),
        eq(templateId),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to prisoner.prisonName,
          ),
        ),
      )
    } returns Unit
    every {
      emailNotificationService.sendCas2Email(
        eq(newOmuContactDetails.emailAddress!!),
        eq(templateId),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "transferringPrisonName" to oldPrison.prisonName,
            "link" to link,
            "applicationStatus" to cas2StatusUpdateEntity.label,
          ),
        ),
      )
    } returns Unit
    every {
      emailNotificationService.sendCas2Email(
        eq(nacroEmail),
        eq(templateId),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to prisoner.prisonName,
            "transferringPrisonName" to oldPrison.prisonName,
            "link" to link,
          ),
        ),
      )
    } returns Unit

    emailService.sendLocationChangedEmails(application, oldUser.id, prisoner)

    verify(exactly = 4) { emailNotificationService.sendCas2Email(any(), any(), any()) }
  }

  @Test
  fun `do not send location changed emails as old prison code not found`() {
    application.applicationAssignments.add(applicationAssignmentOld)

    val exception = assertThrows<IllegalStateException> { emailService.sendLocationChangedEmails(application, oldUser.id, prisoner) }
    assertThat(exception.message).isEqualTo("Old prison code not found.")
  }

  @Test
  fun `do not send location changed emails and throw error as no application status found`() {
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { prisonRegisterClient.getPrison(eq(oldPrisonCode)) } returns ClientResult.Success(status = HttpStatus.OK, body = oldPrison)
    every { prisonRegisterClient.getOmuContactDetails(eq(oldPrisonCode)) } returns ClientResult.Success(status = HttpStatus.OK, body = oldOmuContactDetails)
    every { prisonRegisterClient.getOmuContactDetails(eq(newPrisonCode)) } returns ClientResult.Success(status = HttpStatus.OK, body = newOmuContactDetails)
    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) } returns null
    every { nomisUserRepository.findById(eq(oldUser.id)) } returns Optional.of(oldUser)

    val exception = assertThrows<IllegalStateException> { emailService.sendLocationChangedEmails(application, oldUser.id, prisoner) }
    assertThat(exception.message).isEqualTo("StatusUpdate for ${application.id} not found")
  }

  @Test
  fun `do not send location changed emails as nomis user not found`() {
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { nomisUserRepository.findById(eq(oldUser.id)) } returns Optional.empty()

    assertThrows<NoSuchElementException> { emailService.sendLocationChangedEmails(application, oldUser.id, prisoner) }
  }

  @Test
  fun `should get old prison code`() {
    application.applicationAssignments.add(applicationAssignmentOlder)
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentNew)

    val result = emailService.getOldPrisonCode(application, applicationAssignmentNew.prisonCode)

    assertThat(result).isEqualTo(applicationAssignmentOlder.prisonCode)
  }

  @Test
  fun `should not get old prison code and throw error when no applicationAssignments`() {
    val result = emailService.getOldPrisonCode(application, applicationAssignmentNew.prisonCode)
    assertThat(result).isNull()
  }

  @Test
  fun `should not get old prison code and throw error when applicationAssignments all have new prisonCode`() {
    application.applicationAssignments.add(applicationAssignmentNew)

    val result = emailService.getOldPrisonCode(application, applicationAssignmentNew.prisonCode)
    assertThat(result).isNull()
  }

  @Test
  fun `should get prison name`() {
    every { prisonRegisterClient.getPrison(eq(oldPrisonCode)) } returns ClientResult.Success(status = HttpStatus.OK, body = oldPrison)

    val result = emailService.getPrisonName(oldPrisonCode)

    assertThat(result).isEqualTo(oldPrison.prisonName)
  }

  @Test
  fun `should throw error if prison is not found`() {
    every { prisonRegisterClient.getPrison(eq(oldPrisonCode)) } returns ClientResult.Failure.StatusCode(
      method = HttpMethod.GET,
      path = "/",
      status = HttpStatus.NOT_FOUND,
      body = "the body",
    )

    val exception = assertThrows<IllegalStateException> { emailService.getPrisonName(oldPrisonCode) }

    assertThat(exception.message).isEqualTo("No prison name for prison ID $oldPrisonCode.")
  }

  @Test
  fun `should get omu email`() {
    every { prisonRegisterClient.getOmuContactDetails(eq(oldPrisonCode)) } returns ClientResult.Success(status = HttpStatus.OK, body = oldOmuContactDetails)

    val result = emailService.getEmail(oldPrisonCode)

    assertThat(result).isEqualTo(oldOmuContactDetails.emailAddress!!)
  }

  @Test
  fun `should throw error if omu not found`() {
    every { prisonRegisterClient.getOmuContactDetails(eq(oldPrisonCode)) } returns ClientResult.Failure.StatusCode(
      method = HttpMethod.GET,
      path = "/",
      status = HttpStatus.NOT_FOUND,
      body = "the body",
    )

    val exception = assertThrows<IllegalStateException> { emailService.getEmail(oldPrisonCode) }

    assertThat(exception.message).isEqualTo("No OMU contact details found for prison ID $oldPrisonCode.")
  }

  @Test
  fun `should throw error if omu email not found`() {
    every { prisonRegisterClient.getOmuContactDetails(eq(oldPrisonCode)) } returns ClientResult.Success(status = HttpStatus.OK, body = OmuContactDetails(null))

    val exception = assertThrows<IllegalStateException> { emailService.getEmail(oldPrisonCode) }

    assertThat(exception.message).isEqualTo("OMU email address is null for prison ID $oldPrisonCode.")
  }
}
