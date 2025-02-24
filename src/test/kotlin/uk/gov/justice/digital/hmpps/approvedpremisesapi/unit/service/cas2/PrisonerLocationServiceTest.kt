package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2PrisonerLocationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2PrisonerLocationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.IgnorableMessageException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.PrisonerLocationService
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class PrisonerLocationServiceTest {

  private val mockPrisonerLocationRepository = mockk<Cas2PrisonerLocationRepository>()
  private val mockApplicationRepository = mockk<Cas2ApplicationRepository>()
  private val mockPrisonerSearchClient = mockk<PrisonerSearchClient>()

  private val prisonerLocationService: PrisonerLocationService = PrisonerLocationService(
    mockPrisonerSearchClient,
    mockApplicationRepository,
    mockPrisonerLocationRepository,
  )

  private val prisoner = Prisoner(prisonId = "A1234AB", prisonName = "Bob", lastPrisonId = "1234")
  private val eventType = "prisoner-offender-search.prisoner.updated"
  private val nomsNumber = "NOMSABC"
  private val detailUrl = "some/url"

  private val user = NomisUserEntityFactory()
    .produce()
  private val occurredAt = Instant.now().atZone(ZoneId.systemDefault())
  private val application = Cas2ApplicationEntityFactory()
    .withNomsNumber(nomsNumber)
    .withCreatedByUser(user)
    .produce()

  private val oldPrisonerLocation = Cas2PrisonerLocationEntity(
    id = UUID.randomUUID(),
    application = application,
    prisonCode = prisoner.lastPrisonId,
    staffId = application.createdByUser.id,
    occurredAt = occurredAt.toOffsetDateTime(),
    endDate = null,
  )

  val event = HmppsDomainEvent(
    eventType = eventType,
    version = 1,
    detailUrl = detailUrl,
    occurredAt = occurredAt,
    description = "anything",
    personReference = HmppsDomainEventPersonReference(listOf(PersonIdentifier("NOMS", nomsNumber))),
  )

  @Test
  fun `handle Location Changed Event and save new prisonerLocation to table and update previous location endDate`() {
    val oldPrisonerLocationWithEndDate = oldPrisonerLocation.copy(endDate = occurredAt.toOffsetDateTime())

    val newPrisonerLocation = Cas2PrisonerLocationEntity(
      id = UUID.randomUUID(),
      application = application,
      prisonCode = prisoner.prisonId,
      staffId = null,
      occurredAt = occurredAt.toOffsetDateTime(),
      endDate = null,
    )

    val applications = listOf(application)

    every { mockPrisonerSearchClient.getPrisoner(any()) } returns prisoner
    every { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) } returns applications
    every { mockPrisonerLocationRepository.findPrisonerLocation(eq(application.id)) } returns oldPrisonerLocation
    every { mockPrisonerLocationRepository.save(any()) } returnsMany listOf(
      oldPrisonerLocationWithEndDate,
      newPrisonerLocation,
    )

    prisonerLocationService.handleLocationChangedEvent(event)

    verify(exactly = 1) { mockPrisonerSearchClient.getPrisoner(any()) }
    verify(exactly = 1) { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) }
    verify(exactly = 1) { mockPrisonerLocationRepository.findPrisonerLocation(eq(application.id)) }
    verify(exactly = 2) { mockPrisonerLocationRepository.save(any()) }
  }

  @Test
  fun `handle Location Changed Event and throw error when prisoner location table is not populated with that prisoner's location`() {
    val applications = listOf(application)

    every { mockPrisonerSearchClient.getPrisoner(any()) } returns prisoner
    every { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) } returns applications
    every { mockPrisonerLocationRepository.findPrisonerLocation(eq(application.id)) } returns null

    val exception =
      assertThrows<IgnorableMessageException> { prisonerLocationService.handleLocationChangedEvent(event) }
    assertThat(exception.message).isEqualTo("No null prisoner location found for applicationId ${application.id}")

    verify(exactly = 1) { mockPrisonerSearchClient.getPrisoner(any()) }
    verify(exactly = 1) { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) }
    verify(exactly = 1) { mockPrisonerLocationRepository.findPrisonerLocation(eq(application.id)) }
  }

  @Test
  fun `handle Location Changed Event and throw error when prisoner not found from event detailUrl`() {
    val applications = listOf(application)

    every { mockPrisonerSearchClient.getPrisoner(any()) } returns null
    every { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) } returns applications

    val exception =
      assertThrows<IgnorableMessageException> { prisonerLocationService.handleLocationChangedEvent(event) }
    assertThat(exception.message).isEqualTo("No prisoner found for detailUrl $detailUrl")

    verify(exactly = 1) { mockPrisonerSearchClient.getPrisoner(any()) }
    verify(exactly = 1) { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) }
  }

  @Test
  fun `handle Location Changed Event and throw error when there is no detailUrl in the event`() {
    val eventWithNoDetailUrl = HmppsDomainEvent(
      eventType = eventType,
      version = 1,
      detailUrl = null,
      occurredAt = occurredAt,
      description = "anything",
      personReference = HmppsDomainEventPersonReference(listOf(PersonIdentifier("NOMS", nomsNumber))),
    )

    val exception =
      assertThrows<IgnorableMessageException> { prisonerLocationService.handleLocationChangedEvent(eventWithNoDetailUrl) }
    assertThat(exception.message).isEqualTo("No detail URL found")
  }

  @Test
  fun `handle Location Changed Event and throw error when there is no nomsNumber in the event`() {
    val eventWithNoNomsNumber = HmppsDomainEvent(
      eventType = eventType,
      version = 1,
      detailUrl = detailUrl,
      occurredAt = occurredAt,
      description = "anything",
      personReference = HmppsDomainEventPersonReference(listOf()),
    )

    val exception =
      assertThrows<IgnorableMessageException> { prisonerLocationService.handleLocationChangedEvent(eventWithNoNomsNumber) }
    assertThat(exception.message).isEqualTo("No nomsNumber found")
  }

  @Test
  fun `handle Location Changed Event and do nothing if there are no applications associated with the event`() {
    val applications = listOf<Cas2ApplicationEntity>()

    every { mockPrisonerSearchClient.getPrisoner(any()) } returns null
    every { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) } returns applications

    prisonerLocationService.handleLocationChangedEvent(event)

    verify(exactly = 0) { mockPrisonerSearchClient.getPrisoner(any()) }
    verify(exactly = 1) { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) }
  }

  @Test
  fun `handle Location Changed Event and save new prisonerLocation to table and update previous location endDate with multiple applications`() {
    val application2 = Cas2ApplicationEntityFactory()
      .withNomsNumber(nomsNumber)
      .withCreatedByUser(user)
      .produce()
    val application3 = Cas2ApplicationEntityFactory()
      .withNomsNumber(nomsNumber)
      .withCreatedByUser(user)
      .produce()

    val oldPrisonerLocation2 = Cas2PrisonerLocationEntity(
      id = UUID.randomUUID(),
      application = application2,
      prisonCode = prisoner.lastPrisonId,
      staffId = application2.createdByUser.id,
      occurredAt = occurredAt.toOffsetDateTime(),
      endDate = null,
    )
    val oldPrisonerLocation3 = Cas2PrisonerLocationEntity(
      id = UUID.randomUUID(),
      application = application3,
      prisonCode = prisoner.lastPrisonId,
      staffId = application3.createdByUser.id,
      occurredAt = occurredAt.toOffsetDateTime(),
      endDate = null,
    )

    val applications = listOf(application, application2, application3)

    every { mockPrisonerSearchClient.getPrisoner(any()) } returns prisoner
    every { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) } returns applications
    every { mockPrisonerLocationRepository.findPrisonerLocation(eq(application.id)) } returns oldPrisonerLocation
    every { mockPrisonerLocationRepository.findPrisonerLocation(eq(application2.id)) } returns oldPrisonerLocation2
    every { mockPrisonerLocationRepository.findPrisonerLocation(eq(application3.id)) } returns oldPrisonerLocation3
    every { mockPrisonerLocationRepository.save(any()) } returns null

    prisonerLocationService.handleLocationChangedEvent(event)

    verify(exactly = 1) { mockPrisonerSearchClient.getPrisoner(any()) }
    verify(exactly = 1) { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) }
    verify(exactly = 3) { mockPrisonerLocationRepository.findPrisonerLocation(any()) }
    verify(exactly = 6) { mockPrisonerLocationRepository.save(any()) }
  }
}
