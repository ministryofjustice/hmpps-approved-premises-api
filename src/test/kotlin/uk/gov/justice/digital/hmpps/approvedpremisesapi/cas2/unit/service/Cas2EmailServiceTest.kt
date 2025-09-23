package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2EmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderManagementUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas2EmailServiceTest {

  private val emailNotificationService = mockk<EmailNotificationService>()
  private val cas2UserRepository = mockk<Cas2UserRepository>()
  private val statusUpdateRepository = mockk<Cas2StatusUpdateRepository>()
  private val offenderManagementUnitRepository = mockk<OffenderManagementUnitRepository>()
  private val applicationUrlTemplate = UrlTemplate("/applications/#id/overview").toString()
  private val submittedApplicationUrlTemplate = UrlTemplate("/assess/applications/#applicationId/overview").toString()
  private val nacroEmail = "nacro@test.co.uk"

  private val emailService = Cas2EmailService(
    emailNotificationService,
    cas2UserRepository,
    statusUpdateRepository,
    offenderManagementUnitRepository,
    applicationUrlTemplate,
    submittedApplicationUrlTemplate,
    nacroEmail,
  )
  private val oldUser = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce()
  private val newUser = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce()
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
        eq(Cas2NotifyTemplates.CAS2_TO_RECEIVING_POM_APPLICATION_TRANSFERRED_TO_ANOTHER_POM),
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
        eq(Cas2NotifyTemplates.CAS2_TO_NACRO_APPLICATION_TRANSFERRED_TO_ANOTHER_POM),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to newOmu.prisonName,
            "link" to assessorLink,
          ),
        ),
      )
    } returns Unit

    emailService.sendAllocationChangedEmails(application, newUser.email!!, newOmu.prisonCode)

    verify(exactly = 2) { emailNotificationService.sendCas2Email(any(), any(), any()) }
  }

  @Test
  fun `send allocation changed emails with default status`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) } returns null
    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldOmu.prisonCode)) } returns oldOmu
    every { offenderManagementUnitRepository.findByPrisonCode(eq(newOmu.prisonCode)) } returns newOmu
    every {
      emailNotificationService.sendCas2Email(
        eq(newUser.email!!),
        eq(Cas2NotifyTemplates.CAS2_TO_RECEIVING_POM_APPLICATION_TRANSFERRED_TO_ANOTHER_POM),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "transferringPrisonName" to oldOmu.prisonName,
            "link" to link,
            "applicationStatus" to "Received",
          ),
        ),
      )
    } returns Unit
    every {
      emailNotificationService.sendCas2Email(
        eq(nacroEmail),
        eq(Cas2NotifyTemplates.CAS2_TO_NACRO_APPLICATION_TRANSFERRED_TO_ANOTHER_POM),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to newOmu.prisonName,
            "link" to assessorLink,
          ),
        ),
      )
    } returns Unit

    emailService.sendAllocationChangedEmails(application, newUser.email!!, newOmu.prisonCode)

    verify(exactly = 2) { emailNotificationService.sendCas2Email(any(), any(), any()) }
  }

  @Test
  fun `do not send allocation changed emails and throw error as no new OMU found`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldOmu.prisonCode)) } returns oldOmu
    every { offenderManagementUnitRepository.findByPrisonCode(eq(newOmu.prisonCode)) } returns null

    val exception = assertThrows<IllegalStateException> { emailService.sendAllocationChangedEmails(application, newUser.email!!, newOmu.prisonCode) }
    assertThat(exception.message).isEqualTo("No OMU found for new prison code ${newOmu.prisonCode}.")
  }

  @Test
  fun `do not send allocation changed emails and throw error as no old OMU found`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldOmu.prisonCode)) } returns null

    val exception = assertThrows<IllegalStateException> { emailService.sendAllocationChangedEmails(application, newUser.email!!, newOmu.prisonCode) }
    assertThat(exception.message).isEqualTo("No OMU found for old prison code ${oldOmu.prisonCode}.")
  }

  @Test
  fun `do not send allocation changed emails and throw error as old prison code not found`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)

    val exception = assertThrows<IllegalStateException> { emailService.sendAllocationChangedEmails(application, newUser.email!!, newOmu.prisonCode) }
    assertThat(exception.message).isEqualTo("Old prison code not found.")
  }

  @Test
  fun `send location changed emails`() {
    application.createApplicationAssignment(oldOmu.prisonCode, oldUser)
    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldOmu.prisonCode)) } returns oldOmu
    every { offenderManagementUnitRepository.findByPrisonCode(eq(newOmu.prisonCode)) } returns newOmu
    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) } returns cas2StatusUpdateEntity
    every { cas2UserRepository.findByIdAndUserType(eq(oldUser.id)) } returns oldUser

    every {
      emailNotificationService.sendCas2Email(
        eq(oldUser.email!!),
        eq(Cas2NotifyTemplates.CAS2_TO_TRANSFERRING_POM_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON),
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
        eq(Cas2NotifyTemplates.CAS2_TO_TRANSFERRING_POM_UNIT_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON),
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
        eq(Cas2NotifyTemplates.CAS2_TO_RECEIVING_POM_UNIT_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON),
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
        eq(Cas2NotifyTemplates.CAS2_TO_NACRO_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON),
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

    emailService.sendLocationChangedEmails(application, prisonCode = prisoner.prisonId, transferringFromPomId = oldUser.id)

    verify(exactly = 4) { emailNotificationService.sendCas2Email(any(), any(), any()) }
  }

  @Test
  fun `send location changed emails with default status`() {
    application.createApplicationAssignment(oldOmu.prisonCode, oldUser)
    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldOmu.prisonCode)) } returns oldOmu
    every { offenderManagementUnitRepository.findByPrisonCode(eq(newOmu.prisonCode)) } returns newOmu
    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) } returns null
    every { cas2UserRepository.findByIdAndUserType(eq(oldUser.id)) } returns oldUser

    every {
      emailNotificationService.sendCas2Email(
        eq(oldUser.email!!),
        eq(Cas2NotifyTemplates.CAS2_TO_TRANSFERRING_POM_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON),
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
        eq(Cas2NotifyTemplates.CAS2_TO_TRANSFERRING_POM_UNIT_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON),
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
        eq(Cas2NotifyTemplates.CAS2_TO_RECEIVING_POM_UNIT_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "transferringPrisonName" to oldOmu.prisonName,
            "link" to link,
            "applicationStatus" to "Received",
          ),
        ),
      )
    } returns Unit
    every {
      emailNotificationService.sendCas2Email(
        eq(nacroEmail),
        eq(Cas2NotifyTemplates.CAS2_TO_NACRO_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON),
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

    emailService.sendLocationChangedEmails(application, prisonCode = prisoner.prisonId, transferringFromPomId = oldUser.id)

    verify(exactly = 4) { emailNotificationService.sendCas2Email(any(), any(), any()) }
  }

  @Test
  fun `do not send CAS2_TO_TRANSFERRING_POM_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON email when user id not provided`() {
    every { offenderManagementUnitRepository.findByPrisonCode(any()) } returns oldOmu
    every { emailNotificationService.sendCas2Email(any(), any(), any()) } returns Unit
    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(any()) } returns null
    application.createApplicationAssignment(application.referringPrisonCode!!, null)

    emailService.sendLocationChangedEmails(
      application,
      prisonCode = prisoner.prisonId,
      transferringFromPomId = null,
    )
    verify(exactly = 0) {
      emailNotificationService.sendCas2Email(
        recipientEmailAddress = any(),
        templateId = Cas2NotifyTemplates.CAS2_TO_TRANSFERRING_POM_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON,
        personalisation = any(),
      )
    }

    val shouldBeSentEmails = listOf(
      Cas2NotifyTemplates.CAS2_TO_TRANSFERRING_POM_UNIT_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON,
      Cas2NotifyTemplates.CAS2_TO_NACRO_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON,
      Cas2NotifyTemplates.CAS2_TO_RECEIVING_POM_UNIT_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON,
    )
    shouldBeSentEmails.forEach {
      verify(exactly = 1) {
        emailNotificationService.sendCas2Email(
          recipientEmailAddress = any(),
          templateId = it,
          personalisation = any(),
        )
      }
    }
  }

  @Test
  fun `should send CAS2_TO_TRANSFERRING_POM_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON email when user id is provided`() {
    every { cas2UserRepository.findByIdAndUserType(oldUser.id) } returns oldUser
    every { offenderManagementUnitRepository.findByPrisonCode(any()) } returns oldOmu
    every { emailNotificationService.sendCas2Email(any(), any(), any()) } returns Unit
    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(any()) } returns null
    application.createApplicationAssignment(application.referringPrisonCode!!, null)

    emailService.sendLocationChangedEmails(
      application,
      prisonCode = prisoner.prisonId,
      transferringFromPomId = oldUser.id,
    )

    val shouldBeSentEmails = listOf(
      Cas2NotifyTemplates.CAS2_TO_TRANSFERRING_POM_UNIT_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON,
      Cas2NotifyTemplates.CAS2_TO_NACRO_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON,
      Cas2NotifyTemplates.CAS2_TO_RECEIVING_POM_UNIT_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON,
      Cas2NotifyTemplates.CAS2_TO_TRANSFERRING_POM_APPLICATION_TRANSFERRED_TO_ANOTHER_PRISON,
    )
    shouldBeSentEmails.forEach {
      verify(exactly = 1) {
        emailNotificationService.sendCas2Email(
          recipientEmailAddress = any(),
          templateId = it,
          personalisation = any(),
        )
      }
    }
  }

  @Test
  fun `do not send location changed emails as old POM not found`() {
    application.applicationAssignments.add(applicationAssignmentOld)

    val exception = assertThrows<IllegalStateException> {
      emailService.sendLocationChangedEmails(
        application,
        prisonCode = prisoner.prisonId,
        transferringFromPomId = null,
      )
    }
    assertThat(exception.message).isEqualTo("Old prison code not found.")
  }

  @Test
  fun `do not send location changed emails and throw error as no new omu found`() {
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldOmu.prisonCode)) } returns oldOmu
    every { offenderManagementUnitRepository.findByPrisonCode(eq(newOmu.prisonCode)) } returns null
    every { cas2UserRepository.findByIdAndUserType(eq(oldUser.id)) } returns oldUser

    val exception = assertThrows<IllegalStateException> {
      emailService.sendLocationChangedEmails(
        application,
        prisonCode = prisoner.prisonId,
        transferringFromPomId = null,
      )
    }
    assertThat(exception.message).isEqualTo("No OMU found for new prison code ${newOmu.prisonCode}.")
  }

  @Test
  fun `do not send location changed emails and throw error as no old omu found`() {
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { offenderManagementUnitRepository.findByPrisonCode(eq(oldOmu.prisonCode)) } returns null
    every { cas2UserRepository.findByIdAndUserType(eq(oldUser.id)) } returns oldUser

    val exception = assertThrows<IllegalStateException> {
      emailService.sendLocationChangedEmails(
        application,
        prisonCode = prisoner.prisonId,
        transferringFromPomId = null,
      )
    }
    assertThat(exception.message).isEqualTo("No OMU found for old prison code ${oldOmu.prisonCode}.")
  }

  @Test
  fun `application status defaults to Submitted when no application status found`() {
    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) } returns null
    val result = emailService.getApplicationStatusOrDefault(application.id)
    assertThat(result).isEqualTo("Received")
  }

  @Test
  fun `throws when nomis user not found`() {
    every { offenderManagementUnitRepository.findByPrisonCode(oldOmu.prisonCode) } returns oldOmu
    every { offenderManagementUnitRepository.findByPrisonCode(newOmu.prisonCode) } returns newOmu
    every { cas2UserRepository.findByIdAndUserType(oldUser.id) } returns null

    application.createApplicationAssignment(oldOmu.prisonCode, oldUser)

    val result = assertThrows<IllegalStateException> {
      emailService.sendLocationChangedEmails(
        application,
        prisonCode = prisoner.prisonId,
        transferringFromPomId = oldUser.id,
      )
    }

    assertThat(result.message).isEqualTo("No Cas2 User found for id ${oldUser.id}.")
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

  @Nested
  inner class GetReferrerEmail {
    @Test
    fun `POM email is returned when application is assigned to a POM`() {
      application.createApplicationAssignment("PRI1", oldUser)
      val email = emailService.getReferrerEmail(application)

      assertThat(application.applicationAssignments).hasSize(1)
      assertThat(email).isEqualTo(application.currentAssignment!!.allocatedPomUser!!.email)
    }

    @Test
    fun `OMU email is returned when application has transferred and not assigned to a POM`() {
      application.createApplicationAssignment("PRI1", oldUser)
      application.createApplicationAssignment(newOmu.prisonCode, null)
      every { offenderManagementUnitRepository.findByPrisonCode(newOmu.prisonCode) } returns newOmu
      val email = emailService.getReferrerEmail(application)

      assertThat(application.applicationAssignments).hasSize(2)
      assertThat(email).isEqualTo(newOmu.email)
    }

    @Test
    fun `createdByUser email is returned when the application has not been assigned to a POM`() {
      val email = emailService.getReferrerEmail(application)
      assertThat(application.applicationAssignments).hasSize(0)
      assertThat(email).isEqualTo(application.createdByUser!!.email)
    }
  }
}
