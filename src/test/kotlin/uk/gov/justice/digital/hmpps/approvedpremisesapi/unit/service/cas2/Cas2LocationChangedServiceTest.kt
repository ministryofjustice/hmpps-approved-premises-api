package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InvalidDomainEventException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2LocationChangedService
import java.time.Instant
import java.time.ZoneId

@ExtendWith(MockKExtension::class)
class Cas2LocationChangedServiceTest {

  @MockK
  lateinit var applicationRepository: Cas2ApplicationRepository

  @MockK
  lateinit var applicationService: Cas2ApplicationService

  @MockK
  lateinit var prisonerSearchClient: PrisonerSearchClient

  @InjectMockKs
  lateinit var locationChangedService: Cas2LocationChangedService

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
    additionalInformation = AdditionalInformation(mutableMapOf("categoriesChanged" to listOf("LOCATION"))),
    personReference = PersonReference(listOf(PersonIdentifier("NOMS", nomsNumber))),
  )

  @Test
  fun `handle Location Changed Event and save new location to table`() {
    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withCreatedByUser(user).produce()

    every { prisonerSearchClient.getPrisoner(any()) } returns prisoner
    every { applicationService.findMostRecentApplication(eq(nomsNumber)) } returns application
    every { applicationRepository.save(any()) } returns application

    locationChangedService.process(locationEvent)

    verify(exactly = 1) { prisonerSearchClient.getPrisoner(any()) }
    verify(exactly = 1) { applicationService.findMostRecentApplication(eq(nomsNumber)) }
    verify(exactly = 1) { applicationRepository.save(any()) }
  }

  @Test
  fun `handle Location Changed Event and throw error when prisoner not found from event detailUrl`() {
    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withCreatedByUser(user).produce()

    every { prisonerSearchClient.getPrisoner(any()) } returns null
    every { applicationService.findMostRecentApplication(eq(nomsNumber)) } returns application

    assertThrows<NullPointerException> { locationChangedService.process(locationEvent) }
  }

  @Test
  fun `handle Location Changed Event and throw error when there is no detailUrl in the event`() {
    val eventWithNoDetailUrl = locationEvent.copy(detailUrl = null)

    assertThrows<InvalidDomainEventException> { locationChangedService.process(eventWithNoDetailUrl) }
  }

  @Test
  fun `handle Location Changed Event and throw error when there is no nomsNumber in the event`() {
    val eventWithNoNomsNumber = locationEvent.copy(personReference = PersonReference())
    assertThrows<InvalidDomainEventException> { locationChangedService.process(eventWithNoNomsNumber) }
  }

  @Test
  fun `handle Location Changed Event and do nothing if there is no application associated with the event`() {
    every { prisonerSearchClient.getPrisoner(any()) } returns null
    every { applicationService.findMostRecentApplication(eq(nomsNumber)) } returns null

    locationChangedService.process(locationEvent)

    verify(exactly = 0) { prisonerSearchClient.getPrisoner(any()) }
    verify(exactly = 1) { applicationService.findMostRecentApplication(eq(nomsNumber)) }
  }
}
