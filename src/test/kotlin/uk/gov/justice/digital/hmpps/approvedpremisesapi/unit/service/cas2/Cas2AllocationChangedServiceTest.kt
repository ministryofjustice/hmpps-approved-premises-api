package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManagePomCasesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Manager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prison
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InvalidDomainEventException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2AllocationChangedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2EmailService
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas2AllocationChangedServiceTest {

  @MockK
  lateinit var applicationRepository: Cas2ApplicationRepository

  @MockK
  lateinit var applicationService: Cas2ApplicationService

  @MockK
  lateinit var managePomCasesClient: ManagePomCasesClient

  @MockK
  lateinit var nomisUserRepository: NomisUserRepository

  @MockK
  lateinit var cas2EmailService: Cas2EmailService

  @InjectMockKs
  lateinit var allocationChangedService: Cas2AllocationChangedService

  private val pomAllocation = PomAllocation(Manager(1234), Prison("NEW"))
  private val eventType = "offender-management.allocation.changed"
  private val nomsNumber = "NOMSABC"
  private val detailUrl = "some/url"

  private val user = NomisUserEntityFactory().produce()

  private val occurredAt = Instant.now().atZone(ZoneId.systemDefault())

  private val allocationEvent = HmppsDomainEvent(
    eventType = eventType,
    version = 1,
    detailUrl = detailUrl,
    occurredAt = occurredAt,
    description = "anything",
    additionalInformation = AdditionalInformation(),
    personReference = PersonReference(listOf(PersonIdentifier("NOMS", nomsNumber))),
  )

  @Test
  fun `handle Allocation Changed Event and save new allocation to table`() {
    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withCreatedByUser(user).produce()
    application.createApplicationAssignment(prisonCode = "CODE123", allocatedPomUserId = UUID.randomUUID())

    every { managePomCasesClient.getPomAllocation(any()) } returns ClientResult.Success(HttpStatus.OK, pomAllocation)
    every { applicationService.findMostRecentApplication(eq(nomsNumber)) } returns application
    every { applicationRepository.save(any()) } answers { it.invocation.args[0] as Cas2ApplicationEntity }
    every { nomisUserRepository.findByNomisStaffId(eq(pomAllocation.manager.code)) } returns user
    every { cas2EmailService.sendAllocationChangedEmailToNacro(any(), eq(nomsNumber)) } returns Unit
    every { cas2EmailService.sendAllocationChangedEmailToReceivingPom(any(), eq(nomsNumber)) } returns Unit

    allocationChangedService.process(allocationEvent)

    verify(exactly = 1) { managePomCasesClient.getPomAllocation(any()) }
    verify(exactly = 1) { applicationService.findMostRecentApplication(eq(nomsNumber)) }
    verify(exactly = 1) { nomisUserRepository.findByNomisStaffId(eq(pomAllocation.manager.code)) }
    verify(exactly = 1) { mockApplicationRepository.save(any()) }
    verify(exactly = 1) { cas2EmailService.sendAllocationChangedEmailToNacro(any(), eq(nomsNumber)) }
    verify(exactly = 1) { cas2EmailService.sendAllocationChangedEmailToReceivingPom(any(), eq(nomsNumber)) }
    verify(exactly = 1) { applicationRepository.save(any()) }
  }

  @Test
  fun `handle Allocation Changed Event and throw error when no nomis user is found`() {
    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withCreatedByUser(user).produce()

    every { managePomCasesClient.getPomAllocation(any()) } returns ClientResult.Success(HttpStatus.OK, pomAllocation)
    every { applicationService.findMostRecentApplication(eq(nomsNumber)) } returns application
    every { nomisUserRepository.findByNomisStaffId(eq(pomAllocation.manager.code)) } returns null

    assertThrows<RuntimeException> { allocationChangedService.process(allocationEvent) }
  }

  @Test
  fun `handle Allocation Changed Event and throw error when pomAllocation not found from event detailUrl`() {
    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withCreatedByUser(user).produce()

    every { managePomCasesClient.getPomAllocation(any()) } returns ClientResult.Failure.StatusCode(
      HttpMethod.GET,
      "/",
      HttpStatus.NOT_FOUND,
      body = null,
    )

    every { applicationService.findMostRecentApplication(eq(nomsNumber)) } returns application

    allocationChangedService.process(allocationEvent)

    verify { managePomCasesClient.getPomAllocation(any()) }
    verify { applicationService.findMostRecentApplication(eq(nomsNumber)) }
    verify(exactly = 0) { applicationRepository.save(any()) }
  }

  @Test
  fun `handle Allocation Changed Event and throw error when there is no detailUrl in the event`() {
    val eventWithNoDetailUrl = HmppsDomainEvent(
      eventType = eventType,
      version = 1,
      detailUrl = null,
      occurredAt = occurredAt,
      description = "anything",
      additionalInformation = AdditionalInformation(),
      personReference = PersonReference(listOf(PersonIdentifier("NOMS", nomsNumber))),
    )
    assertThrows<InvalidDomainEventException> { allocationChangedService.process(eventWithNoDetailUrl) }
  }

  @Test
  fun `handle Allocation Changed Event and throw error when there is no nomsNumber in the event`() {
    val eventWithNoNomsNumber = HmppsDomainEvent(
      eventType = eventType,
      version = 1,
      detailUrl = detailUrl,
      occurredAt = occurredAt,
      description = "anything",
      additionalInformation = AdditionalInformation(),
      personReference = PersonReference(listOf()),
    )
    assertThrows<InvalidDomainEventException> { allocationChangedService.process(eventWithNoNomsNumber) }
  }

  @Test
  fun `handle Allocation Changed Event and do nothing if there is no application associated with the event`() {
    every { managePomCasesClient.getPomAllocation(any()) } returns ClientResult.Success(HttpStatus.OK, pomAllocation)
    every { applicationService.findMostRecentApplication(eq(nomsNumber)) } returns null

    allocationChangedService.process(allocationEvent)

    verify(exactly = 0) { managePomCasesClient.getPomAllocation(any()) }
    verify(exactly = 1) { applicationService.findMostRecentApplication(eq(nomsNumber)) }
  }

  @Test
  fun `application assignment is not created when POM has not changed`() {
    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withCreatedByUser(user).produce()
    application.createApplicationAssignment(prisonCode = "CODE123", allocatedPomUserId = user.id)

    every { managePomCasesClient.getPomAllocation(any()) } returns ClientResult.Success(
      HttpStatus.OK,
      pomAllocation.copy(manager = Manager(user.nomisStaffId)),
    )
    every { applicationService.findMostRecentApplication(eq(nomsNumber)) } returns application
    every { nomisUserRepository.findByNomisStaffId(any()) } returns user

    allocationChangedService.process(allocationEvent)

    verify { managePomCasesClient.getPomAllocation(any()) }
    verify(exactly = 0) { applicationRepository.save(any()) }
  }
}
