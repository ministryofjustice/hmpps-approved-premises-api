package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
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
  private val offenderManagementUnitRepository = mockk<OffenderManagementUnitRepository>()
  private val applicationUrlTemplate = UrlTemplate("/applications/#id/overview").toString()
  private val nacroEmail = "nacro@test.co.uk"

  private val emailService = Cas2EmailService(
    emailNotificationService,
    notifyConfig,
    nomisUserRepository,
    statusUpdateRepository,
    offenderManagementUnitRepository,
    applicationUrlTemplate,
    nacroEmail,
  )

  private val newPrisonCode = "LON"
  private val oldPrisonCode = "LIV"
  private val oldOmu = OffenderManagementUnitEntity(UUID.randomUUID(), oldPrisonCode, "HMS LIVERPOOL", "old@digital.justice.gov")
  private val newOmu = OffenderManagementUnitEntity(UUID.randomUUID(), newPrisonCode, "HMS LONDON", "new@digital.justice.gov")
  private val nomsNumber = "NOMSABC"
  private val templateId = "SOME ID"
  private val oldUser = NomisUserEntityFactory().produce()
  private val newUser = NomisUserEntityFactory().produce()

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
    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldPrisonCode)) } returns oldOmu
    every { offenderManagementUnitRepository.findByPrisonCode(eq(newPrisonCode)) } returns newOmu
    every {
      emailNotificationService.sendCas2Email(
        eq(newUser.email!!),
        eq(templateId),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "transferringPrisonName" to oldOmu.prisonName,
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
            "receivingPrisonName" to newOmu.prisonName,
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

    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldPrisonCode)) } returns oldOmu
    every { offenderManagementUnitRepository.findByPrisonCode(eq(newPrisonCode)) } returns newOmu
    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) } returns null

    val exception = assertThrows<IllegalStateException> { emailService.sendAllocationChangedEmails(application, newUser, newPrisonCode) }
    assertThat(exception.message).isEqualTo("StatusUpdate for ${application.id} not found")
  }

  @Test
  fun `do not send allocation changed emails and throw error as new omu not found`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldPrisonCode)) } returns oldOmu
    every { offenderManagementUnitRepository.findByPrisonCode(eq(newPrisonCode)) } returns null

    val exception = assertThrows<IllegalStateException> { emailService.sendAllocationChangedEmails(application, newUser, newPrisonCode) }
    assertThat(exception.message).isEqualTo("No OMU found for new prison code $newPrisonCode.")
  }

  @Test
  fun `do not send allocation changed emails and throw error as old omu not found`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldPrisonCode)) } returns null

    val exception = assertThrows<IllegalStateException> { emailService.sendAllocationChangedEmails(application, newUser, newPrisonCode) }
    assertThat(exception.message).isEqualTo("No OMU found for old prison code $oldPrisonCode.")
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
    every { offenderManagementUnitRepository.findByPrisonCode(newPrisonCode) } returns newOmu
    every { offenderManagementUnitRepository.findByPrisonCode(oldPrisonCode) } returns oldOmu
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
            "receivingPrisonName" to newOmu.prisonName,
          ),
        ),
      )
    } returns Unit
    every {
      emailNotificationService.sendCas2Email(
        eq(oldOmu.email),
        eq(templateId),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to newOmu.prisonName,
          ),
        ),
      )
    } returns Unit
    every {
      emailNotificationService.sendCas2Email(
        eq(newOmu.email),
        eq(templateId),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "transferringPrisonName" to oldOmu.prisonName,
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
            "receivingPrisonName" to newOmu.prisonName,
            "transferringPrisonName" to oldOmu.prisonName,
            "link" to link,
          ),
        ),
      )
    } returns Unit

    emailService.sendLocationChangedEmails(application, oldUser.id, newPrisonCode)

    verify(exactly = 4) { emailNotificationService.sendCas2Email(any(), any(), any()) }
  }

  @Test
  fun `do not send location changed emails as old prison code not found`() {
    application.applicationAssignments.add(applicationAssignmentOld)

    val exception = assertThrows<IllegalStateException> { emailService.sendLocationChangedEmails(application, oldUser.id, newPrisonCode) }
    assertThat(exception.message).isEqualTo("Old prison code not found.")
  }

  @Test
  fun `do not send location changed emails and throw error as no application status found`() {
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldPrisonCode)) } returns oldOmu
    every { offenderManagementUnitRepository.findByPrisonCode(eq(newPrisonCode)) } returns newOmu
    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) } returns null
    every { nomisUserRepository.findById(eq(oldUser.id)) } returns Optional.of(oldUser)

    val exception = assertThrows<IllegalStateException> { emailService.sendLocationChangedEmails(application, oldUser.id, newPrisonCode) }
    assertThat(exception.message).isEqualTo("StatusUpdate for ${application.id} not found")
  }

  @Test
  fun `do not send location changed emails and throw error as new omu not found`() {
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldPrisonCode)) } returns oldOmu
    every { offenderManagementUnitRepository.findByPrisonCode(eq(newPrisonCode)) } returns null
    every { nomisUserRepository.findById(eq(oldUser.id)) } returns Optional.of(oldUser)

    val exception = assertThrows<IllegalStateException> { emailService.sendLocationChangedEmails(application, oldUser.id, newPrisonCode) }
    assertThat(exception.message).isEqualTo("No OMU found for new prison code $newPrisonCode.")
  }

  @Test
  fun `do not send location changed emails and throw error as old omu not found`() {
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { nomisUserRepository.findById(eq(oldUser.id)) } returns Optional.of(oldUser)
    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldPrisonCode)) } returns null

    val exception = assertThrows<IllegalStateException> { emailService.sendLocationChangedEmails(application, oldUser.id, newPrisonCode) }
    assertThat(exception.message).isEqualTo("No OMU found for old prison code $oldPrisonCode.")
  }

  @Test
  fun `do not send location changed emails as nomis user not found`() {
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { nomisUserRepository.findById(eq(oldUser.id)) } returns Optional.empty()

    assertThrows<NoSuchElementException> { emailService.sendLocationChangedEmails(application, oldUser.id, newPrisonCode) }
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
}
