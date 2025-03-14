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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.IgnorableMessageException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2LocationChangedService
import java.time.Instant
import java.time.ZoneId

class Cas2LocationChangedServiceTest {

  private val mockApplicationRepository = mockk<Cas2ApplicationRepository>()
  private val mockApplicationService = mockk<Cas2ApplicationService>()
  private val mockPrisonerSearchClient = mockk<PrisonerSearchClient>()

  private val locationChangedService: Cas2LocationChangedService = Cas2LocationChangedService(
    mockPrisonerSearchClient,
    mockApplicationService,
    mockApplicationRepository,
  )

  private val prisoner = Prisoner(prisonId = "A1234AB")
  private val eventType = "prisoner-offender-search.prisoner.updated"
  private val nomsNumber = "NOMSABC"
  private val detailUrl = "some/url"

  private val user = NomisUserEntityFactory().produce()

  private val occurredAt = Instant.now().atZone(ZoneId.systemDefault())

  private val locationEvent = HmppsDomainEvent(
    eventType = eventType,
    version = 1,
    detailUrl = detailUrl,
    occurredAt = occurredAt,
    description = "anything",
    additionalInformation = AdditionalInformation(),
    personReference = PersonReference(listOf(PersonIdentifier("NOMS", nomsNumber))),
  )

  @Test
  fun `handle Location Changed Event and save new location to table`() {
    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withCreatedByUser(user).produce()

    every { mockPrisonerSearchClient.getPrisoner(any()) } returns prisoner
    every { mockApplicationService.findMostRecentApplication(eq(nomsNumber)) } returns application
    every { mockApplicationRepository.save(any()) } returns null

    locationChangedService.handleLocationChangedEvent(locationEvent)

    verify(exactly = 1) { mockPrisonerSearchClient.getPrisoner(any()) }
    verify(exactly = 1) { mockApplicationService.findMostRecentApplication(eq(nomsNumber)) }
    verify(exactly = 1) { mockApplicationRepository.save(any()) }
  }

  @Test
  fun `handle Location Changed Event and throw error when prisoner not found from event detailUrl`() {
    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withCreatedByUser(user).produce()

    every { mockPrisonerSearchClient.getPrisoner(any()) } returns null
    every { mockApplicationService.findMostRecentApplication(eq(nomsNumber)) } returns application

    val slot = slot<Throwable>()
    mockkStatic(Sentry::class)
    every { Sentry.captureException(capture(slot)) } returns SentryId.EMPTY_ID

    val errorMessage = "No prisoner found for detailUrl $detailUrl"

    locationChangedService.handleLocationChangedEvent(locationEvent)

    verify(exactly = 1) { mockPrisonerSearchClient.getPrisoner(any()) }
    verify(exactly = 1) { mockApplicationService.findMostRecentApplication(eq(nomsNumber)) }
    verify(exactly = 1) { Sentry.captureException(any()) }

    assertTrue(slot.isCaptured)
    assertTrue(slot.captured is IgnorableMessageException)
    assertTrue(slot.captured.message == errorMessage)
  }

  @Test
  fun `handle Location Changed Event and throw error when there is no detailUrl in the event`() {
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

    locationChangedService.handleLocationChangedEvent(eventWithNoDetailUrl)

    verify(exactly = 1) { Sentry.captureException(any()) }

    assertTrue(slot.isCaptured)
    assertTrue(slot.captured is IgnorableMessageException)
    assertTrue(slot.captured.message == errorMessage)
  }

  @Test
  fun `handle Location Changed Event and throw error when there is no nomsNumber in the event`() {
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

    locationChangedService.handleLocationChangedEvent(eventWithNoNomsNumber)

    verify(exactly = 1) { Sentry.captureException(any()) }

    assertTrue(slot.isCaptured)
    assertTrue(slot.captured is IgnorableMessageException)
    assertTrue(slot.captured.message == errorMessage)
  }

  @Test
  fun `handle Location Changed Event and do nothing if there is no application associated with the event`() {
    every { mockPrisonerSearchClient.getPrisoner(any()) } returns null
    every { mockApplicationService.findMostRecentApplication(eq(nomsNumber)) } returns null

    locationChangedService.handleLocationChangedEvent(locationEvent)

    verify(exactly = 0) { mockPrisonerSearchClient.getPrisoner(any()) }
    verify(exactly = 1) { mockApplicationService.findMostRecentApplication(eq(nomsNumber)) }
  }
}
