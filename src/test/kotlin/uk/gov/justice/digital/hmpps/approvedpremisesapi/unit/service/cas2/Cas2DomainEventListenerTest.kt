package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2DomainEventListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2PrisonerLocationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory

class Cas2DomainEventListenerTest {

  private val mockPrisonerLocationService = mockk<Cas2PrisonerLocationService>()
  private val objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper()

  private val cas2DomainEventListener: Cas2DomainEventListener = Cas2DomainEventListener(
    objectMapper,
    mockPrisonerLocationService,
  )

  @Test
  fun `Handle Allocation Changed Message on Domain Events Topic`() {
    every { mockPrisonerLocationService.handleAllocationChangedEvent(any()) } returns Unit

    val msg = """{"Message": "{\"eventType\":\"offender-management.allocation.changed\",\"version\":\"1\"}"}"""

    cas2DomainEventListener.processMessage(msg)

    verify(exactly = 1) { mockPrisonerLocationService.handleAllocationChangedEvent(any()) }
  }

  @Test
  fun `Handle Unwanted Message on Domain Events Topic`() {
    every { mockPrisonerLocationService.handleLocationChangedEvent(any()) } returns Unit
    every { mockPrisonerLocationService.handleAllocationChangedEvent(any()) } returns Unit

    val msg = """{"Message": "{\"eventType\":\"unwanted\",\"version\":\"1\"}"}"""

    cas2DomainEventListener.processMessage(msg)

    verify(exactly = 0) { mockPrisonerLocationService.handleLocationChangedEvent(any()) }
    verify(exactly = 0) { mockPrisonerLocationService.handleAllocationChangedEvent(any()) }
  }

  @Test
  fun `Handle Location Changed Message on Domain Events Topic`() {
    every { mockPrisonerLocationService.handleLocationChangedEvent(any()) } returns Unit

    val msg =
      """{"Message": "{\"eventType\":\"prisoner-offender-search.prisoner.updated\",\"additionalInformation\":{\"categoriesChanged\": [\"LOCATION\"]},\"version\":\"1\"}"}"""

    cas2DomainEventListener.processMessage(msg)

    verify(exactly = 1) { mockPrisonerLocationService.handleLocationChangedEvent(any()) }
  }

  @Test
  fun `Reject Location Changed Message on Domain Events Topic without location`() {
    every { mockPrisonerLocationService.handleLocationChangedEvent(any()) } returns Unit

    val msg =
      """{"Message": "{\"eventType\":\"prisoner-offender-search.prisoner.updated\",\"additionalInformation\":{\"categoriesChanged\": [\"NOT_LOCATION\"]},\"version\":\"1\"}"}"""

    cas2DomainEventListener.processMessage(msg)

    verify(exactly = 0) { mockPrisonerLocationService.handleLocationChangedEvent(any()) }
  }
}
