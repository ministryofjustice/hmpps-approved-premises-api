package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import io.sentry.Sentry
import io.sentry.protocol.SentryId
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManagePomCasesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Manager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prison
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.IgnorableMessageException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2AllocationChangedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2ApplicationService
import java.time.Instant
import java.time.ZoneId

class Cas2AllocationChangedServiceTest {

  private val mockApplicationRepository = mockk<Cas2ApplicationRepository>()
  private val mockApplicationService = mockk<Cas2ApplicationService>()
  private val mockManagePomCasesClient = mockk<ManagePomCasesClient>()
  private val mockNomisUserRepository = mockk<NomisUserRepository>()

  private val allocationChangedService: Cas2AllocationChangedService = Cas2AllocationChangedService(
    mockManagePomCasesClient,
    mockApplicationService,
    mockApplicationRepository,
    mockNomisUserRepository,
  )

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

    every { mockManagePomCasesClient.getPomAllocation(any()) } returns pomAllocation
    every { mockApplicationService.findMostRecentApplication(eq(nomsNumber)) } returns application
    every { mockApplicationRepository.save(any()) } returns null
    every { mockNomisUserRepository.findByNomisStaffId(eq(pomAllocation.manager.code)) } returns user

    allocationChangedService.handleAllocationChangedEvent(allocationEvent)

    verify(exactly = 1) { mockManagePomCasesClient.getPomAllocation(any()) }
    verify(exactly = 1) { mockApplicationService.findMostRecentApplication(eq(nomsNumber)) }
    verify(exactly = 1) { mockNomisUserRepository.findByNomisStaffId(eq(pomAllocation.manager.code)) }
    verify(exactly = 1) { mockApplicationRepository.save(any()) }
  }

  @Test
  fun `handle Allocation Changed Event and throw error when no nomis user is found`() {
    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withCreatedByUser(user).produce()

    every { mockManagePomCasesClient.getPomAllocation(any()) } returns pomAllocation
    every { mockApplicationService.findMostRecentApplication(eq(nomsNumber)) } returns application
    every { mockNomisUserRepository.findByNomisStaffId(eq(pomAllocation.manager.code)) } returns null

    val slot = slot<Throwable>()
    mockkStatic(Sentry::class)
    every { Sentry.captureException(capture(slot)) } returns SentryId.EMPTY_ID

    val errorMessage = "No user found for staffCode ${pomAllocation.manager.code}"

    allocationChangedService.handleAllocationChangedEvent(allocationEvent)

    verify(exactly = 1) { mockManagePomCasesClient.getPomAllocation(any()) }
    verify(exactly = 1) { mockApplicationService.findMostRecentApplication(eq(nomsNumber)) }
    verify(exactly = 1) { mockNomisUserRepository.findByNomisStaffId(eq(pomAllocation.manager.code)) }
    verify(exactly = 1) { Sentry.captureException(any()) }

    assertTrue(slot.isCaptured)
    assertTrue(slot.captured is IgnorableMessageException)
    assertTrue(slot.captured.message == errorMessage)
  }

  @Test
  fun `handle Allocation Changed Event and throw error when pomAllocation not found from event detailUrl`() {
    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withCreatedByUser(user).produce()

    every { mockManagePomCasesClient.getPomAllocation(any()) } throws HttpClientErrorException(
      HttpStatus.NOT_FOUND,
      "Not allocated",
    )

    every { mockApplicationService.findMostRecentApplication(eq(nomsNumber)) } returns application

    val slot = slot<String>()
    mockkStatic(Sentry::class)
    every { Sentry.captureMessage(capture(slot)) } returns SentryId.EMPTY_ID

    val errorMessage = "Pom deallocated for $nomsNumber, no action required"

    allocationChangedService.handleAllocationChangedEvent(allocationEvent)

    verify(exactly = 1) { mockManagePomCasesClient.getPomAllocation(any()) }
    verify(exactly = 1) { mockApplicationService.findMostRecentApplication(eq(nomsNumber)) }
    verify(exactly = 1) { Sentry.captureMessage(any()) }

    assertTrue(slot.isCaptured)
    assertTrue(slot.captured == errorMessage)
  }

  @Test
  fun `handle Allocation Changed Event and throw error when pomAllocation is null from event detailUrl`() {
    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withCreatedByUser(user).produce()

    every { mockManagePomCasesClient.getPomAllocation(any()) } returns null
    every { mockApplicationService.findMostRecentApplication(eq(nomsNumber)) } returns application

    val slot = slot<String>()
    mockkStatic(Sentry::class)
    every { Sentry.captureMessage(capture(slot)) } returns SentryId.EMPTY_ID

    val errorMessage = "Pom not allocated for $nomsNumber, no action required"

    allocationChangedService.handleAllocationChangedEvent(allocationEvent)

    verify(exactly = 1) { mockManagePomCasesClient.getPomAllocation(any()) }
    verify(exactly = 1) { mockApplicationService.findMostRecentApplication(eq(nomsNumber)) }
    verify(exactly = 1) { Sentry.captureMessage(any()) }

    assertTrue(slot.isCaptured)
    assertTrue(slot.captured == errorMessage)
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

    val slot = slot<Throwable>()
    mockkStatic(Sentry::class)
    every { Sentry.captureException(capture(slot)) } returns SentryId.EMPTY_ID

    val errorMessage = "No detail URL found"

    allocationChangedService.handleAllocationChangedEvent(eventWithNoDetailUrl)

    verify(exactly = 1) { Sentry.captureException(any()) }

    assertTrue(slot.isCaptured)
    assertTrue(slot.captured is IgnorableMessageException)
    assertTrue(slot.captured.message == errorMessage)
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

    val slot = slot<Throwable>()
    mockkStatic(Sentry::class)
    every { Sentry.captureException(capture(slot)) } returns SentryId.EMPTY_ID

    val errorMessage = "No nomsNumber found"

    allocationChangedService.handleAllocationChangedEvent(eventWithNoNomsNumber)

    verify(exactly = 1) { Sentry.captureException(any()) }

    assertTrue(slot.isCaptured)
    assertTrue(slot.captured is IgnorableMessageException)
    assertTrue(slot.captured.message == errorMessage)
  }

  @Test
  fun `handle Allocation Changed Event and do nothing if there is no application associated with the event`() {
    every { mockManagePomCasesClient.getPomAllocation(any()) } returns null
    every { mockApplicationService.findMostRecentApplication(eq(nomsNumber)) } returns null

    allocationChangedService.handleAllocationChangedEvent(allocationEvent)

    verify(exactly = 0) { mockManagePomCasesClient.getPomAllocation(any()) }
    verify(exactly = 1) { mockApplicationService.findMostRecentApplication(eq(nomsNumber)) }
  }
}
