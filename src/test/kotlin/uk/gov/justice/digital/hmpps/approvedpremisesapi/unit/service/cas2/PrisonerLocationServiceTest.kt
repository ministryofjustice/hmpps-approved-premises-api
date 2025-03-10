package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManagePomCasesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prison
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2PrisonerLocationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2PrisonerLocationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
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
  private val mockManagePomCasesClient = mockk<ManagePomCasesClient>()
  private val mockNomisUserRepository = mockk<NomisUserRepository>()

  private val prisonerLocationService: PrisonerLocationService = PrisonerLocationService(
    mockPrisonerSearchClient,
    mockApplicationRepository,
    mockPrisonerLocationRepository,
    mockNomisUserRepository,
    mockManagePomCasesClient,
  )

  private val prisoner = Prisoner(prisonId = "A1234AB")
  private val eventType = "prisoner-offender-search.prisoner.updated"
  private val nomsNumber = "NOMSABC"
  private val detailUrl = "some/url"
  private val staffCode = "12345"

  private val user = NomisUserEntityFactory()
    .withNomisStaffCode(staffCode.toLong())
    .produce()
  private val occurredAt = Instant.now().atZone(ZoneId.systemDefault())
  private val application = Cas2ApplicationEntityFactory()
    .withNomsNumber(nomsNumber)
    .withCreatedByUser(user)
    .produce()

  private val oldPrisonerLocation = Cas2PrisonerLocationEntity(
    id = UUID.randomUUID(),
    application = application,
    prisonCode = "LON",
    staffId = application.createdByUser.id,
    occurredAt = occurredAt.toOffsetDateTime(),
    endDate = null,
  )

  private val anotherOldPrisonerLocation = Cas2PrisonerLocationEntity(
    id = UUID.randomUUID(),
    application = application,
    prisonCode = "LON",
    staffId = null,
    occurredAt = occurredAt.toOffsetDateTime(),
    endDate = null,
  )

  private val locationEvent = HmppsDomainEvent(
    eventType = eventType,
    version = 1,
    detailUrl = detailUrl,
    occurredAt = occurredAt,
    description = "anything",
    personReference = HmppsDomainEventPersonReference(listOf(PersonIdentifier("NOMS", nomsNumber))),
  )

  private val allocationEvent = HmppsDomainEvent(
    eventType = eventType,
    version = 1,
    detailUrl = detailUrl,
    occurredAt = occurredAt,
    description = "anything",
    additionalInformation = mapOf("staffCode" to staffCode),
    personReference = HmppsDomainEventPersonReference(listOf(PersonIdentifier("NOMS", nomsNumber))),
  )

  val pomAllocation = PomAllocation(Prison(code = "ABC"))

  @Test
  fun `handle Allocation Changed Event and save new prisonerLocation to table and update previous location endDate`() {
    val oldPrisonerLocationWithEndDate = anotherOldPrisonerLocation.copy(endDate = occurredAt.toOffsetDateTime())

    val newPrisonerLocation = Cas2PrisonerLocationEntity(
      id = UUID.randomUUID(),
      application = application,
      prisonCode = pomAllocation.prison.code,
      staffId = user.id,
      occurredAt = occurredAt.toOffsetDateTime(),
      endDate = null,
    )

    val applications = listOf(application)

    every { mockManagePomCasesClient.getPomAllocation(any()) } returns pomAllocation
    every { mockNomisUserRepository.findByNomisStaffId(any()) } returns user
    every { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) } returns applications
    every { mockPrisonerLocationRepository.findPrisonerLocation(eq(application.id)) } returns oldPrisonerLocation
    every { mockPrisonerLocationRepository.save(any()) } returnsMany listOf(
      oldPrisonerLocationWithEndDate,
      newPrisonerLocation,
    )

    prisonerLocationService.handleAllocationChangedEvent(allocationEvent)

    verify(exactly = 1) { mockManagePomCasesClient.getPomAllocation(any()) }
    verify(exactly = 1) { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) }
    verify(exactly = 1) { mockNomisUserRepository.findByNomisStaffId(eq(staffCode.toLong())) }
    verify(exactly = 1) { mockPrisonerLocationRepository.findPrisonerLocation(eq(application.id)) }
    verify(exactly = 2) { mockPrisonerLocationRepository.save(any()) }
  }

  @Test
  fun `handle Allocation Changed Event and throw error when prisoner location table is not populated with that prisoner's location`() {
    val applications = listOf(application)

    every { mockManagePomCasesClient.getPomAllocation(any()) } returns pomAllocation
    every { mockNomisUserRepository.findByNomisStaffId(any()) } returns user
    every { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) } returns applications
    every { mockPrisonerLocationRepository.findPrisonerLocation(eq(application.id)) } returns null

    val exception =
      assertThrows<IgnorableMessageException> { prisonerLocationService.handleAllocationChangedEvent(allocationEvent) }
    assertThat(exception.message).isEqualTo("No null prisoner location found for applicationId ${application.id}")

    verify(exactly = 1) { mockManagePomCasesClient.getPomAllocation(any()) }
    verify(exactly = 1) { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) }
    verify(exactly = 1) { mockNomisUserRepository.findByNomisStaffId(eq(staffCode.toLong())) }
    verify(exactly = 1) { mockPrisonerLocationRepository.findPrisonerLocation(eq(application.id)) }
  }

  @Test
  fun `handle Allocation Changed Event and throw error when pom allocation not found from event detailUrl`() {
    val applications = listOf(application)

    every { mockManagePomCasesClient.getPomAllocation(any()) } returns null
    every { mockNomisUserRepository.findByNomisStaffId(any()) } returns user
    every { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) } returns applications

    val exception =
      assertThrows<IgnorableMessageException> { prisonerLocationService.handleAllocationChangedEvent(allocationEvent) }
    assertThat(exception.message).isEqualTo("No POM allocation found for detailUrl $detailUrl")

    verify(exactly = 1) { mockManagePomCasesClient.getPomAllocation(any()) }
    verify(exactly = 1) { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) }
    verify(exactly = 1) { mockNomisUserRepository.findByNomisStaffId(eq(staffCode.toLong())) }
  }

  @Test
  fun `handle Allocation Changed Event and throw error when user not found from event staffCode`() {
    val applications = listOf(application)

    every { mockNomisUserRepository.findByNomisStaffId(any()) } returns null
    every { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) } returns applications

    val exception =
      assertThrows<IgnorableMessageException> { prisonerLocationService.handleAllocationChangedEvent(allocationEvent) }
    assertThat(exception.message).isEqualTo("No user found for staffCode $staffCode")

    verify(exactly = 1) { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) }
    verify(exactly = 1) { mockNomisUserRepository.findByNomisStaffId(eq(staffCode.toLong())) }
  }

  @Test
  fun `handle Allocation Changed Event and throw error when there is no detailUrl in the event`() {
    val eventWithNoDetailUrl = HmppsDomainEvent(
      eventType = eventType,
      version = 1,
      detailUrl = null,
      occurredAt = occurredAt,
      description = "anything",
      personReference = HmppsDomainEventPersonReference(listOf(PersonIdentifier("NOMS", nomsNumber))),
    )

    val exception =
      assertThrows<IgnorableMessageException> {
        prisonerLocationService.handleAllocationChangedEvent(
          eventWithNoDetailUrl,
        )
      }
    assertThat(exception.message).isEqualTo("No detail URL found")
  }

  @Test
  fun `handle Allocation Changed Event and throw error when there is no nomsNumber in the event`() {
    val eventWithNoNomsNumber = HmppsDomainEvent(
      eventType = eventType,
      version = 1,
      detailUrl = detailUrl,
      occurredAt = occurredAt,
      description = "anything",
      personReference = HmppsDomainEventPersonReference(listOf()),
    )

    val exception =
      assertThrows<IgnorableMessageException> {
        prisonerLocationService.handleAllocationChangedEvent(
          eventWithNoNomsNumber,
        )
      }
    assertThat(exception.message).isEqualTo("No nomsNumber found")
  }

  @Test
  fun `handle Allocation Changed Event and do nothing if there are no applications associated with the event`() {
    val applications = listOf<Cas2ApplicationEntity>()

    every { mockPrisonerSearchClient.getPrisoner(any()) } returns null
    every { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) } returns applications

    prisonerLocationService.handleAllocationChangedEvent(allocationEvent)

    verify(exactly = 0) { mockPrisonerSearchClient.getPrisoner(any()) }
    verify(exactly = 1) { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) }
  }

  @Test
  fun `handle Allocation Changed Event and save new prisonerLocation to table and update previous location endDate with multiple applications`() {
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
      prisonCode = "LON",
      staffId = application2.createdByUser.id,
      occurredAt = occurredAt.toOffsetDateTime(),
      endDate = null,
    )
    val oldPrisonerLocation3 = Cas2PrisonerLocationEntity(
      id = UUID.randomUUID(),
      application = application3,
      prisonCode = "LON",
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

    prisonerLocationService.handleLocationChangedEvent(allocationEvent)

    verify(exactly = 1) { mockPrisonerSearchClient.getPrisoner(any()) }
    verify(exactly = 1) { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) }
    verify(exactly = 3) { mockPrisonerLocationRepository.findPrisonerLocation(any()) }
    verify(exactly = 6) { mockPrisonerLocationRepository.save(any()) }
  }

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

    prisonerLocationService.handleLocationChangedEvent(locationEvent)

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
      assertThrows<IgnorableMessageException> { prisonerLocationService.handleLocationChangedEvent(locationEvent) }
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
      assertThrows<IgnorableMessageException> { prisonerLocationService.handleLocationChangedEvent(locationEvent) }
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

    prisonerLocationService.handleLocationChangedEvent(locationEvent)

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
      prisonCode = "LON",
      staffId = application2.createdByUser.id,
      occurredAt = occurredAt.toOffsetDateTime(),
      endDate = null,
    )
    val oldPrisonerLocation3 = Cas2PrisonerLocationEntity(
      id = UUID.randomUUID(),
      application = application3,
      prisonCode = "LON",
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

    prisonerLocationService.handleLocationChangedEvent(locationEvent)

    verify(exactly = 1) { mockPrisonerSearchClient.getPrisoner(any()) }
    verify(exactly = 1) { mockApplicationRepository.findAllSubmittedApplicationByNomsNumber(eq(nomsNumber)) }
    verify(exactly = 3) { mockPrisonerLocationRepository.findPrisonerLocation(any()) }
    verify(exactly = 6) { mockPrisonerLocationRepository.save(any()) }
  }
}
