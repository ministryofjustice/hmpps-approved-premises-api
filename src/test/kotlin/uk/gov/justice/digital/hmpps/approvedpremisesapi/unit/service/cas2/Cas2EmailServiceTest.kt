package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderManagementUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2EmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas2EmailServiceTest {

  private val emailNotificationService = mockk<EmailNotificationService>()
  private val nomisUserRepository = mockk<NomisUserRepository>()
  private val statusUpdateRepository = mockk<Cas2StatusUpdateRepository>()
  private val offenderManagementUnitRepository = mockk<OffenderManagementUnitRepository>()
  private val applicationUrlTemplate = UrlTemplate("/applications/#id/overview").toString()
  private val submittedApplicationUrlTemplate = UrlTemplate("/assess/applications/#applicationId/overview").toString()
  private val nacroEmail = "nacro@test.co.uk"

  private val emailService = Cas2EmailService(
    emailNotificationService,
    nomisUserRepository,
    statusUpdateRepository,
    offenderManagementUnitRepository,
    applicationUrlTemplate,
    submittedApplicationUrlTemplate,
    nacroEmail,
  )
  private val oldUser = NomisUserEntityFactory().produce()
  private val newUser = NomisUserEntityFactory().produce()
  private val newerUser = NomisUserEntityFactory().produce()
  private val oldOmu = OffenderManagementUnitEntityFactory()
    .withPrisonCode("LIV")
    .withPrisonName("HMP LIVERPOOL")
    .withEmail("old@digital.justice.gov")
    .produce()
  private val newOmu = OffenderManagementUnitEntityFactory()
    .withPrisonCode("LON")
    .withPrisonName("HMP LONDON")
    .withEmail("new@digital.justice.gov")
    .produce()

  private val prisoner = Prisoner(newOmu.prisonCode, newOmu.prisonName)
  private val nomsNumber = "NOMSABC"

  private val application =
    Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber)
      .withReferringPrisonCode("PRI")
      .withCreatedByUser(oldUser).produce()

  private val assessorLink = submittedApplicationUrlTemplate.replace("#applicationId", application.id.toString())
  private val link = applicationUrlTemplate.replace("#id", application.id.toString())
  private val applicationAssignmentOlder = Cas2ApplicationAssignmentEntity(
    id = UUID.randomUUID(),
    application = application,
    prisonCode = oldOmu.prisonCode,
    createdAt = OffsetDateTime.now().minusDays(2),
    allocatedPomUser = oldUser,
  )
  private val applicationAssignmentOld = Cas2ApplicationAssignmentEntity(
    id = UUID.randomUUID(),
    application = application,
    prisonCode = newOmu.prisonCode,
    createdAt = OffsetDateTime.now().minusDays(1),
    allocatedPomUser = null,
  )
  private val applicationAssignmentNew = Cas2ApplicationAssignmentEntity(
    id = UUID.randomUUID(),
    application = application,
    prisonCode = newOmu.prisonCode,
    createdAt = OffsetDateTime.now(),
    allocatedPomUser = newUser,
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
    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldOmu.prisonCode)) } returns oldOmu
    every { offenderManagementUnitRepository.findByPrisonCode(eq(newOmu.prisonCode)) } returns newOmu
    every {
      emailNotificationService.sendCas2Email(
        eq(newUser.email!!),
        eq(Cas2NotifyTemplates.cas2ToReceivingPomApplicationTransferredToAnotherPom),
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
        eq(Cas2NotifyTemplates.cas2ToNacroApplicationTransferredToAnotherPom),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to newOmu.prisonName,
            "link" to assessorLink,
          ),
        ),
      )
    } returns Unit

    emailService.sendAllocationChangedEmails(application, newUser, newOmu.prisonCode)

    verify(exactly = 2) { emailNotificationService.sendCas2Email(any(), any(), any()) }
  }

  @Test
  fun `do not send allocation changed emails and throw error as no application status found`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldOmu.prisonCode)) } returns oldOmu
    every { offenderManagementUnitRepository.findByPrisonCode(eq(newOmu.prisonCode)) } returns newOmu
    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) } returns null

    val exception = assertThrows<IllegalStateException> { emailService.sendAllocationChangedEmails(application, newUser, newOmu.prisonCode) }
    assertThat(exception.message).isEqualTo("StatusUpdate for ${application.id} not found")
  }

  @Test
  fun `do not send allocation changed emails and throw error as no new OMU found`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldOmu.prisonCode)) } returns oldOmu
    every { offenderManagementUnitRepository.findByPrisonCode(eq(newOmu.prisonCode)) } returns null

    val exception = assertThrows<IllegalStateException> { emailService.sendAllocationChangedEmails(application, newUser, newOmu.prisonCode) }
    assertThat(exception.message).isEqualTo("No OMU found for new prison code ${newOmu.prisonCode}.")
  }

  @Test
  fun `do not send allocation changed emails and throw error as no old OMU found`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldOmu.prisonCode)) } returns null

    val exception = assertThrows<IllegalStateException> { emailService.sendAllocationChangedEmails(application, newUser, newOmu.prisonCode) }
    assertThat(exception.message).isEqualTo("No OMU found for old prison code ${oldOmu.prisonCode}.")
  }

  @Test
  fun `do not send allocation changed emails and throw error as old prison code not found`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)

    val exception = assertThrows<IllegalStateException> { emailService.sendAllocationChangedEmails(application, newUser, newOmu.prisonCode) }
    assertThat(exception.message).isEqualTo("Old prison code not found.")
  }

  @Test
  fun `send location changed emails`() {
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)
    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldOmu.prisonCode)) } returns oldOmu
    every { offenderManagementUnitRepository.findByPrisonCode(eq(newOmu.prisonCode)) } returns newOmu
    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) } returns cas2StatusUpdateEntity
    every { nomisUserRepository.findById(eq(oldUser.id)) } returns Optional.of(oldUser)

    every {
      emailNotificationService.sendCas2Email(
        eq(oldUser.email!!),
        eq(Cas2NotifyTemplates.cas2ToTransferringPomApplicationTransferredToAnotherPrison),
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
        eq(oldOmu.email),
        eq(Cas2NotifyTemplates.cas2ToTransferringPomUnitApplicationTransferredToAnotherPrison),
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
        eq(newOmu.email),
        eq(Cas2NotifyTemplates.cas2ToReceivingPomUnitApplicationTransferredToAnotherPrison),
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
        eq(Cas2NotifyTemplates.cas2ToNacroApplicationTransferredToAnotherPrison),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to prisoner.prisonName,
            "transferringPrisonName" to oldOmu.prisonName,
            "link" to assessorLink,
          ),
        ),
      )
    } returns Unit

    emailService.sendLocationChangedEmails(application, prisonCode = prisoner.prisonId)

    verify(exactly = 4) { emailNotificationService.sendCas2Email(any(), any(), any()) }
  }

  @Test
  fun `do not send location changed emails as old prison code not found`() {
    application.createApplicationAssignment(application.referringPrisonCode!!, null)

    val exception = assertThrows<IllegalStateException> { emailService.sendLocationChangedEmails(application, prisonCode = prisoner.prisonId) }
    assertThat(exception.message).isEqualTo("Old POM user ID not found.")
  }

  @Test
  fun `do not send location changed emails as old POM not found`() {
    application.applicationAssignments.add(applicationAssignmentOld)

    val exception = assertThrows<IllegalStateException> { emailService.sendLocationChangedEmails(application, prisonCode = prisoner.prisonId) }
    assertThat(exception.message).isEqualTo("Old prison code not found.")
  }

  @Test
  fun `do not send location changed emails and throw error as no new omu found`() {
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldOmu.prisonCode)) } returns oldOmu
    every { offenderManagementUnitRepository.findByPrisonCode(eq(newOmu.prisonCode)) } returns null
    every { nomisUserRepository.findById(eq(oldUser.id)) } returns Optional.of(oldUser)

    val exception = assertThrows<IllegalStateException> { emailService.sendLocationChangedEmails(application, prisonCode = prisoner.prisonId) }
    assertThat(exception.message).isEqualTo("No OMU found for new prison code ${newOmu.prisonCode}.")
  }

  @Test
  fun `do not send location changed emails and throw error as no old omu found`() {
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldOmu.prisonCode)) } returns null
    every { nomisUserRepository.findById(eq(oldUser.id)) } returns Optional.of(oldUser)

    val exception = assertThrows<IllegalStateException> { emailService.sendLocationChangedEmails(application, prisonCode = prisoner.prisonId) }
    assertThat(exception.message).isEqualTo("No OMU found for old prison code ${oldOmu.prisonCode}.")
  }

  @Test
  fun `application status defaults to Submitted when no application status found`() {
    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) } returns null
    val result = emailService.getApplicationStatusOrDefault(application.id)
    assertThat(result).isEqualTo("Received")
  }

  @Test
  fun `do not send location changed emails as nomis user not found`() {
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { nomisUserRepository.findById(eq(oldUser.id)) } returns Optional.empty()

    assertThrows<NoSuchElementException> { emailService.sendLocationChangedEmails(application, prisonCode = prisoner.prisonId) }
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
  fun `getOldPomUserId should get created by ID`() {
    application.createApplicationAssignment(application.referringPrisonCode!!, application.createdByUser)
    application.createApplicationAssignment(prisoner.prisonId, null)
    application.applicationAssignments.sortByDescending { it.createdAt }

    val result = emailService.getOldPomUserId(application, prisoner.prisonId)

    assertThat(result).isEqualTo(application.createdByUser.id)
  }

  @Test
  fun `should get old pom user id`() {
    application.createApplicationAssignment(application.referringPrisonCode!!, application.createdByUser)
    application.createApplicationAssignment(oldOmu.prisonCode, null)
    application.createApplicationAssignment(oldOmu.prisonCode, newUser)
    application.createApplicationAssignment(prisoner.prisonId, null)
    application.applicationAssignments.sortByDescending { it.createdAt }

    val result = emailService.getOldPomUserId(application, prisoner.prisonId)

    assertThat(result).isEqualTo(newUser.id)
  }

  @Test
  fun `should get old pom user id and miss earlier allocation event`() {
    application.createApplicationAssignment(application.referringPrisonCode!!, application.createdByUser)
    application.createApplicationAssignment(oldOmu.prisonCode, null)
    application.createApplicationAssignment(oldOmu.prisonCode, newUser)
    application.createApplicationAssignment(prisoner.prisonId, newerUser)
    application.createApplicationAssignment(prisoner.prisonId, null)
    application.applicationAssignments.sortByDescending { it.createdAt }

    val result = emailService.getOldPomUserId(application, prisoner.prisonId)

    assertThat(result).isEqualTo(newUser.id)
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
