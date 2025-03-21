package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import com.fasterxml.jackson.core.JsonParseException
import com.ninjasquad.springmockk.SpykBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2AllocationChangedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2DomainEventListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2LocationChangedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory

@ExtendWith(MockKExtension::class)
class Cas2DomainEventListenerTest {

  @MockK
  lateinit var cas2AllocationChangedService: Cas2AllocationChangedService

  @MockK
  lateinit var cas2LocationChangedService: Cas2LocationChangedService

  @MockK
  lateinit var sentryService: SentryService

  @SpykBean
  var objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper()

  @InjectMockKs
  lateinit var cas2DomainEventListener: Cas2DomainEventListener

  @Test
  fun `POM Allocation changed message is processed`() {
    every { cas2AllocationChangedService.process(any()) } returns Unit
    val msg = """{"Message": "{\"eventType\":\"offender-management.allocation.changed\",\"additionalInformation\":{\"staffCode\":123456,\"prisonId\":\"PPP\"}}"}"""
    cas2DomainEventListener.processMessage(msg)
    verify(exactly = 1) { cas2AllocationChangedService.process(any()) }
  }

  @Test
  fun `Prisoner updated message is processed`() {
    every { cas2LocationChangedService.process(any()) } returns Unit
    val msg =
      """{"Message": "{\"eventType\":\"prisoner-offender-search.prisoner.updated\",\"additionalInformation\":{\"categoriesChanged\": [\"LOCATION\"]},\"version\":\"1\"}"}"""
    cas2DomainEventListener.processMessage(msg)
    verify(exactly = 1) { cas2LocationChangedService.process(any()) }
  }

  @Test
  fun `exceptions are captured by sentry and still thrown`() {
    every { sentryService.captureException(any()) } just Runs
    assertThrows<JsonParseException> { cas2DomainEventListener.processMessage("invalid") }
    verify { sentryService.captureException(any()) }
  }
}
