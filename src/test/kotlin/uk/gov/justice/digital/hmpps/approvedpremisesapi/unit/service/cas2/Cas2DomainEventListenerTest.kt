package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.AllocationChangedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2DomainEventListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.LocationChangedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory

class Cas2DomainEventListenerTest {

  private val mockAllocationChangedService = mockk<AllocationChangedService>()
  private val mockLocationChangedService = mockk<LocationChangedService>()
  private val objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper()

  private val cas2DomainEventListener: Cas2DomainEventListener = Cas2DomainEventListener(
    objectMapper,
    mockAllocationChangedService,
    mockLocationChangedService,
  )

  @Test
  fun `Handle Allocation Changed Message on Domain Events Topic`() {
    every { mockAllocationChangedService.handleEvent(any()) } returns Unit

    val msg = """{"Message": "{\"eventType\":\"offender-management.allocation.changed\",\"version\":\"1\"}"}"""

    cas2DomainEventListener.processMessage(msg)

    verify(exactly = 1) { mockAllocationChangedService.handleEvent(any()) }
  }

  @Test
  fun `Handle Unwanted Message on Domain Events Topic`() {
    every { mockAllocationChangedService.handleEvent(any()) } returns Unit

    val msg = """{"Message": "{\"eventType\":\"unwanted\",\"version\":\"1\"}"}"""

    cas2DomainEventListener.processMessage(msg)

    verify(exactly = 0) { mockAllocationChangedService.handleEvent(any()) }
    verify(exactly = 0) { mockLocationChangedService.handleEvent(any()) }
  }

  @Test
  fun `Handle Location Changed Message on Domain Events Topic`() {
    every { mockLocationChangedService.handleEvent(any()) } returns Unit

    val msg =
      """{"Message": "{\"eventType\":\"prisoner-offender-search.prisoner.updated\",\"additionalInformation\":{\"categoriesChanged\": [\"LOCATION\"]},\"version\":\"1\"}"}"""

    cas2DomainEventListener.processMessage(msg)

    verify(exactly = 1) { mockLocationChangedService.handleEvent(any()) }
  }

  @Test
  fun `Reject Location Changed Message on Domain Events Topic without location`() {
    every { mockLocationChangedService.handleEvent(any()) } returns Unit

    val msg =
      """{"Message": "{\"eventType\":\"prisoner-offender-search.prisoner.updated\",\"additionalInformation\":{\"categoriesChanged\": [\"NOT_LOCATION\"]},\"version\":\"1\"}"}"""

    cas2DomainEventListener.processMessage(msg)

    verify(exactly = 0) { mockLocationChangedService.handleEvent(any()) }
  }
}
