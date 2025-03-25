package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
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
  private val newPrisonCode = "LON"
  private val oldPrisonCode = "LIV"

  private val prisoner = Prisoner(prisonId = newPrisonCode, prisonName = "HMS LONDON")
  private val nomsNumber = "NOMSABC"
  private val templateId = "SOME ID"
  private val user = NomisUserEntityFactory().produce()

  private val application =
    Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber)
      .withCreatedByUser(user).produce()
  private val pomAllocation = PomAllocation(Manager(user.nomisStaffId), Prison(newPrisonCode))

  private val link = applicationUrlTemplate.replace("#id", application.id.toString())
  private val applicationAssignmentOld = Cas2ApplicationAssignmentEntity(
    id = UUID.randomUUID(),
    application = application,
    prisonCode = oldPrisonCode,
    createdAt = OffsetDateTime.now(),
    allocatedPomUserId = user.id,
  )
  private val applicationAssignmentNew = Cas2ApplicationAssignmentEntity(
    id = UUID.randomUUID(),
    application = application,
    prisonCode = newPrisonCode,
    createdAt = OffsetDateTime.now(),
    allocatedPomUserId = user.id,
  )
  private val oldAgency =
    Agency(agencyId = applicationAssignmentOld.prisonCode, description = "HMS LIVERPOOL", agencyType = "prison")
  private val newAgency =
    Agency(agencyId = applicationAssignmentNew.prisonCode, description = "HMS LONDON", agencyType = "prison")

  @Test
  fun `send allocation changed emails`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    every { notifyConfig.templates.toNacroApplicationTransferredToAnotherPom } returns templateId
    every { notifyConfig.templates.toReceivingPomApplicationTransferredToAnotherPom } returns templateId
    every { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) } returns ClientResult.Success(
      HttpStatus.OK,
      oldAgency,
    )
    every { prisonsApiClient.getAgencyDetails(eq(newAgency.agencyId)) } returns ClientResult.Success(
      HttpStatus.OK,
      newAgency,
    )
    every {
      emailNotificationService.sendEmail(
        eq(user.email!!),
        eq(templateId),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "transferringPrisonName" to oldAgency.description,
            "link" to link,
            "applicationStatus" to "PLACEHOLDER",
          ),
        ),
      )
    } returns Unit
    every {
      emailNotificationService.sendEmail(
        eq("tbc"),
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

    emailService.sendAllocationChangedEmails(user, nomsNumber, application.id, oldAgency.agencyId, pomAllocation.prison.code)

    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) }
    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(newAgency.agencyId)) }
    verify(exactly = 2) { emailNotificationService.sendEmail(any(), any(), any()) }
  }

  @Test
  fun `do not send allocation changed emails as no agency found for old Prison Id`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    every { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/api/agencies/${oldAgency.agencyId}", HttpStatus.NOT_FOUND, null)
    every { emailNotificationService.sendEmail(any(), any(), any()) } returns Unit

    emailService.sendAllocationChangedEmails(user, nomsNumber, application.id, oldAgency.agencyId, pomAllocation.prison.code)

    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) }
    verify(exactly = 0) { emailNotificationService.sendEmail(any(), any(), any()) }
  }

  @Test
  fun `do not send allocation changed emails as no agency found for new Prison Id`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    every { prisonsApiClient.getAgencyDetails(eq(newAgency.agencyId)) } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/api/agencies/${newAgency.agencyId}", HttpStatus.NOT_FOUND, null)
    every { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) } returns ClientResult.Success(
      HttpStatus.OK,
      oldAgency,
    )
    every { emailNotificationService.sendEmail(any(), any(), any()) } returns Unit

    emailService.sendAllocationChangedEmails(user, nomsNumber, application.id, oldAgency.agencyId, pomAllocation.prison.code)

    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) }
    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(newAgency.agencyId)) }
    verify(exactly = 0) { emailNotificationService.sendEmail(any(), any(), any()) }
  }

  @Test
  fun `send location changed emails`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    every { notifyConfig.templates.toTransferringPomApplicationTransferredToAnotherPrison } returns templateId
    every { notifyConfig.templates.toTransferringPomUnitApplicationTransferredToAnotherPrison } returns templateId
    every { notifyConfig.templates.toReceivingPomUnitApplicationTransferredToAnotherPrison } returns templateId
    every { notifyConfig.templates.toNacroApplicationTransferredToAnotherPrison } returns templateId
    every { nomisUserRepository.findById(eq(user.id)) } returns Optional.of(user)
    every { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) } returns ClientResult.Success(
      HttpStatus.OK,
      oldAgency,
    )

    every {
      emailNotificationService.sendEmail(
        eq(user.email!!),
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
      emailNotificationService.sendEmail(
        eq("tbc"),
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
      emailNotificationService.sendEmail(
        eq("tbc"),
        eq(templateId),
        eq(
          mapOf(
            "nomsNumber" to nomsNumber,
            "transferringPrisonName" to oldAgency.description,
            "link" to link,
            "applicationStatus" to "PLACEHOLDER",
          ),
        ),
      )
    } returns Unit
    every {
      emailNotificationService.sendEmail(
        eq("tbc"),
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

    emailService.sendLocationChangedEmails(application.id, user.id, applicationAssignmentOld.prisonCode, nomsNumber, prisoner)

    verify(exactly = 1) { nomisUserRepository.findById(eq(user.id)) }
    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) }
    verify(exactly = 4) { emailNotificationService.sendEmail(any(), any(), any()) }
  }

  @Test
  fun `do not send location changed emails as no nomis user found for old POM`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    every { nomisUserRepository.findById(eq(user.id)) } returns Optional.empty()
    every { emailNotificationService.sendEmail(any(), any(), any()) } returns Unit

    emailService.sendLocationChangedEmails(application.id, user.id, applicationAssignmentOld.prisonCode, nomsNumber, prisoner)

    verify(exactly = 1) { nomisUserRepository.findById(eq(user.id)) }
    verify(exactly = 0) { emailNotificationService.sendEmail(any(), any(), any()) }
  }

  @Test
  fun `do not send location changed emails as no agency found for old Prison Id`() {
    application.applicationAssignments.add(applicationAssignmentNew)
    application.applicationAssignments.add(applicationAssignmentOld)
    every { nomisUserRepository.findById(eq(user.id)) } returns Optional.of(user)
    every { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/api/agencies/${oldAgency.agencyId}", HttpStatus.NOT_FOUND, null)
    every { emailNotificationService.sendEmail(any(), any(), any()) } returns Unit

    emailService.sendLocationChangedEmails(application.id, user.id, applicationAssignmentOld.prisonCode, nomsNumber, prisoner)

    verify(exactly = 1) { nomisUserRepository.findById(eq(user.id)) }
    verify(exactly = 1) { prisonsApiClient.getAgencyDetails(eq(oldAgency.agencyId)) }
    verify(exactly = 0) { emailNotificationService.sendEmail(any(), any(), any()) }
  }
}
