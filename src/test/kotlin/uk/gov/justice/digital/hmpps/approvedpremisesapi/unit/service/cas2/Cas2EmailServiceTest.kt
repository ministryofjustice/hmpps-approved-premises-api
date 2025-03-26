package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Manager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prison
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
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
  private val statusUpdateRepository = mockk<Cas2StatusUpdateRepository>()
  private val offenderManagementUnitRepository = mockk<OffenderManagementUnitRepository>()
  private val applicationUrlTemplate = UrlTemplate("/applications/#id/overview").toString()
  private val nacroEmail = "nacro@test.co.uk"

  private val emailService = Cas2EmailService(
    emailNotificationService,
    notifyConfig,
    nomisUserRepository,
    prisonsApiClient,
    statusUpdateRepository,
    offenderManagementUnitRepository,
    applicationUrlTemplate,
    nacroEmail,
  )
  private val newPrisonCode = "LON"
  private val oldPrisonCode = "LIV"

  private val prisoner = Prisoner(prisonId = newPrisonCode, prisonName = "HMS LONDON")
  private val nomsNumber = "NOMSABC"
  private val templateId = "SOME ID"
  private val oldUser = NomisUserEntityFactory().produce()
  private val newUser = NomisUserEntityFactory().produce()

  private val application =
    Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber)
      .withCreatedByUser(oldUser).produce()
  private val pomAllocation = PomAllocation(Manager(newUser.nomisStaffId), Prison(newPrisonCode))

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
  private val oldAgency =
    Agency(agencyId = oldPrisonCode, description = "HMS LIVERPOOL", agencyType = "prison")
  private val newAgency =
    Agency(agencyId = newPrisonCode, description = prisoner.prisonName, agencyType = "prison")

  private val oldOmuEmail = OffenderManagementUnitEntity(UUID.randomUUID(), oldPrisonCode, "old@digital.justice.gov")
  private val newOmuEmail = OffenderManagementUnitEntity(UUID.randomUUID(), newPrisonCode, "new@digital.justice.gov")

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
    every { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) } returns ClientResult.Success(
      HttpStatus.OK,
      oldAgency,
    )
    every { prisonsApiClient.getAgencyDetails(eq(newAgency.agencyId)) } returns ClientResult.Success(
      HttpStatus.OK,
      newAgency,
    )
    every {
      emailNotificationService.sendCas2Email(
        eq(newUser.email!!),
        eq(templateId),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "transferringPrisonName" to oldAgency.description,
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
            "receivingPrisonName" to newAgency.description,
            "link" to link,
          ),
        ),
      )
    } returns Unit

    emailService.sendAllocationChangedEmails(newUser, nomsNumber, application, pomAllocation.prison.code)

    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) }
    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(newAgency.agencyId)) }
    verify(exactly = 1) { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) }
    verify(exactly = 2) { emailNotificationService.sendCas2Email(any(), any(), any()) }
  }

  @Test
  fun `do not send allocation changed emails as no matching status update found and throw error`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) } returns null
    every { emailNotificationService.sendCas2Email(any(), any(), any()) } returns Unit
    every { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) } returns ClientResult.Success(
      HttpStatus.OK,
      oldAgency,
    )
    every { prisonsApiClient.getAgencyDetails(eq(newAgency.agencyId)) } returns ClientResult.Success(
      HttpStatus.OK,
      newAgency,
    )

    assertThrows<EntityNotFoundException> { emailService.sendAllocationChangedEmails(newUser, nomsNumber, application, pomAllocation.prison.code) }

    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) }
    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(newAgency.agencyId)) }
    verify(exactly = 1) { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) }
    verify(exactly = 0) { emailNotificationService.sendCas2Email(any(), any(), any()) }
  }

  @Test
  fun `do not send allocation changed emails as no agency found for old Prison Id`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/api/agencies/${oldAgency.agencyId}", HttpStatus.NOT_FOUND, null)
    every { emailNotificationService.sendCas2Email(any(), any(), any()) } returns Unit

    emailService.sendAllocationChangedEmails(newUser, nomsNumber, application, pomAllocation.prison.code)

    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) }
    verify(exactly = 0) { emailNotificationService.sendCas2Email(any(), any(), any()) }
  }

  @Test
  fun `do not send allocation changed emails as no agency found for new Prison Id`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)

    every { prisonsApiClient.getAgencyDetails(eq(newAgency.agencyId)) } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/api/agencies/${newAgency.agencyId}", HttpStatus.NOT_FOUND, null)
    every { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) } returns ClientResult.Success(
      HttpStatus.OK,
      oldAgency,
    )
    every { emailNotificationService.sendCas2Email(any(), any(), any()) } returns Unit

    emailService.sendAllocationChangedEmails(newUser, nomsNumber, application, pomAllocation.prison.code)

    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) }
    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(newAgency.agencyId)) }
    verify(exactly = 0) { emailNotificationService.sendCas2Email(any(), any(), any()) }
  }

  @Test
  fun `send location changed emails`() {
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)
    every { offenderManagementUnitRepository.findByPrisonCode(newPrisonCode) } returns newOmuEmail
    every { offenderManagementUnitRepository.findByPrisonCode(oldPrisonCode) } returns oldOmuEmail
    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) } returns cas2StatusUpdateEntity
    every { notifyConfig.templates.cas2ToTransferringPomApplicationTransferredToAnotherPrison } returns templateId
    every { notifyConfig.templates.cas2ToTransferringPomUnitApplicationTransferredToAnotherPrison } returns templateId
    every { notifyConfig.templates.cas2ToReceivingPomUnitApplicationTransferredToAnotherPrison } returns templateId
    every { notifyConfig.templates.cas2ToNacroApplicationTransferredToAnotherPrison } returns templateId
    every { nomisUserRepository.findById(eq(oldUser.id)) } returns Optional.of(oldUser)
    every { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) } returns ClientResult.Success(
      HttpStatus.OK,
      oldAgency,
    )

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
        eq(oldOmuEmail.email),
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
        eq(newOmuEmail.email),
        eq(templateId),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "transferringPrisonName" to oldAgency.description,
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
            "transferringPrisonName" to oldAgency.description,
            "link" to link,
          ),
        ),
      )
    } returns Unit

    emailService.sendLocationChangedEmails(application, oldUser.id, nomsNumber, prisoner)

    verify(exactly = 1) { nomisUserRepository.findById(eq(oldUser.id)) }
    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) }
    verify(exactly = 4) { emailNotificationService.sendCas2Email(any(), any(), any()) }
    verify(exactly = 1) { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) }
    verify(exactly = 2) { offenderManagementUnitRepository.findByPrisonCode(any()) }
  }

  @Test
  fun `do not send location changed emails as no matching status update found and throw error`() {
    application.applicationAssignments.add(applicationAssignmentOld)
    application.applicationAssignments.add(applicationAssignmentOlder)
    every { offenderManagementUnitRepository.findByPrisonCode(newPrisonCode) } returns newOmuEmail
    every { offenderManagementUnitRepository.findByPrisonCode(oldPrisonCode) } returns oldOmuEmail
    every { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) } returns null
    every { nomisUserRepository.findById(eq(oldUser.id)) } returns Optional.of(oldUser)
    every { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) } returns ClientResult.Success(
      HttpStatus.OK,
      oldAgency,
    )
    every { emailNotificationService.sendCas2Email(any(), any(), any()) } returns Unit

    assertThrows<EntityNotFoundException> { emailService.sendLocationChangedEmails(application, oldUser.id, nomsNumber, prisoner) }

    verify(exactly = 1) { nomisUserRepository.findById(eq(oldUser.id)) }
    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) }
    verify(exactly = 0) { emailNotificationService.sendCas2Email(any(), any(), any()) }
    verify(exactly = 1) { statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) }
    verify(exactly = 2) { offenderManagementUnitRepository.findByPrisonCode(any()) }
  }

  @Test
  fun `do not send location changed emails as no nomis user found for old POM and throw error`() {
    application.applicationAssignments.add(applicationAssignmentOlder)
    application.applicationAssignments.add(applicationAssignmentOld)

    every { nomisUserRepository.findById(eq(oldUser.id)) } returns Optional.empty()
    every { emailNotificationService.sendCas2Email(any(), any(), any()) } returns Unit

    assertThrows<NoSuchElementException> { emailService.sendLocationChangedEmails(application, oldUser.id, nomsNumber, prisoner) }

    verify(exactly = 1) { nomisUserRepository.findById(eq(oldUser.id)) }
    verify(exactly = 0) { emailNotificationService.sendCas2Email(any(), any(), any()) }
  }

  @Test
  fun `do not send location changed emails as no agency found for old Prison Id`() {
    application.applicationAssignments.add(applicationAssignmentOlder)
    application.applicationAssignments.add(applicationAssignmentOld)

    every { nomisUserRepository.findById(eq(oldUser.id)) } returns Optional.of(oldUser)
    every { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/api/agencies/${oldAgency.agencyId}", HttpStatus.NOT_FOUND, null)
    every { emailNotificationService.sendCas2Email(any(), any(), any()) } returns Unit

    emailService.sendLocationChangedEmails(application, oldUser.id, nomsNumber, prisoner)

    verify(exactly = 1) { nomisUserRepository.findById(eq(oldUser.id)) }
    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) }
    verify(exactly = 0) { emailNotificationService.sendCas2Email(any(), any(), any()) }
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
